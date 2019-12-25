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

import org.apache.commons.lang3.Range;
import org.apache.log4j.Logger;
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
import org.zaproxy.zap.extension.help.ExtensionHelp;
import org.zaproxy.zap.view.table.DefaultHistoryReferencesTableModel;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExtensionNeonmarker extends ExtensionAdaptor {
    private static final Logger LOGGER = Logger.getLogger(ExtensionNeonmarker.class);
    private static final Range<Integer> INT_RANGE = Range.between(Integer.MIN_VALUE,
            Integer.MAX_VALUE);
    private static final List<Class<? extends Extension>> EXTENSION_DEPENDENCIES;

    static {
        List<Class<? extends Extension>> dependencies = new ArrayList<>(1);
        dependencies.add(ExtensionHistory.class);
        EXTENSION_DEPENDENCIES = Collections.unmodifiableList(dependencies);
    }

    public static final String NAME = "ExtensionNeonmarker";
    private ArrayList<ColorMapping> colormap;
    private NeonmarkerPanel neonmarkerPanel;
    private MarkItemColorHighlighter highlighter = null;

    static Color[] palette = {
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
            new Color(0x384040)
    };

    public ExtensionNeonmarker() {
        super(NAME);
    }

    public void hook(ExtensionHook extensionHook) {
        getHistoryExtension().getHistoryReferencesTable().setHighlighters(getHighligher());

        colormap = new ArrayList<>();

        if (getView() != null) {
            extensionHook.getHookView().addWorkPanel(getNeonmarkerPanel());
            ExtensionHelp.enableHelpKey(getNeonmarkerPanel(), "neonmarker");
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
        Control.getSingleton().getExtensionLoader().removeWorkPanel(getNeonmarkerPanel());
        neonmarkerPanel = null;
        getHistoryExtension().getHistoryReferencesTable().removeHighlighter(getHighligher());
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

    private NeonmarkerPanel getNeonmarkerPanel() {
        if (neonmarkerPanel == null) {
            neonmarkerPanel = new NeonmarkerPanel(getHistoryExtension().getModel(), colormap);
        }
        return neonmarkerPanel;
    }

    private ExtensionHistory getHistoryExtension() {
        return Control.getSingleton().getExtensionLoader().getExtension(ExtensionHistory.class);
    }

    private MarkItemColorHighlighter getHighligher() {
        if (highlighter == null) {
            int idColumnIndex = getHistoryExtension().getHistoryReferencesTable().getModel()
                    .getColumnIndex(DefaultHistoryReferencesTableModel.Column.HREF_ID);
            highlighter = new MarkItemColorHighlighter(getHistoryExtension(), idColumnIndex);
        }
        return highlighter;
    }

    /**
     * Allows the addition of a color mapping between a {@code Color} and passive scan Tag.
     *
     * @param tag the name of the tag to be mapped.
     * @param color the {@code int} representation of the color to be mapped
     * @return whether or not the addition of the color mapping was successful ({@code true} if it
     *         was, {@code false} otherwise).
     */
    public boolean addColorMapping(String tag, int color) {
        if (isValidTag(tag) && isValidColor(color)) {
            getColorMap().add(new ColorMapping(tag, new Color(color)));
            getNeonmarkerPanel().refreshDisplay();
            return true;
        }
        return false;
    }

    private boolean isValidColor(int colorValue) {
        return INT_RANGE.contains(colorValue);
    }

    private boolean isValidTag(String tag) {
        try {
            return getHistoryExtension().getModel().getDb().getTableTag().getAllTags()
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
            HistoryReference ref = extHistory
                    .getHistoryReference((int) adapter.getValue(idColumnIndex));
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

    static class ColorMapping {
        public String tag;
        public Color color;

        ColorMapping() {
            this.tag = null;
            this.color = palette[0];
        }

        ColorMapping(String tag, Color color) {
            this.tag = tag;
            this.color = color;
        }
    }
}
