package com.example.kvm.backend.service.libvirt;

import com.example.kvm.backend.exception.BusinessException;
import com.example.kvm.backend.service.NetworkService;
import com.example.kvm.common.dto.NetworkInfoDto;
import com.example.kvm.common.request.CreateNetworkRequest;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

@Service
@Profile("libvirt")
public class LibvirtNetworkService implements NetworkService {
    private final LibvirtConnectionManager manager;

    public LibvirtNetworkService(LibvirtConnectionManager manager) {
        this.manager = manager;
    }

    @Override
    public List<NetworkInfoDto> listNetworks() {
        LibvirtLibrary lib = manager.library();
        Pointer conn = manager.open();
        PointerByReference networksRef = new PointerByReference();
        try {
            int count = lib.virConnectListAllNetworks(conn, networksRef, 0);
            check(count, "获取网络列表失败");
            List<NetworkInfoDto> result = new ArrayList<>();
            Pointer networks = networksRef.getValue();
            if (networks != null) {
                for (Pointer network : networks.getPointerArray(0, count)) {
                    try {
                        result.add(toDto(network));
                    } finally {
                        lib.virNetworkFree(network);
                    }
                }
                manager.free(networks);
            }
            return result.stream().sorted(Comparator.comparing(n -> n.name)).toList();
        } finally {
            manager.close(conn);
        }
    }

    @Override
    public void startNetwork(String name) {
        withNetwork(name, network -> {
            check(manager.library().virNetworkCreate(network), "启动网络失败：" + name);
            return null;
        });
    }

    @Override
    public void stopNetwork(String name) {
        withNetwork(name, network -> {
            check(manager.library().virNetworkDestroy(network), "停止网络失败：" + name);
            return null;
        });
    }

    private NetworkInfoDto toDto(Pointer network) {
        LibvirtLibrary lib = manager.library();
        NetworkInfoDto dto = new NetworkInfoDto();
        dto.name = LibvirtUtil.pointerString(lib.virNetworkGetName(network));
        byte[] uuid = new byte[37];
        if (lib.virNetworkGetUUIDString(network, uuid) == 0) {
            dto.uuid = LibvirtUtil.cString(uuid);
        }
        dto.active = lib.virNetworkIsActive(network) == 1;
        IntByReference autostart = new IntByReference();
        dto.autostart = lib.virNetworkGetAutostart(network, autostart) == 0 && autostart.getValue() == 1;
        fillXml(network, dto);
        return dto;
    }

    private void fillXml(Pointer network, NetworkInfoDto dto) {
        Pointer xmlPointer = manager.library().virNetworkGetXMLDesc(network, 0);
        if (xmlPointer == null) {
            return;
        }
        try {
            Document doc = LibvirtUtil.xml(xmlPointer.getString(0, "UTF-8"));
            dto.bridgeName = LibvirtUtil.firstAttribute(doc, "bridge", "name");
            dto.forwardMode = LibvirtUtil.firstAttribute(doc, "forward", "mode");
            dto.ipAddress = LibvirtUtil.firstAttribute(doc, "ip", "address");
            dto.netmask = LibvirtUtil.firstAttribute(doc, "ip", "netmask");
            if (dto.netmask == null || "-".equals(dto.netmask)) {
                String prefix = LibvirtUtil.firstAttribute(doc, "ip", "prefix");
                if (prefix != null && !"-".equals(prefix)) {
                    dto.netmask = "/" + prefix;
                }
            }
            dto.dhcpStart = LibvirtUtil.firstAttribute(doc, "range", "start");
            dto.dhcpEnd = LibvirtUtil.firstAttribute(doc, "range", "end");
        } finally {
            manager.free(xmlPointer);
        }
    }

    private <T> T withNetwork(String name, NetworkCallback<T> callback) {
        LibvirtLibrary lib = manager.library();
        Pointer conn = manager.open();
        Pointer network = lib.virNetworkLookupByName(conn, name);
        if (network == null) {
            manager.close(conn);
            throw new BusinessException("网络不存在：" + name);
        }
        try {
            return callback.apply(network);
        } finally {
            lib.virNetworkFree(network);
            manager.close(conn);
        }
    }

    private interface NetworkCallback<T> {
        T apply(Pointer network);
    }

    private void check(int code, String message) {
        if (code < 0) {
            throw new BusinessException(message + "：" + manager.lastErrorMessage());
        }
    }

    @Override
    public void createNetwork(CreateNetworkRequest request) {
        if (request.name == null || request.name.isBlank()) {
            throw new BusinessException("网络名称不能为空");
        }

        LibvirtLibrary lib = manager.library();
        Pointer conn = manager.open();
        try {
            Pointer existing = lib.virNetworkLookupByName(conn, request.name);
            if (existing != null) {
                lib.virNetworkFree(existing);
                throw new BusinessException("局域网网络 " + request.name + " 已经存在");
            }
        } finally {
            manager.close(conn);
        }

        String bridgeName = "br-" + (request.name.length() > 10 ? request.name.substring(0, 10) : request.name);
        StringBuilder xml = new StringBuilder();
        xml.append("<network>\n");
        xml.append("  <name>").append(request.name).append("</name>\n");
        if (request.forwardMode != null && !request.forwardMode.isBlank() && !request.forwardMode.equalsIgnoreCase("none")) {
            xml.append("  <forward mode='").append(request.forwardMode.toLowerCase()).append("'/>\n");
        }
        xml.append("  <bridge name='").append(bridgeName).append("' stp='on' delay='0'/>\n");

        if (request.ipAddress != null && !request.ipAddress.isBlank()) {
            xml.append("  <ip address='").append(request.ipAddress).append("'");
            if (request.netmask != null && !request.netmask.isBlank()) {
                xml.append(" netmask='").append(request.netmask).append("'");
            }
            xml.append(">\n");
            if (request.dhcpEnabled && request.dhcpStart != null && !request.dhcpStart.isBlank() && request.dhcpEnd != null && !request.dhcpEnd.isBlank()) {
                xml.append("    <dhcp>\n");
                xml.append("      <range start='").append(request.dhcpStart).append("' end='").append(request.dhcpEnd).append("'/>\n");
                xml.append("    </dhcp>\n");
            }
            xml.append("  </ip>\n");
        }
        xml.append("</network>\n");

        Pointer conn2 = manager.open();
        Pointer network = null;
        try {
            network = lib.virNetworkDefineXML(conn2, xml.toString());
            if (network == null) {
                throw new BusinessException("定义局域网网络失败：" + manager.lastErrorMessage());
            }
            lib.virNetworkSetAutostart(network, 1);
            check(lib.virNetworkCreate(network), "启动局域网网络失败");
        } finally {
            if (network != null) {
                lib.virNetworkFree(network);
            }
            manager.close(conn2);
        }
    }

    @Override
    public void deleteNetwork(String name) {
        withNetwork(name, network -> {
            LibvirtLibrary lib = manager.library();
            if (lib.virNetworkIsActive(network) == 1) {
                lib.virNetworkDestroy(network);
            }
            check(lib.virNetworkUndefine(network), "注销虚拟网络失败：" + name);
            return null;
        });
    }
}
