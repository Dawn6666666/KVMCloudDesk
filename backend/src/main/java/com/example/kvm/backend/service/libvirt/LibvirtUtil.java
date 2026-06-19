package com.example.kvm.backend.service.libvirt;

import com.example.kvm.backend.exception.BusinessException;
import com.sun.jna.Pointer;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

final class LibvirtUtil {
    private LibvirtUtil() {
    }

    static void check(int code, String message) {
        if (code < 0) {
            throw new BusinessException(message);
        }
    }

    static String pointerString(Pointer pointer) {
        return pointer == null ? "" : pointer.getString(0, "UTF-8");
    }

    static String version(long value) {
        long major = value / 1_000_000;
        long minor = (value / 1_000) % 1_000;
        long release = value % 1_000;
        return major + "." + minor + "." + release;
    }

    static String cString(byte[] bytes) {
        int len = 0;
        while (len < bytes.length && bytes[len] != 0) {
            len++;
        }
        return new String(bytes, 0, len).trim();
    }

    static Document xml(String text) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(text)));
        } catch (Exception ex) {
            throw new BusinessException("解析 libvirt XML 失败：" + ex.getMessage());
        }
    }

    static String firstAttribute(Document doc, String tag, String attribute) {
        NodeList nodes = doc.getElementsByTagName(tag);
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element element && element.hasAttribute(attribute)) {
                return element.getAttribute(attribute);
            }
        }
        return "-";
    }
}
