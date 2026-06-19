package com.example.kvm.client.ui;

import com.example.kvm.client.api.BackendApiClient;
import com.example.kvm.client.util.SwingTasks;
import com.example.kvm.common.dto.NetworkInfoDto;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class NetworkPanel extends PanelSupport {
    private final BackendApiClient api;
    private final DefaultTableModel model = model("名称", "状态", "自动启动", "网桥名称", "转发模式", "IP 地址", "DHCP 起始地址", "DHCP 结束地址");
    private final JTable table = table(model);

    public NetworkPanel(BackendApiClient api, MainFrame frame) {
        super(frame);
        this.api = api;
        JButton refresh = new JButton("刷新");
        JButton start = new JButton("启动网络");
        JButton stop = new JButton("停止网络");
        refresh.addActionListener(e -> refresh());
        start.addActionListener(e -> action("启动", api::startNetwork));
        stop.addActionListener(e -> action("停止", api::stopNetwork));
        add(toolbar("网络管理", refresh, start, stop), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    private void refresh() {
        SwingTasks.run(api::listNetworks, this::render, ex -> error("刷新网络失败", ex));
    }

    private void render(List<NetworkInfoDto> list) {
        model.setRowCount(0);
        for (NetworkInfoDto n : list) {
            model.addRow(new Object[]{n.name, n.active ? "活动" : "停止", n.autostart ? "是" : "否",
                    n.bridgeName, n.forwardMode, n.ipAddress, n.dhcpStart, n.dhcpEnd});
        }
        frame.log("网络列表已刷新");
    }

    private void action(String label, NetOperation op) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择网络");
            return;
        }
        String name = model.getValueAt(table.convertRowIndexToModel(row), 0).toString();
        if (!confirm("确认" + label + "网络 " + name + "？")) return;
        SwingTasks.run(() -> {
            op.apply(name);
            return null;
        }, ok -> {
            frame.log(label + "网络 " + name + " 成功");
            refresh();
        }, ex -> error(label + "网络失败", ex));
    }

    private interface NetOperation {
        void apply(String name);
    }
}
