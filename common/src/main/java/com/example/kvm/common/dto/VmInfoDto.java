package com.example.kvm.common.dto;

public class VmInfoDto {
    public String name;
    public String uuid;
    public String state;
    public int cpuCount;
    public int memoryMb;
    public String diskPath;
    public int diskSizeGb;
    public String networkName;
    public String ipAddress;
    public boolean autostart;
    public boolean persistent;
    public String description;
}
