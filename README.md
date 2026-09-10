

```markdown
# 商城后端部署文档

## 项目简介

商城后端服务，基于 Spring Boot 3.4 + MySQL 8 + Redis + RabbitMQ 构建，使用 Docker Compose 一键部署。

---

## 目录结构

```
mall-primary-back-end/
├── compose.yaml           # 服务编排文件
├── Dockerfile             # 镜像构建文件（多阶段构建）
├── .env                   # 环境变量配置（本地，不提交）
├── .env.example           # 环境变量模板（提交 Git）
├── sql/
│   └── init.sql           # 数据库初始化脚本
├── src/                   # 源码
├── pom.xml                # Maven 依赖
└── README.md              # 本文档
```

---

## 环境要求

| 软件 | 版本要求 | 说明 |
|---|---|---|
| Docker Desktop | 20.10+ | Windows / Mac / Linux 均可 |
| 内存 | 建议 8GB 以上 | 4 个容器同时运行 |
| 磁盘 | 至少 5GB 空闲 | 镜像 + 数据卷 |

> **Windows 用户**：需要安装 WSL2（Docker Desktop 会自动提示）
> **Mac 用户（M1/M2）**：建议开启 Docker Desktop 设置中的 `Use Rosetta for x86/amd64 emulation`

---

## 快速开始

### 第 1 步：安装 Docker Desktop

- **Windows / Mac**：https://www.docker.com/products/docker-desktop
- **Linux**：`curl -fsSL https://get.docker.com | sh`

安装后启动 Docker Desktop，确保系统托盘图标为绿色运行状态。

### 第 2 步：配置镜像加速器（国内用户推荐）

打开 Docker Desktop → Settings → Docker Engine，修改 JSON 配置：

```json
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://docker.1panel.live",
    "https://hub.rat.dev"
  ]
}
```

点击 **Apply & Restart** 保存。

### 第 3 步：配置环境变量

```bash
# 复制模板（首次部署）
cp .env.example .env

# 按需修改密码等配置
```

### 第 4 步：启动所有服务

```bash
docker compose up -d
```

首次启动会自动拉取镜像（约 1GB），耐心等待 5-15 分钟。

### 第 5 步：等待服务就绪

```bash
docker compose ps
```

**4 个容器状态全部为 `Up` 或 `Up (healthy)` 即启动成功**：

```
NAME            IMAGE                           STATUS
mall-backend    baizhou2026/mall-backend:latest Up
mall-db         mysql:8.0                       Up (healthy)
mall-rabbitmq   rabbitmq:3-management-alpine    Up (healthy)
mall-redis      redis:alpine                    Up (healthy)
```

> **首次启动**：MySQL 会自动执行 `sql/init.sql` 初始化数据，大约需要 30-60 秒。期间 `mall-backend` 可能启动失败重启，属正常现象，等 `mall-db` 变为 `healthy` 后会自动恢复。

### 第 6 步：验证部署

浏览器访问：

```
http://localhost:8080/actuator/health
```

返回以下内容即部署成功：

```json
{"status":"UP"}
```

---

## 服务访问地址

| 服务 | 地址 | 凭据 |
|---|---|---|
| **后端应用** | http://localhost:8080 | 无 |
| **健康检查** | http://localhost:8080/actuator/health | 无 |
| **Swagger 接口文档** | http://localhost:8080/swagger-ui.html | 无 |
| **RabbitMQ 管理界面** | http://localhost:15672 | guest / guest |
| **MySQL** | localhost:3307 | mall / root |
| **Redis** | localhost:6379 | 无密码 |

> **注意**：MySQL 映射到宿主机的 **3307** 端口（不是默认的 3306），避免与本地已有 MySQL 冲突。

---

## 常用命令

```bash
# 查看所有容器状态
docker compose ps

# 查看所有服务日志
docker compose logs -f

# 查看某个服务日志
docker compose logs -f backend
docker compose logs -f db
docker compose logs -f redis
docker compose logs -f rabbitmq

# 停止所有服务（保留数据）
docker compose stop

# 启动所有服务
docker compose start

# 重启所有服务
docker compose restart

# 重启某个服务
docker compose restart backend

# 完全停止并删除容器、网络（数据保留）
docker compose down

# 完全停止并删除容器、网络、数据卷（⚠️ 数据清空）
docker compose down -v

# 拉取最新镜像
docker compose pull

# 更新后端到最新版本
docker compose pull backend
docker compose up -d backend
```

---

## 数据库操作

### 进入数据库命令行

```bash
docker exec -it mall-db mysql -umall -proot mall
```

### 查看所有表

```bash
docker exec -it mall-db mysql -umall -proot mall -e "SHOW TABLES;"
```

### 备份数据库

```bash
docker exec mall-db mysqldump -umall -proot mall > backup.sql
```

### 恢复数据库

```bash
docker exec -i mall-db mysql -umall -proot mall < backup.sql
```

### 使用外部客户端连接

| 参数 | 值 |
|---|---|
| 主机 | localhost |
| 端口 | **3307** |
| 数据库 | mall |
| 用户名 | mall |
| 密码 | root |

推荐工具：Navicat、DBeaver、DataGrip、MySQL Workbench

---

## Redis 操作

```bash
# 进入 Redis 命令行
docker exec -it mall-redis redis-cli

# 查看所有 key
docker exec -it mall-redis redis-cli KEYS "*"

# 查看 Redis 信息
docker exec -it mall-redis redis-cli INFO
```

---

## RabbitMQ 操作

### 管理界面

浏览器访问 http://localhost:15672

- 用户名：`guest`
- 密码：`guest`

### 命令行

```bash
# 查看队列
docker exec -it mall-rabbitmq rabbitmqctl list_queues

# 查看交换机
docker exec -it mall-rabbitmq rabbitmqctl list_exchanges

# 查看连接
docker exec -it mall-rabbitmq rabbitmqctl list_connections
```

---

## 自行构建镜像

如果你修改了源码，需要重新构建并推送镜像：

```bash
# 多阶段构建（Maven 打包 + 镜像制作，一步完成）
docker build -t baizhou2026/mall-backend:latest .

# 推送到 Docker Hub
docker push baizhou2026/mall-backend:latest
```

> Dockerfile 采用多阶段构建：第一阶段用 Maven 镜像打包，第二阶段只复制 JAR 到 JRE Alpine 精简镜像，最终镜像仅约 200MB。

---

## 更新后端版本

当后端有新版本发布时：

```bash
# 1. 拉取最新镜像
docker compose pull backend

# 2. 重启后端服务
docker compose up -d backend

# 3. 查看日志确认启动成功
docker compose logs -f backend
```

看到以下日志说明更新成功：

```
Started MallPrimaryBackEndApplication in XX.XXX seconds
```

---

## 故障排查

### 1. 后端启动失败

```bash
docker compose logs backend
```

**常见错误关键词及原因**：

| 错误 | 原因 | 解决 |
|---|---|---|
| `Access denied for user` | 数据库密码不对 | 检查 `.env` 里的 `DB_PASSWORD` |
| `Unknown database 'mall'` | 数据库未初始化 | 检查 `sql/init.sql` 是否存在，尝试 `docker compose down -v` 重新初始化 |
| `Connection refused` | 依赖服务未就绪 | 等待 MySQL 变为 `healthy` 后重启 backend |
| `Port 8080 already in use` | 端口被占用 | 修改 `compose.yaml` 里 backend 的端口映射 |

### 2. MySQL 启动失败

```bash
docker compose logs db
```

**常见错误**：

- `MYSQL_USER="root" ... cannot be used for the root user` → `.env` 里 `DB_USER` 不能是 `root`，改成 `mall`
- `Port 3307 already in use` → 修改 `compose.yaml` 里 db 的端口映射

### 3. 端口被占用

**Windows**：
```cmd
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

**Mac / Linux**：
```bash
lsof -i :8080
kill -9 <PID>
```

### 4. 磁盘空间不足

```bash
# 查看 Docker 磁盘占用
docker system df

# 清理未使用的资源
docker system prune -a

# 清理未使用的数据卷（⚠️ 会删除数据）
docker volume prune
```

### 5. 完全重置（清空所有数据）

```bash
docker compose down -v
docker compose up -d
```

> ⚠️ 这会删除所有数据库数据，请先备份。

---

## 配置文件说明

### compose.yaml

定义 4 个服务：`db`、`redis`、`rabbitmq`、`backend`，通过内部网络 `app-network` 互联。

### .env / .env.example

| 变量 | 说明 | 默认值 |
|---|---|---|
| `DB_ROOT_PASSWORD` | MySQL root 密码 | root |
| `DB_DATABASE` | 数据库名 | mall |
| `DB_USER` | 应用数据库用户 | mall |
| `DB_PASSWORD` | 应用数据库密码 | root |
| `RABBITMQ_USER` | RabbitMQ 用户 | guest |
| `RABBITMQ_PASSWORD` | RabbitMQ 密码 | guest |
| `RABBITMQ_VHOST` | RabbitMQ 虚拟主机 | / |

> `.env.example` 提交到 Git 作为模板，`.env` 在 `.gitignore` 中忽略。

### sql/init.sql

数据库初始化脚本，包含所有表结构和初始数据。

**注意**：`init.sql` 只在**首次启动**（数据卷为空时）执行。如需重新初始化：

```bash
docker compose down -v
docker compose up -d
```

---

## 生产环境建议

1. **修改默认密码**：编辑 `.env`，设置强密码
2. **关闭端口对外暴露**：删除 `db`、`redis`、`rabbitmq` 的 `ports` 映射，只保留内部访问
3. **使用外部数据库**：将 `db` 换成云数据库（阿里云 RDS、腾讯云等）
4. **配置 HTTPS**：在前面加 Nginx 反向代理
5. **设置资源限制**：

```yaml
  backend:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
```

6. **配置日志轮转**：

```yaml
  backend:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

---

## 技术栈

| 组件 | 版本 | 用途 |
|---|---|---|
| Spring Boot | 3.4.0 | 应用框架 |
| Java | 21 | 运行环境 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 7 (alpine) | 缓存 / 分布式锁 |
| RabbitMQ | 3.x (management-alpine) | 消息队列 |
| MyBatis-Plus | 3.5.7 | ORM 框架 |
| Spring Security | 6.x | 安全框架 |
| Docker | 20.10+ | 容器化 |

---

## 联系方式

- **镜像地址**：https://hub.docker.com/r/baizhou2026/mall-backend
- **拉取命令**：`docker pull baizhou2026/mall-backend:latest`

---

## 许可

仅供学习交流使用。

---

**祝部署顺利！** 遇到问题先看日志：

```bash
docker compose logs -f
```

90% 的问题日志里都能找到答案。
```

---

### 优化点总结

| 改动 | 说明 |
|------|------|
| 目录结构 | 从 `mall-deploy/` 改为实际 `mall-primary-back-end/` |
| 文件名 | `docker-compose.yml` → `compose.yaml` |
| 命令 | `docker-compose` → `docker compose`（全局替换） |
| 新增 `.env.example` | 添加模板说明，第 3 步引导用户复制 |
| 新增 Dockerfile | 目录结构 + "自行构建镜像" 章节 |
| 新增 Swagger 地址 | `http://localhost:8080/swagger-ui.html` |
| 技术栈精简 | 去掉 Redisson 等冗余项，保持核心 |

你可以直接复制上面的内容覆盖 `README.md`。