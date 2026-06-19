package com.example.kvm.backend.service.libvirt;

import com.sun.jna.Library;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import java.util.List;

public interface LibvirtLibrary extends Library {
    Pointer virConnectOpen(String name);
    int virConnectClose(Pointer conn);
    Pointer virConnectGetHostname(Pointer conn);
    int virConnectGetLibVersion(Pointer conn, LongByReference libVer);
    int virConnectGetVersion(Pointer conn, LongByReference hvVer);
    int virNodeGetInfo(Pointer conn, VirNodeInfo info);
    long virNodeGetFreeMemory(Pointer conn);
    int virConnectListAllDomains(Pointer conn, PointerByReference domains, int flags);

    Pointer virDomainLookupByName(Pointer conn, String name);
    Pointer virDomainGetName(Pointer domain);
    int virDomainGetUUIDString(Pointer domain, byte[] buf);
    int virDomainGetInfo(Pointer domain, VirDomainInfo info);
    int virDomainGetState(Pointer domain, IntByReference state, IntByReference reason, int flags);
    Pointer virDomainGetXMLDesc(Pointer domain, int flags);
    int virDomainGetAutostart(Pointer domain, IntByReference autostart);
    int virDomainIsPersistent(Pointer domain);
    int virDomainCreate(Pointer domain);
    int virDomainShutdown(Pointer domain);
    int virDomainDestroy(Pointer domain);
    int virDomainSuspend(Pointer domain);
    int virDomainResume(Pointer domain);
    int virDomainUndefine(Pointer domain);
    int virDomainFree(Pointer domain);

    class VirNodeInfo extends Structure {
        public byte[] model = new byte[32];
        public NativeLong memory;
        public int cpus;
        public int mhz;
        public int nodes;
        public int sockets;
        public int cores;
        public int threads;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("model", "memory", "cpus", "mhz", "nodes", "sockets", "cores", "threads");
        }
    }

    class VirDomainInfo extends Structure {
        public byte state;
        public NativeLong maxMem;
        public NativeLong memory;
        public short nrVirtCpu;
        public long cpuTime;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("state", "maxMem", "memory", "nrVirtCpu", "cpuTime");
        }
    }
}
