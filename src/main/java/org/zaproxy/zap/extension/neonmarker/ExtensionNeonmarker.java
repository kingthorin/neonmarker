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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;
import org.apache.commons.lang3.Range;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.db.DatabaseException;
import org.parosproxy.paros.extension.Extension;
import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.extension.ExtensionHook;
import org.parosproxy.paros.extension.history.ExtensionHistory;
import org.parosproxy.paros.model.HistoryReference;
import org.zaproxy.addon.pscan.ExtensionPassiveScan2;
import org.zaproxy.zap.extension.help.ExtensionHelp;
import org.zaproxy.zap.view.table.HistoryReferencesTableModel;

public class ExtensionNeonmarker extends ExtensionAdaptor {
    private static final Logger LOGGER = LogManager.getLogger(ExtensionNeonmarker.class);
    private static final Range<Integer> INT_RANGE = Range.of(Integer.MIN_VALUE, Integer.MAX_VALUE);
    private static final List<Class<? extends Extension>> EXTENSION_DEPENDENCIES =
            List.of(ExtensionPassiveScan2.class, ExtensionHistory.class);

    public static final String NAME = "ExtensionNeonmarker";
    public static final Color PLACEHOLDER = new Color(0, 0, 0, 0);
    public static final String RESOURCE = "/org/zaproxy/zap/extension/neonmarker/resources";

    public static final String TAG_PREFIX = "neon_";
    public static final Pattern TAG_PATTERN =
            Pattern.compile(
                    ExtensionNeonmarker.TAG_PREFIX
                            + "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12}");

    private static List<Color> palette;
    private static ImageIcon icon;

    private ArrayList<ColorMapping> colormap;
    private NeonmarkerPanel neonmarkerPanel;
    private MarkItemColorHighlighter highlighter;
    private PopupMenuHistoryNeonmarker menuHistoryColor;

    public ExtensionNeonmarker() {
        super(NAME);
        getPalette();
    }

    @Override
    public void hook(ExtensionHook extensionHook) {
        colormap = new ArrayList<>();

        if (hasView()) {
            toggleHighlighter(true);
            extensionHook.getHookView().addWorkPanel(getNeonmarkerPanel());
            ExtensionHelp.enableHelpKey(getNeonmarkerPanel(), "neonmarker");

            extensionHook.getHookMenu().addPopupMenuItem(getMenuHistoryColor());
        }
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
        neonmarkerPanel = null;
        toggleHighlighter(false);
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

    static List<Color> getPalette() {
        if (palette == null) {
            palette = new ArrayList<>(17);
            palette.addAll(
                    Arrays.asList(
                            // RAINBOW HACKER THEME
                            new Color(0xff8080),
                            new Color(0xffc080),
                            new Color(0xffff80),
                            new Color(0xc0ff80),
                            new Color(0x80ff80),
                            new Color(0x80ffc0),
                            new Color(0x80ffff),
                            new Color(0x80c0ff),
                            new Color(0x8080ff),
                            new Color(0xc080ff),
                            new Color(0xff80ff),
                            new Color(0xff80c0),
                            // CORPORATE EDITION
                            new Color(0xe0ffff),
                            new Color(0xa8c0c0),
                            new Color(0x708080),
                            new Color(0x384040),
                            // Placeholder
                            PLACEHOLDER));
        }
        return palette;
    }

    NeonmarkerPanel getNeonmarkerPanel() {
        if (neonmarkerPanel == null) {
            neonmarkerPanel = new NeonmarkerPanel(getHistoryExtension().getModel(), colormap);
        }
        return neonmarkerPanel;
    }

    static ImageIcon getIcon() {
        if (icon == null) {
            icon = new ImageIcon(ExtensionNeonmarker.class.getResource(RESOURCE + "/spectrum.png"));
        }
        return icon;
    }

    private static ExtensionHistory getHistoryExtension() {
        return getExtension(ExtensionHistory.class);
    }

    private static <T extends Extension> T getExtension(Class<T> clazz) {
        return Control.getSingleton().getExtensionLoader().getExtension(clazz);
    }

    private MarkItemColorHighlighter getHighligher() {
        if (highlighter == null) {
            int idColumnIndex =
                    getHistoryExtension()
                            .getHistoryReferencesTable()
                            .getModel()
                            .getColumnIndex(HistoryReferencesTableModel.Column.HREF_ID);
            highlighter = new MarkItemColorHighlighter(getHistoryExtension(), idColumnIndex);
        }
        return highlighter;
    }

    protected void toggleHighlighter(boolean on) {
        if (on) {
            getHistoryExtension().getHistoryReferencesTable().setHighlighters(getHighligher());
        } else {
            getHistoryExtension().getHistoryReferencesTable().removeHighlighter(getHighligher());
        }
    }

    /**
     * Allows the addition of a color mapping between a {@code Color} and passive scan Tag.
     *
     * @param tag the name of the tag to be mapped.
     * @param color the {@code int} representation of the color to be mapped
     * @return whether or not the addition of the color mapping was successful ({@code true} if it
     *     was, {@code false} otherwise).
     */
    public boolean addColorMapping(String tag, int color) {
        if (isValidTag(tag) && isValidColor(color)) {
            Color newColor = new Color(color);
            ColorMapping newMapping = new ColorMapping(tag, newColor);
            if (!getColorMap().contains(newMapping)) {
                getColorMap().add(newMapping);
            }
            if (!palette.contains(newColor)) {
                addToPalette(newColor);
            }
            getNeonmarkerPanel().refreshDisplay();
            return true;
        }
        LOGGER.debug("Either the tag: \"{}\" or the color: \"{}\" was invalid.", tag, color);
        return false;
    }

    public static void addToPalette(Color addColor) {
        palette.add(addColor);
    }

    private static boolean isValidColor(int colorValue) {
        return INT_RANGE.contains(colorValue);
    }

    private static boolean isValidTag(String tag) {
        try {
            return getExtension(ExtensionPassiveScan2.class).getAutoTaggingTags().contains(tag)
                    || getHistoryExtension()
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

    private List<ColorMapping> getColorMap() {
        return colormap;
    }

    private class MarkItemColorHighlighter extends AbstractHighlighter {
        private int idColumnIndex;
        private ExtensionHistory extHistory;

        MarkItemColorHighlighter(ExtensionHistory extHistory, int idColumnIndex) {
            super();
            setHighlightPredicate(HighlightPredicate.ALWAYS);
            this.extHistory = extHistory;
            this.idColumnIndex = idColumnIndex;
        }

        @Override
        protected Component doHighlight(Component component, ComponentAdapter adapter) {
            HistoryReference ref =
                    extHistory.getHistoryReference((int) adapter.getValue(idColumnIndex));
            List<String> tags;
            try {
                tags = ref.getTags();
            } catch (Exception e) {
                LOGGER.error("Failed to fetch tags for history reference");
                return component;
            }

            Color mark = mapTagsToColor(tags);
            if (mark != null) {
                component.setBackground(mark);
            }
            return component;
        }

        private Color mapTagsToColor(List<String> tags) {
            for (ColorMapping colorMapping : colormap) {
                if (tags.contains(colorMapping.tag)) {
                    return colorMapping.color;
                }
            }
            return null;
        }
    }

    static class ColorMapping implements Serializable {

        private static final long serialVersionUID = -1054428983726909074L;
        private String tag;
        private Color color;

        ColorMapping() {
            this.tag = null;
            this.color = palette.get(0);
        }

        ColorMapping(String tag, Color color) {
            this.tag = tag;
            this.color = color;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        @Override
        public int hashCode() {
            return Objects.hash(color, tag);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ColorMapping other)) {
                return false;
            }
            return Objects.equals(color, other.color) && Objects.equals(tag, other.tag);
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
