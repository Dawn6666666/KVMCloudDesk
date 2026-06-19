package com.example.kvm.client.config;

import java.io.InputStream;
import java.util.Properties;

public class ClientConfig {
    private final String backendUrl;

    private ClientConfig(String backendUrl) {
        this.backendUrl = backendUrl;
    }

    public static ClientConfig load(String[] args) {
        String url = "http://127.0.0.1:8080";
        Properties properties = new Properties();
        try (InputStream in = ClientConfig.class.getClassLoader().getResourceAsStream("client.properties")) {
            if (in != null) {
                properties.load(in);
                url = properties.getProperty("backend.url", url);
            }
        } catch (Exception ignored) {
            // Defaults keep the client usable during first-run setup.
        }
        for (String arg : args) {
            if (arg.startsWith("--backend.url=")) {
                url = arg.substring("--backend.url=".length());
            }
        }
        return new ClientConfig(url);
    }

    public String backendUrl() {
        return backendUrl;
    }
}
