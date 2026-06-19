# REST API 列表

所有接口返回 `ApiResponse<T>`：

```json
{
  "success": true,
  "message": "操作成功",
  "data": {}
}
```

接口前缀为 `/api`：

- `GET /host/info`
- `GET /vms`
- `GET /vms/{name}`
- `POST /vms`
- `POST /vms/{name}/start`
- `POST /vms/{name}/shutdown`
- `POST /vms/{name}/destroy`
- `POST /vms/{name}/suspend`
- `POST /vms/{name}/resume`
- `DELETE /vms/{name}`
- `GET /images`
- `POST /images`
- `DELETE /images/{name}`
- `GET /networks`
- `POST /networks/{name}/start`
- `POST /networks/{name}/stop`
- `GET /vms/{vmName}/snapshots`
- `POST /vms/{vmName}/snapshots`
- `POST /vms/{vmName}/snapshots/{snapshotName}/revert`
- `DELETE /vms/{vmName}/snapshots/{snapshotName}`
- `GET /storage/pools`
- `GET /storage/pools/{poolName}/volumes`
