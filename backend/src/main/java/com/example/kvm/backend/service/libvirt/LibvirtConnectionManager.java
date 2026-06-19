package com.example.kvm.backend.service.libvirt;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.example.kvm.backend.exception.BusinessException;
import com.sun.jna.Library;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("libvirt")
public class LibvirtConnectionManager {
    interface LibcLibrary extends Library {
        void free(Pointer ptr);
    }

    private final String uri;
    private final LibvirtLibrary library;
    private final LibcLibrary libc;

    public LibvirtConnectionManager(@Value("${kvm.libvirt.uri}") String uri,
                                    @Value("${kvm.libvirt.library}") String libraryPath) {
        this.uri = uri;
        this.library = Native.load(libraryPath, LibvirtLibrary.class);
        this.libc = Native.load("c", LibcLibrary.class);
    }

    public String uri() {
        return uri;
    }

    public LibvirtLibrary library() {
        return library;
    }

    public Pointer open() {
        Pointer conn = library.virConnectOpen(uri);
        if (conn == null) {
            throw new BusinessException("连接 libvirt 失败：" + uri);
        }
        return conn;
    }

    public void close(Pointer conn) {
        if (conn != null) {
            library.virConnectClose(conn);
        }
    }

    public void free(Pointer pointer) {
        if (pointer != null) {
            libc.free(pointer);
        }
    }
}
