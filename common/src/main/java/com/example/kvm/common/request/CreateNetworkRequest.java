package com.example.kvm.common.request;

public class CreateNetworkRequest {
    public String name;
    public String forwardMode;
    public String ipAddress;
    public String netmask;
    public boolean dhcpEnabled;
    public String dhcpStart;
    public String dhcpEnd;
}
