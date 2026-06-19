package com.example.kvm.client;

import com.example.kvm.client.api.BackendApiClient;
import com.example.kvm.client.config.ClientConfig;
import com.example.kvm.client.ui.MainFrame;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;

public class ClientApplication {
    public static void main(String[] args) {
        ClientConfig config = ClientConfig.load(args);
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> new MainFrame(new BackendApiClient(config.backendUrl()), config.backendUrl()).setVisible(true));
    }
}
