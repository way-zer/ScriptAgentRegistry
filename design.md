# ScriptAgent 中心仓库
## 项目结构
* `xxx`分支 表示`xxx`模块
  * 所有分支内直接存放kts文件，由CI负责编译
  * 分支内不包含metadata和gradle相关文件
* @开头的为特殊分支，不代表模块

### 分发方式
* Github直接kts源码分发
* 提供CI编译为ktc，上传到registry

registry以分支为单位分发。
每个分支包含一个`manifest.json`
```json5
{
  "name": "xxxModule",
  "date": "2023-11-12",
  "scripts": {
    "xxxModule.example": {
      "hash": "xxxxxxxxxxxxxxxxxxx",
      "res": {
        "example.json": {
          "hash": "xxxxxxxxxxxxxxxxx",
        } 
      }
    }
  }
}
```
### API接口
> 设计参考OCI规范: https://github.com/opencontainers/distribution-spec/blob/main/spec.md
#### 获取manifest文件
```http request
GET /manifests/<name>
Content-Type: application/json
If-None-Match: <hash> #可选
```
RETURN
* 404 Not Found
* 403/401 Forbidden
* 304 Not Modify when ETag match
* 200 OK with ETag and `json` body
#### 获取对象
```http request
GET /blobs/<hash>
Content-Type: application/octet-stream
```
RETURN
* 404 Not Found
* 200 OK with body
#### 上传对象
```http request
PUT /blobs/<hash>
Content-Type: application/octet-stream
Content-Length: <length>
```
* 400 hash check failed
* 202 Accepted with `Location` for redirect
* 201 Created
#### 更新manifest
```http request
PUT /manifests/<name>
Content-Type: application/json
ETag: <hash> #可选
```
* 412 When any blobs not found
* 304 Not Modify when ETag match
* 201 Created

### 用户使用
TODO

### 开发使用
1. 使用`git clone`到`scripts`下进行开发
2. TODO: metadata和gradle.kts生成工具

### 提交PR
1. Fork`模块分支` / 新模块Fork`@readme`
2. 添加脚本
3. 发起PR请求
