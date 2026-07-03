/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.zaproxy.zap.extension.neonmarker;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.db.DatabaseException;
import org.parosproxy.paros.extension.Extension;
import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.extension.ExtensionHook;
import org.parosproxy.paros.extension.OptionsChangedListener;
import org.parosproxy.paros.extension.history.ExtensionHistory;
import org.parosproxy.paros.model.HistoryReference;
import org.parosproxy.paros.model.HistoryReferenceEventPublisher;
import org.parosproxy.paros.model.OptionsParam;
import org.zaproxy.addon.pscan.ExtensionPassiveScan2;
import org.zaproxy.zap.ZAP;
import org.zaproxy.zap.eventBus.Event;
import org.zaproxy.zap.eventBus.EventConsumer;
import org.zaproxy.zap.extension.help.ExtensionHelp;
import org.zaproxy.zap.utils.DisplayUtils;
import org.zaproxy.zap.view.table.HistoryReferencesTableModel;

public class ExtensionNeonmarker extends ExtensionAdaptor implements EventConsumer {
    private static final Logger LOGGER = LogManager.getLogger(ExtensionNeonmarker.class);
    private static final List<Class<? extends Extension>> EXTENSION_DEPENDENCIES =
            List.of(ExtensionPassiveScan2.class, ExtensionHistory.class);

    public static final String NAME = "ExtensionNeonmarker";
    public static final String RESOURCE = "/org/zaproxy/zap/extension/neonmarker/resources";

    public static final String TAG_PREFIX = "neon_";
    public static final Pattern TAG_PATTERN =
            Pattern.compile(
                    ExtensionNeonmarker.TAG_PREFIX
                            + "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12}");

    private static ImageIcon icon;
    private NeonmarkerColorService colorService;
    private NeonmarkerPanel neonmarkerPanel;
    private MarkItemColorHighlighter highlighter;
    private boolean highlighterEnabled;
    private PopupMenuHistoryNeonmarker menuHistoryColor;

    public ExtensionNeonmarker() {
        super(NAME);
    }

    @Override
    public void hook(ExtensionHook extensionHook) {
        colorService =
                new NeonmarkerColorService(
                        ExtensionNeonmarker::isValidTag, this::refreshColouringView);

        ZAP.getEventBus()
                .registerConsumer(
                        this,
                        HistoryReferenceEventPublisher.getPublisher().getPublisherName(),
                        HistoryReferenceEventPublisher.EVENT_REMOVED,
                        HistoryReferenceEventPublisher.EVENT_TAG_REMOVED,
                        HistoryReferenceEventPublisher.EVENT_TAGS_SET);

        if (hasView()) {
            toggleHighlighter(true);
            extensionHook.getHookView().addWorkPanel(getNeonmarkerPanel());
            ExtensionHelp.enableHelpKey(getNeonmarkerPanel(), "neonmarker");

            extensionHook.getHookMenu().addPopupMenuItem(getMenuHistoryColor());
            extensionHook.addOptionsChangedListener(new OptionsChangedListenerImpl());
        }
    }

    @Override
    public void eventReceived(Event event) {
        if (EventQueue.isDispatchThread()) {
            pruneOrphanNeonRules();
        } else {
            EventQueue.invokeLater(this::pruneOrphanNeonRules);
        }
    }

    private void pruneOrphanNeonRules() {
        if (colorService == null) {
            return;
        }
        Set<String> existingTags;
        try {
            existingTags =
                    new HashSet<>(
                            getExtension(ExtensionHistory.class)
                                    .getModel()
                                    .getDb()
                                    .getTableTag()
                                    .getAllTags());
        } catch (DatabaseException e) {
            LOGGER.debug("Couldn't get tags from DB while pruning neon rules.", e);
            return;
        }
        colorService.removeOrphanNeonRules(tag -> TAG_PATTERN.matcher(tag).matches(), existingTags);
    }

    private void refreshColouringView() {
        if (!hasView() || colorService == null || neonmarkerPanel == null) {
            return;
        }
        neonmarkerPanel.refreshDisplay();
    }

    @Override
    public List<Class<? extends Extension>> getDependencies() {
        return EXTENSION_DEPENDENCIES;
    }

    @Override
    public boolean canUnload() {
        return true;
    }

    @Override
    public void unload() {
        ZAP.getEventBus().unregisterConsumer(this);
        toggleHighlighter(false);
        neonmarkerPanel = null;
        colorService = null;
        super.unload();
    }

    @Override
    public String getUIName() {
        return Constant.messages.getString("neonmarker.name");
    }

    @Override
    public String getAuthor() {
        return "Juha Kivekäs, Kingthorin";
    }

    NeonmarkerPanel getNeonmarkerPanel() {
        if (neonmarkerPanel == null) {
            neonmarkerPanel =
                    new NeonmarkerPanel(
                            getExtension(ExtensionHistory.class).getModel(), colorService);
        }
        return neonmarkerPanel;
    }

    static ImageIcon getIcon() {
        if (icon == null) {
            icon =
                    DisplayUtils.getScaledIcon(
                            ExtensionNeonmarker.class.getResource(RESOURCE + "/spectrum.png"));
        }
        return icon;
    }

    static <T extends Extension> T getExtension(Class<T> clazz) {
        return Control.getSingleton().getExtensionLoader().getExtension(clazz);
    }

    private MarkItemColorHighlighter getHighlighter() {
        if (highlighter == null) {
            ExtensionHistory extHistory = getExtension(ExtensionHistory.class);
            int idColumnIndex =
                    extHistory
                            .getHistoryReferencesTable()
                            .getModel()
                            .getColumnIndex(HistoryReferencesTableModel.Column.HREF_ID);
            highlighter = new MarkItemColorHighlighter(extHistory, idColumnIndex);
        }
        return highlighter;
    }

    protected void toggleHighlighter(boolean on) {
        if (on == highlighterEnabled) {
            return;
        }
        ExtensionHistory extHistory = getExtension(ExtensionHistory.class);
        if (on) {
            extHistory.getHistoryReferencesTable().addHighlighter(getHighlighter());
        } else {
            extHistory.getHistoryReferencesTable().removeHighlighter(getHighlighter());
        }
        highlighterEnabled = on;
    }

    /**
     * Allows the addition of a color mapping between a {@code Color} and passive scan Tag.
     *
     * @param tag the name of the tag to be mapped.
     * @param color opaque RGB as {@code 0xRRGGBB} (e.g. {@code 0x990000}), or a value from {@link
     *     Color#getRGB()} (alpha component is ignored; colour is treated as opaque)
     * @return whether or not the addition of the color mapping was successful ({@code true} if it
     *     was, {@code false} otherwise).
     */
    public boolean addColorMapping(String tag, int color) {
        return colorService != null && colorService.addColorMapping(tag, color);
    }

    private static boolean isValidTag(String tag) {
        if (TAG_PATTERN.matcher(tag).matches()) {
            return true;
        }
        try {
            return getExtension(ExtensionPassiveScan2.class).getAutoTaggingTags().contains(tag)
                    || getExtension(ExtensionHistory.class)
                            .getModel()
                            .getDb()
                            .getTableTag()
                            .getAllTags()
                            .contains(tag);
        } catch (DatabaseException e) {
            LOGGER.debug("Couldn't get tags from DB.");
            return false;
        }
    }

    private class OptionsChangedListenerImpl implements OptionsChangedListener {
        @Override
        public void optionsChanged(OptionsParam optionsParam) {
            refreshColouringView();
        }
    }

    private class MarkItemColorHighlighter extends AbstractHighlighter {
        private int idColumnIndex;
        private ExtensionHistory extHistory;

        MarkItemColorHighlighter(ExtensionHistory extHistory, int idColumnIndex) {
            super();
            setHighlightPredicate((renderer, adapter) -> !adapter.isSelected());
            this.extHistory = extHistory;
            this.idColumnIndex = idColumnIndex;
        }

        @Override
        protected Component doHighlight(Component component, ComponentAdapter adapter) {
            int historyId = (int) adapter.getValue(idColumnIndex);
            HistoryReference ref = extHistory.getHistoryReference(historyId);
            List<String> tags;
            try {
                tags = ref.getTags();
            } catch (Exception e) {
                LOGGER.error("Failed to fetch tags for history reference {}", historyId, e);
                return component;
            }

            Color mark = colorService.resolveColor(tags);
            if (mark != null) {
                component.setBackground(mark);
                component.setForeground(NeonmarkerColorUtils.contrastingForeground(mark));
            }
            return component;
        }
    }

    @Override
    public void postInstall() {
        super.postInstall();
        if (getView() != null) {
            EventQueue.invokeLater(() -> getNeonmarkerPanel().setTabFocus());
        }
    }

    private PopupMenuHistoryNeonmarker getMenuHistoryColor() {
        if (menuHistoryColor == null) {
            menuHistoryColor = new PopupMenuHistoryNeonmarker();
        }
        return menuHistoryColor;
    }
}
