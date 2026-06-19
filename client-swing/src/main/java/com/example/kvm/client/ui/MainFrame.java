package com.example.kvm.client.ui;

import com.example.kvm.client.api.BackendApiClient;
import com.example.kvm.client.util.SwingTasks;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import net.miginfocom.swing.MigLayout;

public class MainFrame extends JFrame {
    private final BackendApiClient api;
    private final String backendUrl;
    private final JPanel pages = new JPanel(new CardLayout());
    private final JTextArea logArea = new JTextArea(5, 80);
    private final JLabel statusLabel = new JLabel("连接中");
    private boolean dark;

    public MainFrame(BackendApiClient api, String backendUrl) {
        super("KVM 云平台管理系统");
        this.api = api;
        this.backendUrl = backendUrl;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1180, 760);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(topBar(), BorderLayout.NORTH);
        add(content(), BorderLayout.CENTER);
        add(logPanel(), BorderLayout.SOUTH);
        checkBackend();
        log("系统启动");
    }

    public void log(String message) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logArea.append("[" + time + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private JPanel topBar() {
        JPanel panel = new JPanel(new MigLayout("insets 10", "[grow][]10[]10[]", "[]"));
        JLabel title = new JLabel("KVM 云平台管理系统");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JButton refresh = new JButton("刷新");
        JButton theme = new JButton("主题");
        refresh.addActionListener(e -> checkBackend());
        theme.addActionListener(e -> switchTheme());
        panel.add(title, "growx");
        panel.add(new JLabel("后端: " + backendUrl));
        panel.add(statusLabel);
        panel.add(refresh);
        panel.add(theme);
        return panel;
    }

    private JSplitPane content() {
        JPanel nav = new JPanel(new MigLayout("wrap 1, insets 10", "[160!]", "[]8[]8[]8[]8[]8[]"));
        addPage(nav, "宿主机概览", new HostPanel(api, this));
        addPage(nav, "虚拟机管理", new VmPanel(api, this));
        addPage(nav, "镜像管理", new ImagePanel(api, this));
        addPage(nav, "网络管理", new NetworkPanel(api, this));
        addPage(nav, "快照管理", new SnapshotPanel(api, this));
        addPage(nav, "存储管理", new StoragePanel(api, this));
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, nav, pages);
        splitPane.setDividerLocation(190);
        return splitPane;
    }

    private void addPage(JPanel nav, String name, JPanel panel) {
        JButton button = new JButton(name);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.addActionListener(e -> ((CardLayout) pages.getLayout()).show(pages, name));
        nav.add(button, "growx");
        pages.add(panel, name);
    }

    private JScrollPane logPanel() {
        logArea.setEditable(false);
        return new JScrollPane(logArea);
    }

    private void checkBackend() {
        statusLabel.setText("连接中");
        SwingTasks.run(api::getHostInfo, host -> {
            statusLabel.setText("已连接");
            log("已连接后端：" + backendUrl);
        }, ex -> {
            statusLabel.setText("连接失败");
            log("无法连接后端：" + ex.getMessage());
        });
    }

    private void switchTheme() {
        dark = !dark;
        if (dark) {
            FlatDarculaLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        SwingUtilities.updateComponentTreeUI(this);
    }
}
