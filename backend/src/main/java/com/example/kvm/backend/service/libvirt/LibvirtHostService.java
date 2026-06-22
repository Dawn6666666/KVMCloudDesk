package com.example.kvm.backend.service.libvirt;

import com.example.kvm.backend.service.HostService;
import com.example.kvm.common.dto.HostInfoDto;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.LongByReference;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("libvirt")
public class LibvirtHostService implements HostService {
    private final LibvirtConnectionManager manager;

    public LibvirtHostService(LibvirtConnectionManager manager) {
        this.manager = manager;
    }

    @Override
    public HostInfoDto getHostInfo() {
        LibvirtLibrary lib = manager.library();
        Pointer conn = manager.open();
        try {
            HostInfoDto dto = new HostInfoDto();
            dto.hostname = getLocalHostname();

            LibvirtLibrary.VirNodeInfo node = new LibvirtLibrary.VirNodeInfo();
            check(lib.virNodeGetInfo(conn, node), "获取宿主机信息失败");
            dto.cpuModel = LibvirtUtil.cString(node.model);
            dto.cpuCount = node.cpus;
            dto.cpuMHz = node.mhz;
            dto.cpuSockets = node.sockets;
            dto.cpuCores = node.cores;
            dto.cpuThreads = node.threads;
            dto.numaNodes = node.nodes;

            dto.osName = System.getProperty("os.name");
            dto.osKernel = System.getProperty("os.version");
            dto.uptime = formatUptime(getHostUptime());

            dto.totalMemoryMb = node.memory.longValue() / 1024;
            long freeMemoryMb = getFreeMemoryFromProc();
            if (freeMemoryMb <= 0) {
                long freeMemoryBytes = lib.virNodeGetFreeMemory(conn);
                freeMemoryMb = freeMemoryBytes > 0 ? freeMemoryBytes / 1024 / 1024 : 0;
            }
            dto.freeMemoryMb = freeMemoryMb;
            dto.usedMemoryMb = Math.max(0, dto.totalMemoryMb - dto.freeMemoryMb);
            dto.memoryUsagePercent = dto.totalMemoryMb == 0 ? 0 : (int) (dto.usedMemoryMb * 100 / dto.totalMemoryMb);

            java.lang.management.OperatingSystemMXBean osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            double systemLoad = osBean.getSystemLoadAverage();
            dto.systemLoadAverage = systemLoad < 0 ? 0.0 : Math.round(systemLoad * 100.0) / 100.0;

            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                double cpuLoad = ((com.sun.management.OperatingSystemMXBean) osBean).getCpuLoad();
                if (cpuLoad < 0) {
                    cpuLoad = ((com.sun.management.OperatingSystemMXBean) osBean).getSystemCpuLoad();
                }
                dto.cpuUsagePercent = cpuLoad < 0 ? 0.0 : Math.round(cpuLoad * 1000.0) / 10.0;
            } else {
                dto.cpuUsagePercent = 0.0;
            }

            dto.virtualizationType = "KVM";
            dto.kvmEnabled = true;
            dto.connectionUri = manager.uri();

            LongByReference libVersion = new LongByReference();
            if (lib.virConnectGetLibVersion(conn, libVersion) == 0) {
                dto.libvirtVersion = LibvirtUtil.version(libVersion.getValue());
            }
            LongByReference qemuVersion = new LongByReference();
            if (lib.virConnectGetVersion(conn, qemuVersion) == 0) {
                dto.qemuVersion = LibvirtUtil.version(qemuVersion.getValue());
            } else {
                dto.qemuVersion = "未知";
            }
            fillNetworkBytes(dto);
            return dto;
        } finally {
            manager.close(conn);
        }
    }

    private void fillNetworkBytes(HostInfoDto dto) {
        long rx = 0;
        long tx = 0;
        try {
            java.io.File file = new java.io.File("/proc/net/dev");
            if (file.exists() && file.canRead()) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                    String line;
                    reader.readLine();
                    reader.readLine();
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        String[] parts = line.split(":");
                        if (parts.length >= 2) {
                            String iface = parts[0].trim();
                            if ("lo".equals(iface)) {
                                continue;
                            }
                            String[] stats = parts[1].trim().split("\\s+");
                            if (stats.length >= 9) {
                                rx += Long.parseLong(stats[0]);
                                tx += Long.parseLong(stats[8]);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        dto.networkRxBytes = rx;
        dto.networkTxBytes = tx;
    }

    private long getFreeMemoryFromProc() {
        try {
            java.io.File file = new java.io.File("/proc/meminfo");
            if (file.exists() && file.canRead()) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                    String line;
                    long memAvailableKb = -1;
                    long memFreeKb = -1;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("MemAvailable:")) {
                            memAvailableKb = parseKbValue(line);
                            break;
                        } else if (line.startsWith("MemFree:")) {
                            memFreeKb = parseKbValue(line);
                        }
                    }
                    long finalKb = memAvailableKb != -1 ? memAvailableKb : memFreeKb;
                    if (finalKb > 0) {
                        return finalKb / 1024;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore exception and fall back to native JNA call
        }
        return -1;
    }

    private long parseKbValue(String line) {
        try {
            String[] parts = line.split("\\s+");
            if (parts.length >= 2) {
                return Long.parseLong(parts[1]);
            }
        } catch (Exception e) {
            // Ignore
        }
        return -1;
    }

    private String getLocalHostname() {
        String envHost = System.getenv("HOSTNAME");
        if (envHost != null && !envHost.isBlank()) {
            return envHost.trim();
        }
        try {
            java.io.File file = new java.io.File("/proc/sys/kernel/hostname");
            if (file.exists() && file.canRead()) {
                try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(file))) {
                    String line = r.readLine();
                    if (line != null && !line.isBlank()) {
                        return line.trim();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        try {
            java.io.File file = new java.io.File("/etc/hostname");
            if (file.exists() && file.canRead()) {
                try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(file))) {
                    String line = r.readLine();
                    if (line != null && !line.isBlank()) {
                        return line.trim();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            // Ignore
        }
        return "centos";
    }

    private void check(int code, String message) {
        if (code < 0) {
            throw new com.example.kvm.backend.exception.BusinessException(message + "：" + manager.lastErrorMessage());
        }
    }

    private long getHostUptime() {
        try {
            java.io.File file = new java.io.File("/proc/uptime");
            if (file.exists() && file.canRead()) {
                try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(file))) {
                    String line = r.readLine();
                    if (line != null && !line.isBlank()) {
                        String[] parts = line.split("\\s+");
                        if (parts.length > 0) {
                            double seconds = Double.parseDouble(parts[0]);
                            return (long) seconds;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return -1;
    }

    private String formatUptime(long seconds) {
        if (seconds < 0) {
            return "未知";
        }
        long days = seconds / (24 * 3600);
        long hours = (seconds % (24 * 3600)) / 3600;
        long minutes = (seconds % 3600) / 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("天 ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append("小时 ");
        }
        sb.append(minutes).append("分钟");
        return sb.toString();
    }
}
