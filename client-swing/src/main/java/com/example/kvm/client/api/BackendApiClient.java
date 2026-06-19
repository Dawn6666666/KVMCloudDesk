package com.example.kvm.client.api;

import com.example.kvm.common.dto.*;
import com.example.kvm.common.request.*;
import com.example.kvm.common.response.ApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class BackendApiClient {
    private final String baseUrl;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public BackendApiClient(String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public HostInfoDto getHostInfo() { return get("/api/host/info", new TypeReference<>() {}); }
    public List<VmInfoDto> listVms() { return get("/api/vms", new TypeReference<>() {}); }
    public VmInfoDto getVm(String name) { return get("/api/vms/" + enc(name), new TypeReference<>() {}); }
    public VmInfoDto createVm(CreateVmRequest request) { return post("/api/vms", request, new TypeReference<>() {}); }
    public void startVm(String name) { postVoid("/api/vms/" + enc(name) + "/start"); }
    public void shutdownVm(String name) { postVoid("/api/vms/" + enc(name) + "/shutdown"); }
    public void destroyVm(String name) { postVoid("/api/vms/" + enc(name) + "/destroy"); }
    public void suspendVm(String name) { postVoid("/api/vms/" + enc(name) + "/suspend"); }
    public void resumeVm(String name) { postVoid("/api/vms/" + enc(name) + "/resume"); }
    public void deleteVm(String name) { delete("/api/vms/" + enc(name)); }
    public List<ImageInfoDto> listImages() { return get("/api/images", new TypeReference<>() {}); }
    public ImageInfoDto addImage(AddImageRequest request) { return post("/api/images", request, new TypeReference<>() {}); }
    public void deleteImage(String name) { delete("/api/images/" + enc(name)); }
    public List<NetworkInfoDto> listNetworks() { return get("/api/networks", new TypeReference<>() {}); }
    public void startNetwork(String name) { postVoid("/api/networks/" + enc(name) + "/start"); }
    public void stopNetwork(String name) { postVoid("/api/networks/" + enc(name) + "/stop"); }
    public List<SnapshotInfoDto> listSnapshots(String vmName) { return get("/api/vms/" + enc(vmName) + "/snapshots", new TypeReference<>() {}); }
    public SnapshotInfoDto createSnapshot(String vmName, CreateSnapshotRequest request) { return post("/api/vms/" + enc(vmName) + "/snapshots", request, new TypeReference<>() {}); }
    public void revertSnapshot(String vmName, String snapshotName) { postVoid("/api/vms/" + enc(vmName) + "/snapshots/" + enc(snapshotName) + "/revert"); }
    public void deleteSnapshot(String vmName, String snapshotName) { delete("/api/vms/" + enc(vmName) + "/snapshots/" + enc(snapshotName)); }
    public List<StoragePoolInfoDto> listPools() { return get("/api/storage/pools", new TypeReference<>() {}); }
    public List<StorageVolumeInfoDto> listVolumes(String poolName) { return get("/api/storage/pools/" + enc(poolName) + "/volumes", new TypeReference<>() {}); }

    private <T> T get(String path, TypeReference<ApiResponse<T>> type) {
        return send(HttpRequest.newBuilder(uri(path)).GET().timeout(Duration.ofSeconds(10)).build(), type);
    }

    private <T> T post(String path, Object body, TypeReference<ApiResponse<T>> type) {
        try {
            return send(HttpRequest.newBuilder(uri(path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(10)).build(), type);
        } catch (Exception ex) {
            throw new ApiClientException("请求发送失败：" + ex.getMessage(), ex);
        }
    }

    private void postVoid(String path) {
        post(path, new Object(), new TypeReference<ApiResponse<Void>>() {});
    }

    private void delete(String path) {
        send(HttpRequest.newBuilder(uri(path)).DELETE().timeout(Duration.ofSeconds(10)).build(),
                new TypeReference<ApiResponse<Void>>() {});
    }

    private <T> T send(HttpRequest request, TypeReference<ApiResponse<T>> type) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            ApiResponse<T> api = mapper.readValue(response.body(), type);
            if (!api.success) {
                throw new ApiClientException(api.message);
            }
            return api.data;
        } catch (ApiClientException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiClientException("无法连接后端或解析响应：" + ex.getMessage(), ex);
        }
    }

    private URI uri(String path) {
        return URI.create(baseUrl + path);
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
