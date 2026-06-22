package com.example.kvm.common.dto;

public class HostInfoDto {
    public String hostname;
    public String cpuModel;
    public int cpuCount;
    public int cpuMHz;
    public long totalMemoryMb;
    public long usedMemoryMb;
    public long freeMemoryMb;
    public int memoryUsagePercent;
    public String virtualizationType;
    public String libvirtVersion;
    public String qemuVersion;
    public boolean kvmEnabled;
    public String connectionUri;
    public double cpuUsagePercent;
    public double systemLoadAverage;
    public int cpuSockets;
    public int cpuCores;
    public int cpuThreads;
    public int numaNodes;
    public String osName;
    public String osKernel;
    public String uptime;
    public long networkRxBytes;
    public long networkTxBytes;
}
