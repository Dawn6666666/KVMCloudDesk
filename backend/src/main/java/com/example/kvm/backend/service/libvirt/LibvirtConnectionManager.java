package com.example.kvm.backend.service.libvirt;

import com.sun.jna.Library;
import com.sun.jna.Native;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("libvirt")
public class LibvirtConnectionManager {
    public interface LibvirtLibrary extends Library {
    }

    private final String uri;
    private final LibvirtLibrary library;

    public LibvirtConnectionManager(@Value("${kvm.libvirt.uri}") String uri,
                                    @Value("${kvm.libvirt.library}") String libraryPath) {
        this.uri = uri;
        this.library = Native.load(libraryPath, LibvirtLibrary.class);
    }

    public String uri() {
        return uri;
    }

    public LibvirtLibrary library() {
        return library;
    }
}
