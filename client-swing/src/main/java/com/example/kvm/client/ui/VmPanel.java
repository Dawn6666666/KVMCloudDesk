package com.example.kvm.client.ui;

import com.example.kvm.client.api.BackendApiClient;
import com.example.kvm.client.util.SwingTasks;
import com.example.kvm.common.dto.*;
import com.example.kvm.common.request.CreateVmRequest;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

public class VmPanel extends PanelSupport {
    private final BackendApiClient api;
    private final DefaultTableModel model = model("名称", "状态", "CPU", "内存", "磁盘路径", "磁盘大小", "网络", "IP 地址", "自动启动", "持久化", "描述");
    private final JTable table = table(model);

    public VmPanel(BackendApiClient api, MainFrame frame) {
        super(frame);
        this.api = api;
        JButton refresh = button("刷新", this::refresh);
        JButton create = button("创建", this::create);
        JButton start = button("启动", () -> vmAction("启动", api::startVm));
        JButton shutdown = button("关机", () -> vmAction("关机", api::shutdownVm));
        JButton destroy = button("强制关闭", () -> vmAction("强制关闭", api::destroyVm));
        JButton suspend = button("暂停", () -> vmAction("暂停", api::suspendVm));
        JButton resume = button("恢复", () -> vmAction("恢复", api::resumeVm));
        JButton delete = button("删除", () -> vmAction("删除", api::deleteVm));
        add(toolbar("虚拟机管理", refresh, create, start, shutdown, destroy, suspend, resume, delete), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    private JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        return button;
    }

    private void refresh() {
        SwingTasks.run(api::listVms, this::render, ex -> error("刷新虚拟机失败", ex));
    }

    private void render(List<VmInfoDto> list) {
        model.setRowCount(0);
        for (VmInfoDto v : list) {
            model.addRow(new Object[]{v.name, v.state, v.cpuCount, v.memoryMb + " MB", v.diskPath, v.diskSizeGb + " GB",
                    v.networkName, v.ipAddress, yes(v.autostart), yes(v.persistent), v.description});
        }
        frame.log("虚拟机列表已刷新");
    }

    private String selectedName() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择虚拟机", "提示", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return model.getValueAt(table.convertRowIndexToModel(row), 0).toString();
    }

    private void vmAction(String action, VmOperation operation) {
        String name = selectedName();
        if (name == null || !confirm("确认" + action + "虚拟机 " + name + "？")) {
            return;
        }
        SwingTasks.run(() -> {
            operation.apply(name);
            return null;
        }, ok -> {
            frame.log(action + "虚拟机 " + name + " 成功");
            refresh();
        }, ex -> error(action + "虚拟机失败", ex));
    }

    private void create() {
        SwingTasks.run(() -> new CreateOptions(api.listImages(), api.listNetworks()), this::showCreateDialog,
                ex -> error("加载创建选项失败", ex));
    }

    private void showCreateDialog(CreateOptions options) {
        JPanel form = new JPanel(new MigLayout("wrap 2", "[100!][220!]"));
        JTextField name = new JTextField();
        JSpinner cpu = new JSpinner(new SpinnerNumberModel(1, 1, 32, 1));
        JSpinner memory = new JSpinner(new SpinnerNumberModel(1024, 128, 65536, 128));
        JSpinner disk = new JSpinner(new SpinnerNumberModel(5, 1, 1024, 1));
        JComboBox<String> image = new JComboBox<>();
        JComboBox<String> network = new JComboBox<>();
        JTextField desc = new JTextField();
        for (ImageInfoDto item : options.images()) image.addItem(item.name);
        for (NetworkInfoDto item : options.networks()) network.addItem(item.name);
        form.add(new JLabel("虚拟机名称")); form.add(name, "growx");
        form.add(new JLabel("CPU 核数")); form.add(cpu);
        form.add(new JLabel("内存大小")); form.add(memory);
        form.add(new JLabel("磁盘大小")); form.add(disk);
        form.add(new JLabel("选择镜像")); form.add(image, "growx");
        form.add(new JLabel("选择网络")); form.add(network, "growx");
        form.add(new JLabel("描述")); form.add(desc, "growx");
        if (JOptionPane.showConfirmDialog(this, form, "创建虚拟机", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        CreateVmRequest req = new CreateVmRequest();
        req.name = name.getText();
        req.cpuCount = (Integer) cpu.getValue();
        req.memoryMb = (Integer) memory.getValue();
        req.diskSizeGb = (Integer) disk.getValue();
        req.imageName = (String) image.getSelectedItem();
        req.networkName = (String) network.getSelectedItem();
        req.description = desc.getText();
        SwingTasks.run(() -> api.createVm(req), vm -> {
            frame.log("创建虚拟机 " + vm.name + " 成功");
            refresh();
        }, ex -> error("创建虚拟机失败", ex));
    }

    private String yes(boolean value) {
        return value ? "是" : "否";
    }

    private interface VmOperation {
        void apply(String name);
    }

    private record CreateOptions(List<ImageInfoDto> images, List<NetworkInfoDto> networks) {
    }
}
