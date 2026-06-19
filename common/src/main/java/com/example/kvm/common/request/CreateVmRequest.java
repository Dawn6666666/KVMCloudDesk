package com.example.kvm.common.request;

public class CreateVmRequest {
    public String name;
    public int cpuCount;
    public int memoryMb;
    public int diskSizeGb;
    public String imageName;
    public String networkName;
    public String description;
}
