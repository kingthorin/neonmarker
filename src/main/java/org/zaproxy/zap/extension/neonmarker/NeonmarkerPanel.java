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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.db.DatabaseException;
import org.parosproxy.paros.extension.AbstractPanel;
import org.parosproxy.paros.extension.history.ExtensionHistory;
import org.parosproxy.paros.model.HistoryReference;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.extension.pscan.PassiveScanParam;
import org.zaproxy.zap.view.ZapToggleButton;

@SuppressWarnings("serial")
class NeonmarkerPanel extends AbstractPanel {

    private static final long serialVersionUID = 3053763855777533887L;

    private static final Logger LOGGER = LogManager.getLogger(NeonmarkerPanel.class);

    private static final String UP_ARROW = "\u2191";
    private static final String DOWN_ARROW = "\u2193";
    private static final String MULTIPLICATION_X = "\u2715";

    private Model historyTableModel;
    private List<ExtensionNeonmarker.ColorMapping> colormap;
    private Container colorSelectionPanel;
    private JToolBar toolbar;
    private JButton clearButton;
    private JButton addButton;
    private ZapToggleButton enableButton;
    private ExtensionHistory extHist =
            Control.getSingleton().getExtensionLoader().getExtension(ExtensionHistory.class);

    NeonmarkerPanel(Model model, List<ExtensionNeonmarker.ColorMapping> colormap) {
        historyTableModel = model;
        this.colormap = colormap;
        initializePanel();
    }

    private void initializePanel() {
        setName(Constant.messages.getString("neonmarker.panel.title"));
        setIcon(ExtensionNeonmarker.getIcon());
        setLayout(new BorderLayout());
        add(getPanelToolbar(), BorderLayout.PAGE_START);

        colorSelectionPanel = new JPanel();
        colorSelectionPanel.setLayout(new GridBagLayout());
        add(new JScrollPane(colorSelectionPanel), BorderLayout.CENTER);
        clearColorSelectionPanel();
    }

    private Component getPanelToolbar() {
        if (toolbar == null) {
            toolbar = new JToolBar();
            toolbar.setEnabled(true);
            toolbar.setFloatable(false);
            toolbar.setRollover(true);
            toolbar.add(getClearButton());
            toolbar.add(getAddButton());
            toolbar.add(new JSeparator(SwingConstants.VERTICAL));
            toolbar.add(getEnableToggleButton());
        }
        return toolbar;
    }

    private Component getClearButton() {
        if (clearButton == null) {
            clearButton = new JButton();
            clearButton.setEnabled(true);
            clearButton.setIcon(
                    new ImageIcon(
                            NeonmarkerPanel.class.getResource("/resource/icon/fugue/broom.png")));
            clearButton.setToolTipText(
                    Constant.messages.getString("neonmarker.panel.button.clear"));
            clearButton.addActionListener(actionEvent -> clearColorSelectionPanel());
        }
        return clearButton;
    }

    private Component getAddButton() {
        if (addButton == null) {
            addButton = new JButton();
            addButton.setEnabled(true);
            addButton.setIcon(
                    new ImageIcon(NeonmarkerPanel.class.getResource("/resource/icon/16/103.png")));
            addButton.setToolTipText(Constant.messages.getString("neonmarker.panel.button.add"));
            addButton.addActionListener(
                    actionEvent -> {
                        colormap.add(new ExtensionNeonmarker.ColorMapping());
                        refreshDisplay();
                    });
        }
        return addButton;
    }

    private ZapToggleButton getEnableToggleButton() {
        if (enableButton == null) {
            enableButton =
                    new ZapToggleButton(
                            Constant.messages.getString(
                                    "neonmarker.panel.toolbar.toggle.button.label.enabled"),
                            true);
            enableButton.setIcon(
                    new ImageIcon(
                            NeonmarkerPanel.class.getResource(
                                    ExtensionNeonmarker.RESOURCE + "/off.png")));
            enableButton.setToolTipText(
                    Constant.messages.getString(
                            "neonmarker.panel.toolbar.toggle.button.tooltip.disabled"));
            enableButton.setSelectedIcon(
                    new ImageIcon(
                            NeonmarkerPanel.class.getResource(
                                    ExtensionNeonmarker.RESOURCE + "/on.png")));
            enableButton.setSelectedToolTipText(
                    Constant.messages.getString(
                            "neonmarker.panel.toolbar.toggle.button.tooltip.enabled"));
            enableButton.addItemListener(
                    event -> {
                        if (event.getStateChange() == ItemEvent.SELECTED) {
                            enableButton.setText(
                                    Constant.messages.getString(
                                            "neonmarker.panel.toolbar.toggle.button.label.enabled"));
                        } else {
                            enableButton.setText(
                                    Constant.messages.getString(
                                            "neonmarker.panel.toolbar.toggle.button.label.disabled"));
                        }
                        Control.getSingleton()
                                .getExtensionLoader()
                                .getExtension(ExtensionNeonmarker.class)
                                .toggleHighlighter(enableButton.isSelected());
                    });
        }
        return enableButton;
    }

    private void clearColorSelectionPanel() {
        colormap.clear();
        colormap.add(new ExtensionNeonmarker.ColorMapping());
        refreshDisplay();
    }

    protected void refreshDisplay() {
        colorSelectionPanel.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.VERTICAL;
        c.gridy = 0;
        for (ExtensionNeonmarker.ColorMapping rule : colormap) {
            c.gridx = 0;
            colorSelectionPanel.add(getColorComboBox(rule), c);
            c.gridx++;
            colorSelectionPanel.add(getTagComboBox(rule), c);
            c.gridx++;
            colorSelectionPanel.add(getMoveButton(c.gridy, true), c);
            c.gridx++;
            colorSelectionPanel.add(getMoveButton(c.gridy, false), c);
            c.gridx++;
            colorSelectionPanel.add(getRemoveButton(c.gridy), c);
            c.gridy++;
        }
        c.weightx = 1;
        c.weighty = 1;
        colorSelectionPanel.add(new JLabel(" "), c);
        colorSelectionPanel.validate();
        colorSelectionPanel.repaint();
        repaintHistoryTable();
    }

    private Component getMoveButton(int ruleNumber, boolean up) {
        JButton move = new JButton(up ? UP_ARROW : DOWN_ARROW);
        if ((up && ruleNumber == 0) || (!up && ruleNumber == colormap.size() - 1)) {
            move.setEnabled(false);
        }
        move.setActionCommand(up ? "up" : "down");
        move.setToolTipText(Constant.messages.getString("neonmarker.panel.mapping.move"));
        move.addActionListener(
                e -> {
                    Collections.swap(colormap, ruleNumber, up ? ruleNumber - 1 : ruleNumber + 1);
                    refreshDisplay();
                });
        return move;
    }

    private Component getColorComboBox(ExtensionNeonmarker.ColorMapping rule) {
        JComboBox<Color> colorSelect = new JComboBox<>();
        ExtensionNeonmarker.getPalette().forEach(colorSelect::addItem);
        colorSelect.setRenderer(new ColorListRenderer());
        colorSelect.setSelectedItem(
                isOnPalette(rule.getColor()) ? rule.getColor() : ExtensionNeonmarker.PLACEHOLDER);
        colorSelect.addActionListener(
                actionEvent -> {
                    if (((Color) colorSelect.getSelectedItem())
                            .equals(ExtensionNeonmarker.PLACEHOLDER)) {
                        rule.setColor(
                                JColorChooser.showDialog(
                                        this,
                                        Constant.messages.getString(
                                                "neonmarker.panel.color.chooser.title"),
                                        Color.WHITE));
                        if (rule.getColor() != null && !isOnPalette(rule.getColor())) {
                            ExtensionNeonmarker.addToPalette(rule.getColor());
                            refreshDisplay();
                        }
                    } else {
                        rule.setColor((Color) colorSelect.getSelectedItem());
                        repaintHistoryTable();
                    }
                });
        return colorSelect;
    }

    private static boolean isOnPalette(Color color) {
        if (color == null) {
            return false;
        }
        return ExtensionNeonmarker.getPalette().contains(color);
    }

    private Component getTagComboBox(ExtensionNeonmarker.ColorMapping rule) {
        TagListModel tagListModel = new TagListModel();
        JComboBox<String> tagSelect = new JComboBox<>(tagListModel);
        tagSelect.addPopupMenuListener(
                new PopupMenuListener() {
                    @Override
                    public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                        ((TagListModel) tagSelect.getModel()).updateTags();
                    }

                    @Override
                    public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                        // Nothing to do
                    }

                    @Override
                    public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                        // Nothing to do
                    }
                });
        tagSelect.addActionListener(
                actionEvent -> {
                    rule.setTag((String) tagSelect.getSelectedItem());
                    repaintHistoryTable();
                });
        tagSelect.setSelectedItem(rule.getTag());
        return tagSelect;
    }

    private static void repaintHistoryTable() {
        Control.getSingleton()
                .getExtensionLoader()
                .getExtension(ExtensionHistory.class)
                .getHistoryReferencesTable()
                .repaint();
    }

    private Component getRemoveButton(int ruleNumber) {
        JButton remove = new JButton(MULTIPLICATION_X);
        remove.setToolTipText(Constant.messages.getString("neonmarker.panel.mapping.remove"));
        remove.setActionCommand("remove");
        remove.addActionListener(
                e -> {
                    if (colormap.isEmpty()) {
                        return;
                    }
                    String tag = colormap.get(ruleNumber).getTag();
                    if (tag != null && ExtensionNeonmarker.TAG_PATTERN.matcher(tag).matches()) {
                        switch (getRemovalChoice()) {
                            case JOptionPane.CANCEL_OPTION:
                            case JOptionPane.CLOSED_OPTION:
                                return;
                            case JOptionPane.NO_OPTION:
                                removeMapping(ruleNumber);
                                break;
                            case JOptionPane.YES_OPTION:
                                removeTagFromHistoryTable(tag);
                                removeMapping(ruleNumber);
                                break;
                            default:
                                return;
                        }
                    } else {
                        removeMapping(ruleNumber);
                    }
                    refreshDisplay();
                });
        return remove;
    }

    private void removeMapping(int ruleNumber) {
        colormap.remove(ruleNumber);
        if (colormap.isEmpty()) {
            colormap.add(new ExtensionNeonmarker.ColorMapping());
        }
    }

    private static int getRemovalChoice() {
        return JOptionPane.showConfirmDialog(
                View.getSingleton().getMainFrame(),
                Constant.messages.getString("neonmarker.remove.prompt.message"),
                Constant.messages.getString("neonmarker.remove.prompt.title"),
                JOptionPane.YES_NO_CANCEL_OPTION);
    }

    private List<Integer> getHistoryIds() {
        List<Integer> historyIds = null;
        try {
            historyIds =
                    historyTableModel
                            .getDb()
                            .getTableHistory()
                            .getHistoryIdsOfHistType(
                                    Model.getSingleton().getSession().getSessionId(),
                                    HistoryReference.TYPE_PROXIED,
                                    HistoryReference.TYPE_ZAP_USER,
                                    HistoryReference.TYPE_PROXY_CONNECT);
        } catch (DatabaseException e1) {
            LOGGER.warn("Could not get History IDs");
        }
        if (historyIds == null) {
            return Collections.emptyList();
        }
        return historyIds;
    }

    private void removeTagFromHistoryTable(String tag) {
        SwingWorker<Void, Void> remove =
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        for (int id : getHistoryIds()) {
                            HistoryReference hr = extHist.getHistoryReference(id);
                            hr.deleteTag(tag);
                        }
                        return null;
                    }
                };
        remove.execute();
    }

    /**
     * A list model that dynamically gets all the tags that ZAP has given to any messages in the
     * history
     */
    private class TagListModel implements ComboBoxModel<String> {
        private List<String> allTags;
        private ArrayList<ListDataListener> listDataListeners;
        private Object selectedItem;

        TagListModel() {
            listDataListeners = new ArrayList<>();
            updateTags();
        }

        private void updateTags() {
            try {
                allTags = historyTableModel.getDb().getTableTag().getAllTags();
            } catch (Exception e) {
                // do nothing
            }
            Model.getSingleton()
                    .getOptionsParam()
                    .getParamSet(PassiveScanParam.class)
                    .getAutoTagScanners()
                    .forEach(
                            tagger -> {
                                if (!allTags.contains(tagger.getConf())) {
                                    allTags.add(tagger.getConf());
                                }
                            });
            if (allTags.isEmpty()) {
                allTags.add(Constant.messages.getString("neonmarker.panel.mapping.notags"));
            }
            for (ListDataListener l : listDataListeners) {
                l.contentsChanged(
                        new ListDataEvent(
                                this, ListDataEvent.CONTENTS_CHANGED, 0, allTags.size() - 1));
            }
        }

        @Override
        public int getSize() {
            return allTags.size();
        }

        @Override
        public String getElementAt(int i) {
            return allTags.get(i);
        }

        @Override
        public void addListDataListener(ListDataListener listDataListener) {
            listDataListeners.add(listDataListener);
        }

        @Override
        public void removeListDataListener(ListDataListener listDataListener) {
            listDataListeners.remove(listDataListener);
        }

        @Override
        public void setSelectedItem(Object o) {
            selectedItem = o;
        }

        @Override
        public Object getSelectedItem() {
            return selectedItem;
        }
    }

    /**
     * Renderer for JComboBox that makes colours visible in the UI instead of handling them by name
     * or value.
     */
    private static class ColorListRenderer extends JLabel implements ListCellRenderer<Object> {

        private static final long serialVersionUID = -5808258749585681496L;

        @Override
        public Component getListCellRendererComponent(
                JList<?> jList, Object entry, int i, boolean b, boolean b1) {
            if (((Color) entry).equals(ExtensionNeonmarker.PLACEHOLDER)) {
                setIcon(null);
                setText(Constant.messages.getString("neonmarker.panel.color.menu.custom.label"));
            } else {
                setText("");
                setIcon(getColorIcon((Color) entry));
            }
            this.setBorder(new EmptyBorder(0, 3, 0, 3));
            return this;
        }

        private static ImageIcon getColorIcon(Color color) {
            BufferedImage img = new BufferedImage(100, 16, BufferedImage.TYPE_INT_RGB);
            Graphics graphics = img.createGraphics();
            graphics.setColor(color);
            graphics.fillRect(0, 0, img.getWidth(), img.getHeight());
            return new ImageIcon(img);
        }
    }
}
