package com.example.kvm.client.ui;

import com.example.kvm.client.api.BackendApiClient;
import com.example.kvm.client.util.SwingTasks;
import com.example.kvm.common.dto.SnapshotInfoDto;
import com.example.kvm.common.dto.VmInfoDto;
import com.example.kvm.common.request.CreateSnapshotRequest;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

public class SnapshotPanel extends PanelSupport {
    private final BackendApiClient api;
    private final JComboBox<String> vmBox = new JComboBox<>();
    private final DefaultTableModel model = model("快照名称", "虚拟机名称", "创建时间", "状态", "描述");
    private final JTable table = table(model);

    public SnapshotPanel(BackendApiClient api, MainFrame frame) {
        super(frame);
        this.api = api;
        JButton refresh = new JButton("刷新");
        JButton create = new JButton("创建快照");
        JButton revert = new JButton("恢复快照");
        JButton delete = new JButton("删除快照");
        refresh.addActionListener(e -> loadVms());
        create.addActionListener(e -> create());
        revert.addActionListener(e -> snapshotAction("恢复", api::revertSnapshot));
        delete.addActionListener(e -> snapshotAction("删除", api::deleteSnapshot));
        JPanel top = new JPanel(new MigLayout("insets 0", "[][220!]push[][][][]", "[]"));
        top.add(new JLabel("虚拟机"));
        top.add(vmBox);
        top.add(refresh);
        top.add(create);
        top.add(revert);
        top.add(delete);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        vmBox.addActionListener(e -> refreshSnapshots());
        loadVms();
    }

    private void loadVms() {
        SwingTasks.run(api::listVms, list -> {
            vmBox.removeAllItems();
            for (VmInfoDto vm : list) vmBox.addItem(vm.name);
            refreshSnapshots();
        }, ex -> error("加载虚拟机失败", ex));
    }

    private void refreshSnapshots() {
        String vm = (String) vmBox.getSelectedItem();
        if (vm == null) return;
        SwingTasks.run(() -> api.listSnapshots(vm), this::render, ex -> error("刷新快照失败", ex));
    }

    private void render(List<SnapshotInfoDto> list) {
        model.setRowCount(0);
        for (SnapshotInfoDto s : list) {
            model.addRow(new Object[]{s.name, s.vmName, s.createTime, s.state, s.description});
        }
        frame.log("快照列表已刷新");
    }

    private void create() {
        String vm = (String) vmBox.getSelectedItem();
        if (vm == null) return;
        JTextField name = new JTextField("snapshot-" + System.currentTimeMillis());
        JTextField desc = new JTextField();
        JPanel form = new JPanel(new MigLayout("wrap 2", "[80!][260!]"));
        form.add(new JLabel("快照名称")); form.add(name, "growx");
        form.add(new JLabel("描述")); form.add(desc, "growx");
        if (JOptionPane.showConfirmDialog(this, form, "创建快照", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        CreateSnapshotRequest req = new CreateSnapshotRequest();
        req.name = name.getText();
        req.description = desc.getText();
        SwingTasks.run(() -> api.createSnapshot(vm, req), snap -> {
            frame.log("创建快照 " + snap.name + " 成功");
            refreshSnapshots();
        }, ex -> error("创建快照失败", ex));
    }

    private void snapshotAction(String label, SnapshotOperation op) {
        String vm = (String) vmBox.getSelectedItem();
        int row = table.getSelectedRow();
        if (vm == null || row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择快照");
            return;
        }
        String snapshot = model.getValueAt(table.convertRowIndexToModel(row), 0).toString();
        if (!confirm("确认" + label + "快照 " + snapshot + "？")) return;
        SwingTasks.run(() -> {
            op.apply(vm, snapshot);
            return null;
        }, ok -> {
            frame.log(label + "快照 " + snapshot + " 成功");
            refreshSnapshots();
        }, ex -> error(label + "快照失败", ex));
    }

    private interface SnapshotOperation {
        void apply(String vm, String snapshot);
    }
}
