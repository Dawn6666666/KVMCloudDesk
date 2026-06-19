package com.example.kvm.backend.service.libvirt;

import com.example.kvm.backend.exception.BusinessException;
import com.example.kvm.backend.service.SnapshotService;
import com.example.kvm.common.dto.SnapshotInfoDto;
import com.example.kvm.common.request.CreateSnapshotRequest;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

@Service
@Profile("libvirt")
public class LibvirtSnapshotService implements SnapshotService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private final LibvirtConnectionManager manager;

    public LibvirtSnapshotService(LibvirtConnectionManager manager) {
        this.manager = manager;
    }

    @Override
    public List<SnapshotInfoDto> listSnapshots(String vmName) {
        return withDomain(vmName, domain -> {
            LibvirtLibrary lib = manager.library();
            PointerByReference snapshotsRef = new PointerByReference();
            int count = lib.virDomainListAllSnapshots(domain, snapshotsRef, 0);
            check(count, "获取快照列表失败：" + vmName);
            List<SnapshotInfoDto> result = new ArrayList<>();
            Pointer snapshots = snapshotsRef.getValue();
            if (snapshots != null) {
                for (Pointer snapshot : snapshots.getPointerArray(0, count)) {
                    try {
                        result.add(toDto(snapshot, vmName));
                    } finally {
                        lib.virDomainSnapshotFree(snapshot);
                    }
                }
                manager.free(snapshots);
            }
            return result.stream().sorted(Comparator.comparing(s -> s.name)).toList();
        });
    }

    @Override
    public SnapshotInfoDto createSnapshot(String vmName, CreateSnapshotRequest request) {
        if (request.name == null || request.name.isBlank()) {
            throw new BusinessException("快照名称不能为空");
        }
        return withDomain(vmName, domain -> {
            String xml = """
                    <domainsnapshot>
                      <name>%s</name>
                      <description>%s</description>
                    </domainsnapshot>
                    """.formatted(escape(request.name), escape(request.description == null ? "" : request.description));
            Pointer snapshot = manager.library().virDomainSnapshotCreateXML(domain, xml, 0);
            if (snapshot == null) {
                throw new BusinessException("创建快照失败：" + request.name + "：" + manager.lastErrorMessage());
            }
            try {
                return toDto(snapshot, vmName);
            } finally {
                manager.library().virDomainSnapshotFree(snapshot);
            }
        });
    }

    @Override
    public void revertSnapshot(String vmName, String snapshotName) {
        withSnapshot(vmName, snapshotName, snapshot -> {
            check(manager.library().virDomainRevertToSnapshot(snapshot, 0), "恢复快照失败：" + snapshotName);
            return null;
        });
    }

    @Override
    public void deleteSnapshot(String vmName, String snapshotName) {
        withSnapshot(vmName, snapshotName, snapshot -> {
            check(manager.library().virDomainSnapshotDelete(snapshot, 0), "删除快照失败：" + snapshotName);
            return null;
        });
    }

    private SnapshotInfoDto toDto(Pointer snapshot, String vmName) {
        SnapshotInfoDto dto = new SnapshotInfoDto();
        dto.name = LibvirtUtil.pointerString(manager.library().virDomainSnapshotGetName(snapshot));
        dto.vmName = vmName;
        dto.createTime = "-";
        dto.state = "未知";
        dto.description = "";
        Pointer xmlPointer = manager.library().virDomainSnapshotGetXMLDesc(snapshot, 0);
        if (xmlPointer != null) {
            try {
                Document doc = LibvirtUtil.xml(xmlPointer.getString(0, "UTF-8"));
                String creationTime = LibvirtUtil.firstText(doc, "creationTime");
                if (!"-".equals(creationTime)) {
                    dto.createTime = FORMATTER.format(Instant.ofEpochSecond(Long.parseLong(creationTime)));
                }
                dto.state = LibvirtUtil.firstText(doc, "state");
                dto.description = LibvirtUtil.firstText(doc, "description");
            } finally {
                manager.free(xmlPointer);
            }
        }
        return dto;
    }

    private <T> T withSnapshot(String vmName, String snapshotName, SnapshotCallback<T> callback) {
        return withDomain(vmName, domain -> {
            Pointer snapshot = manager.library().virDomainSnapshotLookupByName(domain, snapshotName, 0);
            if (snapshot == null) {
                throw new BusinessException("快照不存在：" + snapshotName);
            }
            try {
                return callback.apply(snapshot);
            } finally {
                manager.library().virDomainSnapshotFree(snapshot);
            }
        });
    }

    private <T> T withDomain(String name, DomainCallback<T> callback) {
        LibvirtLibrary lib = manager.library();
        Pointer conn = manager.open();
        Pointer domain = lib.virDomainLookupByName(conn, name);
        if (domain == null) {
            manager.close(conn);
            throw new BusinessException("虚拟机不存在：" + name);
        }
        try {
            return callback.apply(domain);
        } finally {
            lib.virDomainFree(domain);
            manager.close(conn);
        }
    }

    private String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private interface DomainCallback<T> {
        T apply(Pointer domain);
    }

    private interface SnapshotCallback<T> {
        T apply(Pointer snapshot);
    }

    private void check(int code, String message) {
        if (code < 0) {
            throw new BusinessException(message + "：" + manager.lastErrorMessage());
        }
    }
}
