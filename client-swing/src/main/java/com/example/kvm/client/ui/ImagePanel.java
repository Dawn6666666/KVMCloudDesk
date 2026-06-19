package com.example.kvm.client.ui;

import com.example.kvm.client.api.BackendApiClient;
import com.example.kvm.client.util.SwingTasks;
import com.example.kvm.common.dto.ImageInfoDto;
import com.example.kvm.common.request.AddImageRequest;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

public class ImagePanel extends PanelSupport {
    private final BackendApiClient api;
    private final DefaultTableModel model = model("名称", "格式", "大小", "路径", "创建时间", "描述");
    private final JTable table = table(model);

    public ImagePanel(BackendApiClient api, MainFrame frame) {
        super(frame);
        this.api = api;
        JButton refresh = new JButton("刷新");
        JButton add = new JButton("添加镜像");
        JButton delete = new JButton("删除镜像");
        refresh.addActionListener(e -> refresh());
        add.addActionListener(e -> addImage());
        delete.addActionListener(e -> deleteImage());
        add(toolbar("镜像管理", refresh, add, delete), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    private void refresh() {
        SwingTasks.run(api::listImages, this::render, ex -> error("刷新镜像失败", ex));
    }

    private void render(List<ImageInfoDto> list) {
        model.setRowCount(0);
        for (ImageInfoDto i : list) {
            model.addRow(new Object[]{i.name, i.format, i.sizeGb + " GB", i.path, i.createTime, i.description});
        }
        frame.log("镜像列表已刷新");
    }

    private void addImage() {
        JPanel form = new JPanel(new MigLayout("wrap 2", "[90!][300!]"));
        JTextField name = new JTextField();
        JTextField path = new JTextField();
        JTextField desc = new JTextField();
        form.add(new JLabel("镜像名称")); form.add(name, "growx");
        form.add(new JLabel("镜像路径")); form.add(path, "growx");
        form.add(new JLabel("描述")); form.add(desc, "growx");
        if (JOptionPane.showConfirmDialog(this, form, "添加镜像", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        AddImageRequest req = new AddImageRequest();
        req.name = name.getText();
        req.path = path.getText();
        req.description = desc.getText();
        SwingTasks.run(() -> api.addImage(req), image -> {
            frame.log("添加镜像 " + image.name + " 成功");
            refresh();
        }, ex -> error("添加镜像失败", ex));
    }

    private void deleteImage() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择镜像");
            return;
        }
        String name = model.getValueAt(table.convertRowIndexToModel(row), 0).toString();
        if (!confirm("确认删除镜像 " + name + "？")) return;
        SwingTasks.run(() -> {
            api.deleteImage(name);
            return null;
        }, ok -> {
            frame.log("删除镜像 " + name + " 成功");
            refresh();
        }, ex -> error("删除镜像失败", ex));
    }
}
