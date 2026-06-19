package com.example.kvm.client.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

abstract class PanelSupport extends JPanel {
    protected final MainFrame frame;

    PanelSupport(MainFrame frame) {
        super(new BorderLayout(10, 10));
        this.frame = frame;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    protected JPanel toolbar(String title, JButton... buttons) {
        JPanel panel = new JPanel(new MigLayout("insets 0", "[][grow][]", "[]"));
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 16f));
        panel.add(label);
        panel.add(new JLabel(), "growx");
        for (JButton button : buttons) {
            panel.add(button);
        }
        return panel;
    }

    protected JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(26);
        return table;
    }

    protected DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    protected void error(String message, Exception ex) {
        frame.log(message + "：" + ex.getMessage());
        JOptionPane.showMessageDialog(this, ex.getMessage(), "操作失败", JOptionPane.ERROR_MESSAGE);
    }

    protected boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "确认操作", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
