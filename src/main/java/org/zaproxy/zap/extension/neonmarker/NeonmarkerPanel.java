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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
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
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.db.DatabaseException;
import org.parosproxy.paros.extension.AbstractPanel;
import org.parosproxy.paros.extension.history.ExtensionHistory;
import org.parosproxy.paros.model.HistoryReference;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.view.View;
import org.zaproxy.addon.pscan.ExtensionPassiveScan2;
import org.zaproxy.zap.utils.DisplayUtils;
import org.zaproxy.zap.view.ZapToggleButton;

@SuppressWarnings("serial")
class NeonmarkerPanel extends AbstractPanel {

    private static final long serialVersionUID = 3053763855777533887L;

    private static final Logger LOGGER = LogManager.getLogger(NeonmarkerPanel.class);

    private Model historyTableModel;
    private NeonmarkerColorService colorService;
    private Container colorSelectionPanel;
    private ZapToggleButton enableButton;
    private ExtensionHistory extHist;

    NeonmarkerPanel(Model model, NeonmarkerColorService colorService) {
        historyTableModel = model;
        this.colorService = colorService;
        extHist = ExtensionNeonmarker.getExtension(ExtensionHistory.class);
        initializePanel();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (colorService != null && colorSelectionPanel != null) {
            refreshDisplay();
        }
    }

    private void initializePanel() {
        setName(Constant.messages.getString("neonmarker.panel.title"));
        setIcon(ExtensionNeonmarker.getIcon());
        setLayout(new BorderLayout());

        JToolBar toolbar = new JToolBar();
        toolbar.setEnabled(true);
        toolbar.setFloatable(false);
        toolbar.setRollover(true);

        JButton clearButton = new JButton();
        clearButton.setEnabled(true);
        clearButton.setIcon(
                DisplayUtils.getScaledIcon(
                        NeonmarkerPanel.class.getResource("/resource/icon/fugue/broom.png")));
        clearButton.setToolTipText(Constant.messages.getString("neonmarker.panel.button.clear"));
        clearButton.addActionListener(actionEvent -> clearColorSelectionPanel());
        toolbar.add(clearButton);

        JButton addButton = new JButton();
        addButton.setEnabled(true);
        addButton.setIcon(
                DisplayUtils.getScaledIcon(
                        NeonmarkerPanel.class.getResource("/resource/icon/16/103.png")));
        addButton.setToolTipText(Constant.messages.getString("neonmarker.panel.button.add"));
        addButton.addActionListener(actionEvent -> colorService.addEmptyRule());
        toolbar.add(addButton);
        toolbar.add(new JSeparator(SwingConstants.VERTICAL));

        enableButton =
                new ZapToggleButton(
                        Constant.messages.getString(
                                "neonmarker.panel.toolbar.toggle.button.label.enabled"),
                        true);
        enableButton.setIcon(
                DisplayUtils.getScaledIcon(
                        NeonmarkerPanel.class.getResource(
                                ExtensionNeonmarker.RESOURCE + "/off.png")));
        enableButton.setToolTipText(
                Constant.messages.getString(
                        "neonmarker.panel.toolbar.toggle.button.tooltip.disabled"));
        enableButton.setSelectedIcon(
                DisplayUtils.getScaledIcon(
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
                    ExtensionNeonmarker.getExtension(ExtensionNeonmarker.class)
                            .toggleHighlighter(enableButton.isSelected());
                });
        toolbar.add(enableButton);

        add(toolbar, BorderLayout.PAGE_START);

        colorSelectionPanel = new JPanel();
        colorSelectionPanel.setLayout(new GridBagLayout());
        add(new JScrollPane(colorSelectionPanel), BorderLayout.CENTER);
        clearColorSelectionPanel();
        // Extension field still null here, so service onChange cannot refresh yet.
        refreshDisplay();
    }

    private void clearColorSelectionPanel() {
        colorService.clearRules();
    }

    protected void refreshDisplay() {
        colorSelectionPanel.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.VERTICAL;
        c.gridy = 0;
        for (ColorMapping rule : colorService.getColorRules()) {
            c.gridx = 0;
            colorSelectionPanel.add(getColorComboBox(rule), c);
            c.gridx++;
            colorSelectionPanel.add(getTagComboBox(rule), c);
            c.gridx++;
            colorSelectionPanel.add(getMoveAbsoluteButton(c.gridy, true), c);
            c.gridx++;
            colorSelectionPanel.add(getMoveButton(c.gridy, true), c);
            c.gridx++;
            colorSelectionPanel.add(getMoveButton(c.gridy, false), c);
            c.gridx++;
            colorSelectionPanel.add(getMoveAbsoluteButton(c.gridy, false), c);
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
        JButton move =
                new JButton(
                        Constant.messages.getString(
                                up
                                        ? "neonmarker.panel.mapping.move.up.label"
                                        : "neonmarker.panel.mapping.move.down.label"));
        if ((up && ruleNumber == 0)
                || (!up && ruleNumber == colorService.getColorRules().size() - 1)) {
            move.setEnabled(false);
        }
        move.setActionCommand(up ? "up" : "down");
        move.setToolTipText(Constant.messages.getString("neonmarker.panel.mapping.move"));
        move.addActionListener(e -> colorService.swapRules(ruleNumber, up));
        return move;
    }

    private Component getMoveAbsoluteButton(int ruleNumber, boolean top) {
        JButton move =
                new JButton(
                        Constant.messages.getString(
                                top
                                        ? "neonmarker.panel.mapping.move.top.label"
                                        : "neonmarker.panel.mapping.move.bottom.label"));
        int lastIndex = colorService.getColorRules().size() - 1;
        if ((top && ruleNumber == 0) || (!top && ruleNumber == lastIndex)) {
            move.setEnabled(false);
        }
        move.setToolTipText(
                Constant.messages.getString(
                        top
                                ? "neonmarker.panel.mapping.move.top"
                                : "neonmarker.panel.mapping.move.bottom"));
        move.addActionListener(
                e -> {
                    if (top) {
                        colorService.moveRuleToTop(ruleNumber);
                    } else {
                        colorService.moveRuleToBottom(ruleNumber);
                    }
                });
        return move;
    }

    private Component getColorComboBox(ColorMapping rule) {
        JComboBox<Color> colorSelect = new JComboBox<>();
        NeonmarkerColorService.getPalette().forEach(colorSelect::addItem);
        colorSelect.setRenderer(new ColorListRenderer());
        colorSelect.setSelectedItem(
                isOnPalette(rule.getColor())
                        ? rule.getColor()
                        : NeonmarkerColorService.PLACEHOLDER);
        colorSelect.addActionListener(
                actionEvent -> {
                    if (((Color) colorSelect.getSelectedItem())
                            .equals(NeonmarkerColorService.PLACEHOLDER)) {
                        Color previous = rule.getColor();
                        Color chosen =
                                JColorChooser.showDialog(
                                        this,
                                        Constant.messages.getString(
                                                "neonmarker.panel.color.chooser.title"),
                                        Color.WHITE);
                        if (chosen == null) {
                            colorSelect.setSelectedItem(
                                    isOnPalette(previous)
                                            ? previous
                                            : NeonmarkerColorService.PLACEHOLDER);
                            return;
                        }
                        rule.setColor(chosen);
                        if (!isOnPalette(chosen)) {
                            NeonmarkerColorService.addToPalette(chosen);
                            refreshDisplay();
                        } else {
                            repaintHistoryTable();
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
        return NeonmarkerColorService.getPalette().contains(color);
    }

    private Component getTagComboBox(ColorMapping rule) {
        JComboBox<String> tagSelect = new JComboBox<>();
        populateTagComboBox(tagSelect);
        selectTag(tagSelect, rule.getTag());
        final AtomicBoolean refreshingTags = new AtomicBoolean(false);
        tagSelect.addPopupMenuListener(
                new PopupMenuListener() {
                    @Override
                    public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                        refreshingTags.set(true);
                        try {
                            tagSelect.removeAllItems();
                            populateTagComboBox(tagSelect);
                            selectTag(tagSelect, rule.getTag());
                        } finally {
                            refreshingTags.set(false);
                        }
                    }

                    @Override
                    public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {}

                    @Override
                    public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {}
                });
        tagSelect.addActionListener(
                actionEvent -> {
                    if (refreshingTags.get()) {
                        return;
                    }
                    rule.setTag((String) tagSelect.getSelectedItem());
                    repaintHistoryTable();
                });
        return tagSelect;
    }

    private static void selectTag(JComboBox<String> tagSelect, String tag) {
        if (tag == null) {
            tagSelect.setSelectedIndex(-1);
            return;
        }
        for (int i = 0; i < tagSelect.getItemCount(); i++) {
            if (tag.equals(tagSelect.getItemAt(i))) {
                tagSelect.setSelectedIndex(i);
                return;
            }
        }
        tagSelect.insertItemAt(tag, 0);
        tagSelect.setSelectedIndex(0);
    }

    private void populateTagComboBox(JComboBox<String> tagSelect) {
        for (String tag : loadTags()) {
            tagSelect.addItem(tag);
        }
    }

    private List<String> loadTags() {
        List<String> allTags = new ArrayList<>();
        try {
            allTags.addAll(historyTableModel.getDb().getTableTag().getAllTags());
        } catch (DatabaseException e) {
            LOGGER.debug("Failed to load tags from DB, falling back to pscan auto-tags.", e);
        }
        ExtensionNeonmarker.getExtension(ExtensionPassiveScan2.class).getAutoTaggingTags().stream()
                .filter(tag -> !allTags.contains(tag))
                .forEach(allTags::add);
        if (allTags.isEmpty()) {
            allTags.add(Constant.messages.getString("neonmarker.panel.mapping.notags"));
        }
        return allTags;
    }

    private static void repaintHistoryTable() {
        ExtensionNeonmarker.getExtension(ExtensionHistory.class)
                .getHistoryReferencesTable()
                .repaint();
    }

    private Component getRemoveButton(int ruleNumber) {
        JButton remove =
                new JButton(Constant.messages.getString("neonmarker.panel.mapping.remove.label"));
        remove.setToolTipText(Constant.messages.getString("neonmarker.panel.mapping.remove"));
        remove.setActionCommand("remove");
        remove.addActionListener(
                e -> {
                    if (colorService.getColorRules().isEmpty()) {
                        return;
                    }
                    String tag = colorService.getColorRules().get(ruleNumber).getTag();
                    if (tag != null && ExtensionNeonmarker.TAG_PATTERN.matcher(tag).matches()) {
                        switch (getRemovalChoice()) {
                            case JOptionPane.CANCEL_OPTION:
                            case JOptionPane.CLOSED_OPTION:
                                return;
                            case JOptionPane.NO_OPTION:
                                colorService.removeRule(ruleNumber);
                                break;
                            case JOptionPane.YES_OPTION:
                                removeTagFromHistoryTable(tag);
                                colorService.removeRule(ruleNumber);
                                break;
                            default:
                                return;
                        }
                    } else {
                        colorService.removeRule(ruleNumber);
                    }
                });
        return remove;
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
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                for (int id : getHistoryIds()) {
                    extHist.getHistoryReference(id).deleteTag(tag);
                }
                return null;
            }

            @Override
            protected void done() {
                repaintHistoryTable();
            }
        }.execute();
    }

    /**
     * Renderer for JComboBox that makes colours visible in the UI instead of handling them by name
     * or value.
     */
    private static class ColorListRenderer extends JLabel implements ListCellRenderer<Object> {

        private static final long serialVersionUID = -5808258749585681496L;

        // Unbounded swatch cache (final map reference, mutable entries). Fine for the fixed
        // palette plus occasional custom colours; revisit if many temporary colours are added
        // programmatically.
        private static final Map<Color, ImageIcon> COLOR_ICONS = new HashMap<>();

        @Override
        public Component getListCellRendererComponent(
                JList<?> jList, Object entry, int i, boolean b, boolean b1) {
            if (((Color) entry).equals(NeonmarkerColorService.PLACEHOLDER)) {
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
            return COLOR_ICONS.computeIfAbsent(
                    color,
                    c -> {
                        BufferedImage img = new BufferedImage(100, 16, BufferedImage.TYPE_INT_RGB);
                        Graphics graphics = img.createGraphics();
                        graphics.setColor(c);
                        graphics.fillRect(0, 0, img.getWidth(), img.getHeight());
                        graphics.dispose();
                        return DisplayUtils.getScaledIcon(new ImageIcon(img));
                    });
        }
    }
}
