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
    Pointer virGetLastErrorMessage();

    Pointer virConnectOpen(String name);
    int virConnectClose(Pointer conn);
    Pointer virConnectGetHostname(Pointer conn);
    int virConnectGetLibVersion(Pointer conn, LongByReference libVer);
    int virConnectGetVersion(Pointer conn, LongByReference hvVer);
    int virNodeGetInfo(Pointer conn, VirNodeInfo info);
    long virNodeGetFreeMemory(Pointer conn);
    Pointer virDomainDefineXML(Pointer conn, String xmlDesc, int flags);
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

    int virConnectListAllNetworks(Pointer conn, PointerByReference networks, int flags);
    Pointer virNetworkLookupByName(Pointer conn, String name);
    Pointer virNetworkGetName(Pointer network);
    int virNetworkGetUUIDString(Pointer network, byte[] buf);
    int virNetworkIsActive(Pointer network);
    int virNetworkGetAutostart(Pointer network, IntByReference autostart);
    Pointer virNetworkGetXMLDesc(Pointer network, int flags);
    int virNetworkCreate(Pointer network);
    int virNetworkDestroy(Pointer network);
    int virNetworkFree(Pointer network);
    Pointer virNetworkDefineXML(Pointer conn, String xmlDesc);
    int virNetworkUndefine(Pointer network);
    int virNetworkSetAutostart(Pointer network, int autostart);

    int virDomainListAllSnapshots(Pointer domain, PointerByReference snapshots, int flags);
    Pointer virDomainSnapshotGetName(Pointer snapshot);
    Pointer virDomainSnapshotGetXMLDesc(Pointer snapshot, int flags);
    Pointer virDomainSnapshotCreateXML(Pointer domain, String xmlDesc, int flags);
    Pointer virDomainSnapshotLookupByName(Pointer domain, String name, int flags);
    int virDomainRevertToSnapshot(Pointer snapshot, int flags);
    int virDomainSnapshotDelete(Pointer snapshot, int flags);
    int virDomainSnapshotFree(Pointer snapshot);

    int virConnectListAllStoragePools(Pointer conn, PointerByReference pools, int flags);
    Pointer virStoragePoolLookupByName(Pointer conn, String name);
    Pointer virStoragePoolGetName(Pointer pool);
    int virStoragePoolGetUUIDString(Pointer pool, byte[] buf);
    int virStoragePoolIsActive(Pointer pool);
    int virStoragePoolGetAutostart(Pointer pool, IntByReference autostart);
    int virStoragePoolGetInfo(Pointer pool, VirStoragePoolInfo info);
    Pointer virStoragePoolGetXMLDesc(Pointer pool, int flags);
    int virStoragePoolListAllVolumes(Pointer pool, PointerByReference volumes, int flags);
    int virStoragePoolFree(Pointer pool);

    Pointer virStorageVolGetName(Pointer volume);
    Pointer virStorageVolGetPath(Pointer volume);
    int virStorageVolGetInfo(Pointer volume, VirStorageVolInfo info);
    int virStorageVolFree(Pointer volume);

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

    class VirStoragePoolInfo extends Structure {
        public int state;
        public long capacity;
        public long allocation;
        public long available;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("state", "capacity", "allocation", "available");
        }
    }

    class VirStorageVolInfo extends Structure {
        public int type;
        public long capacity;
        public long allocation;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("type", "capacity", "allocation");
        }
    }
}
