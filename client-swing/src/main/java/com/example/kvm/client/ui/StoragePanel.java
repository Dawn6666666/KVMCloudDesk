package com.example.kvm.client.ui;

import com.example.kvm.client.api.BackendApiClient;
import com.example.kvm.client.util.SwingTasks;
import com.example.kvm.common.dto.StoragePoolInfoDto;
import com.example.kvm.common.dto.StorageVolumeInfoDto;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class StoragePanel extends PanelSupport {
    private final BackendApiClient api;
    private final DefaultTableModel poolModel = model("名称", "状态", "自动启动", "路径", "总容量", "已分配", "可用容量");
    private final DefaultTableModel volumeModel = model("名称", "路径", "类型", "总容量", "已分配容量");
    private final JTable poolTable = table(poolModel);
    private final JTable volumeTable = table(volumeModel);

    public StoragePanel(BackendApiClient api, MainFrame frame) {
        super(frame);
        this.api = api;
        JButton refresh = new JButton("刷新存储池");
        JButton volumes = new JButton("查看存储卷");
        refresh.addActionListener(e -> refreshPools());
        volumes.addActionListener(e -> refreshVolumes());
        add(toolbar("存储管理", refresh, volumes), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(poolTable), new JScrollPane(volumeTable));
        split.setDividerLocation(230);
        add(split, BorderLayout.CENTER);
        refreshPools();
    }

    private void refreshPools() {
        SwingTasks.run(api::listPools, this::renderPools, ex -> error("刷新存储池失败", ex));
    }

    private void renderPools(List<StoragePoolInfoDto> list) {
        poolModel.setRowCount(0);
        for (StoragePoolInfoDto p : list) {
            poolModel.addRow(new Object[]{p.name, p.active ? "活动" : "停止", p.autostart ? "是" : "否",
                    p.path, p.capacityGb + " GB", p.allocationGb + " GB", p.availableGb + " GB"});
        }
        frame.log("存储池列表已刷新");
        if (!list.isEmpty()) {
            poolTable.setRowSelectionInterval(0, 0);
            refreshVolumes();
        }
    }

    private void refreshVolumes() {
        int row = poolTable.getSelectedRow();
        if (row < 0) return;
        String pool = poolModel.getValueAt(poolTable.convertRowIndexToModel(row), 0).toString();
        SwingTasks.run(() -> api.listVolumes(pool), this::renderVolumes, ex -> error("刷新存储卷失败", ex));
    }

    private void renderVolumes(List<StorageVolumeInfoDto> list) {
        volumeModel.setRowCount(0);
        for (StorageVolumeInfoDto v : list) {
            volumeModel.addRow(new Object[]{v.name, v.path, v.type, v.capacityGb + " GB", v.allocationGb + " GB"});
        }
        frame.log("存储卷列表已刷新");
    }
}
