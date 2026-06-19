package com.example.kvm.client.ui;

import com.example.kvm.client.api.BackendApiClient;
import com.example.kvm.client.util.SwingTasks;
import com.example.kvm.common.dto.HostInfoDto;
import java.awt.*;
import javax.swing.*;
import net.miginfocom.swing.MigLayout;

public class HostPanel extends PanelSupport {
    private final BackendApiClient api;
    private final JPanel info = new JPanel(new MigLayout("wrap 2, insets 16", "[120!][grow]", "[]8[]"));
    private final JProgressBar memoryBar = new JProgressBar(0, 100);

    public HostPanel(BackendApiClient api, MainFrame frame) {
        super(frame);
        this.api = api;
        JButton refresh = new JButton("刷新");
        refresh.addActionListener(e -> refresh());
        add(toolbar("宿主机概览", refresh), BorderLayout.NORTH);
        add(info, BorderLayout.CENTER);
        refresh();
    }

    private void refresh() {
        SwingTasks.run(api::getHostInfo, this::render, ex -> error("刷新宿主机失败", ex));
    }

    private void render(HostInfoDto h) {
        info.removeAll();
        addRow("主机名", h.hostname);
        addRow("CPU 型号", h.cpuModel);
        addRow("CPU 核心数", String.valueOf(h.cpuCount));
        addRow("CPU 主频", h.cpuMHz + " MHz");
        addRow("总内存", h.totalMemoryMb + " MB");
        addRow("已用内存", h.usedMemoryMb + " MB");
        memoryBar.setValue(h.memoryUsagePercent);
        memoryBar.setStringPainted(true);
        memoryBar.setString(h.memoryUsagePercent + "%");
        info.add(new JLabel("内存使用率"));
        info.add(memoryBar, "growx");
        addRow("虚拟化类型", h.virtualizationType);
        addRow("libvirt 版本", h.libvirtVersion);
        addRow("QEMU 版本", h.qemuVersion);
        addRow("KVM 是否可用", h.kvmEnabled ? "是" : "否");
        addRow("连接 URI", h.connectionUri);
        revalidate();
        repaint();
        frame.log("宿主机信息已刷新");
    }

    private void addRow(String name, String value) {
        JLabel key = new JLabel(name);
        key.setFont(key.getFont().deriveFont(Font.BOLD));
        info.add(key);
        info.add(new JLabel(value), "growx");
    }
}
