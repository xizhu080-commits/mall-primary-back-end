# 🛒 Mall Primary Backend

> 基于 Spring Boot 3 + MyBatis-Plus 构建的电商后端系统，涵盖用户认证、商品管理、订单交易、库存控制、优惠券、支付、消息队列等核心业务，并支持 Docker Compose 一键部署与完整的可观测性（Prometheus + Grafana + ELK）。

## 📖 项目简介

Mall Primary Backend 是一个基于 **Spring Boot 3 + MyBatis-Plus** 开发的电商后端项目，采用前后端分离架构。

项目围绕完整的电商交易流程进行设计，主要包含：

- 用户注册与登录
- JWT 身份认证
- 商品与 SKU 管理
- 商品搜索与详情
- 订单创建与管理
- Redis 库存控制
- 优惠券计算
- 支付宝沙箱支付
- 退款处理
- Redis 缓存
- RabbitMQ 异步消息
- WebSocket 实时通知
- Docker Compose 容器化部署
- Prometheus + Grafana 指标监控
- ELK 日志集中化

---

## ✨ 核心功能

### 👤 用户与认证

- 用户注册 / 登录
- JWT 身份认证
- Spring Security 权限控制
- Token 鉴权
- BCrypt 密码加密

### 🛍️ 商品模块

- 商品分类
- SPU / SKU 管理
- 商品上下架
- 商品搜索
- 商品详情
- SKU 多规格展示
- 商品库存管理

### 📦 订单模块

- 创建订单
- 多商品订单
- 订单预览
- 订单金额计算
- 优惠券计算
- 订单状态管理
- 订单详情查询
- 订单取消
- 退款处理

### 🎟️ 优惠券模块

支持：

- 满减优惠
- 折扣优惠
- 使用门槛
- 有效期控制
- 优惠券叠加规则
- 订单优惠金额计算
- 优惠券状态管理

### 💰 支付模块

集成支付宝沙箱支付：

- 创建支付订单
- 生成支付二维码
- 支付状态查询
- 支付异步通知
- RSA2 签名验证
- 退款处理

### 📦 库存模块

- Redis 库存缓存
- Redis Lua 原子扣减
- 库存不足判断
- 库存回滚
- Redisson 分布式锁
- 高并发场景下库存控制

### 📨 消息队列

基于 RabbitMQ 实现异步业务处理：

- 消息生产与消费
- Topic Exchange
- 消费者并发处理
- 消息确认
- 消息重试
- 异步业务解耦

### 🔔 WebSocket

基于 WebSocket 实现实时消息通知：

- 用户支付结果通知
- 商家支付结果通知
- 实时业务消息推送

### 📊 可观测性

**监控（Prometheus + Grafana）**：

- 应用指标暴露（`/actuator/prometheus`）
- JVM 内存、CPU、线程、HTTP 请求
- Prometheus 定时抓取
- Grafana 仪表盘可视化（Dashboard `11378`）
- 系统级监控（`node-exporter`）

**日志（ELK）**：

- Spring Boot 输出 JSON 日志
- Logstash 收集并转发
- Elasticsearch 存储与检索
- Kibana 集中查询与分析

---

## 🧰 技术栈

| 技术 | 版本 | 用途 |
|---|---|---|
| Java | 21 | 后端开发 |
| Spring Boot | 3.4.0 | 核心开发框架 |
| Spring Security | 6.x | 身份认证与授权 |
| JWT | 0.12.6 | Token 身份认证 |
| MyBatis-Plus | 3.5.7 | ORM / 数据访问 |
| MySQL | 8.x | 关系型数据库 |
| Redis | 7.x | 缓存、库存、分布式锁 |
| RabbitMQ | 3.x | 异步消息 |
| Redisson | 3.x | 分布式锁 |
| WebSocket | - | 实时消息推送 |
| SpringDoc | 2.x | API 文档 |
| MapStruct | 1.5.3 | DTO / VO 转换 |
| Hutool | 5.8.x | Java 工具类 |
| Alipay SDK | - | 支付宝支付 |
| Docker Compose | - | 容器化部署 |
| Prometheus | latest | 指标采集 |
| Grafana | latest | 指标可视化 |
| Elasticsearch | 8.13.0 | 日志存储与检索 |
| Logstash | 8.13.0 | 日志收集 |
| Kibana | 8.13.0 | 日志查询 |
| node-exporter | latest | 系统级指标 |

---

## 🏗️ 项目结构

```text
mall-primary-back-end/
├── .mvn/
│   └── wrapper/                    # Maven Wrapper
│
├── lib/                            # 本地依赖库
│
├── logs/                           # 应用日志输出目录（挂载到 backend / logstash）
│
├── monitor/                        # 可观测性配置
│   ├── logstash.conf               # Logstash 管道配置
│   ├── prometheus.yml              # Prometheus 抓取配置
│   └── alert.rules.yml             # Prometheus 告警规则
│
├── sql/
│   └── init.sql                    # 数据库初始化脚本
│
├── src/
│   ├── main/
│   │   ├── java/                   # Java 源代码
│   │   └── resources/
│   │       ├── application.yml             # Spring Boot 公共配置
│   │       ├── application-dev.yml         # 本地开发环境
│   │       ├── application-docker.yml      # Docker 环境
│   │       └── logback/
│   │           └── logback-spring.xml      # 日志配置（JSON 输出）
│   │
│   └── test/                       # 测试代码
│
├── .dockerignore                   # Docker 构建忽略文件
├── .env.example                    # 环境变量模板
├── .gitattributes                  # Git 属性配置
├── .gitignore                      # Git 忽略配置
├── DESIGN.md                       # 项目设计文档
├── Dockerfile                      # Docker 镜像构建文件
├── compose.yaml                    # Docker Compose 配置（含所有服务）
├── pom.xml                         # Maven 配置
├── mvnw                            # Maven Wrapper
├── mvnw.cmd                        # Windows Maven Wrapper
└── README.md                       # 项目说明
```

---

## 🔄 系统架构

```text
                    ┌─────────────────┐
                    │   Vue 3 前端    │
                    └────────┬────────┘
                             │
                      HTTP / WebSocket
                             │
                             ▼
                  ┌──────────────────────┐
                  │    Spring Boot API   │
                  └──────────┬───────────┘
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
          ▼                  ▼                  ▼
      ┌────────┐        ┌──────────┐       ┌───────────┐
      │ MySQL  │        │  Redis   │       │ RabbitMQ  │
      └────────┘        └──────────┘       └───────────┘
          │                  │                  │
          ▼                  ▼                  ▼
       数据持久化          缓存/库存            异步消息
                         /分布式锁

───────────────────── 可观测性 ─────────────────────

   ┌─────────────────┐        ┌─────────────────┐
   │  node-exporter  │        │   Prometheus    │
   │  （系统指标）    │───────▶│  （指标抓取）    │
   └─────────────────┘        └────────┬────────┘
                                       │
                                       ▼
                              ┌─────────────────┐
                              │     Grafana     │
                              │  （可视化）      │
                              └─────────────────┘

   ┌─────────────────┐        ┌─────────────────┐
   │  Spring Boot    │        │    Logstash     │
   │  （JSON 日志）   │───────▶│  （日志收集）    │
   └─────────────────┘        └────────┬────────┘
                                       │
                                       ▼
                              ┌─────────────────┐        ┌─────────────────┐
                              │ Elasticsearch   │───────▶│     Kibana      │
                              │ （日志存储）     │        │  （日志查询）    │
                              └─────────────────┘        └─────────────────┘
```

---

# 🚀 快速开始

## 1. 环境要求

### 本地开发

- JDK 21
- Maven 3.x
- MySQL 8.x
- Redis 7.x
- RabbitMQ 3.x

### Docker 部署

- Docker
- Docker Compose
- 建议至少 4GB 内存（ELK + Prometheus + Grafana 资源占用较高）

使用 Docker Compose 部署时，可以直接通过容器运行 MySQL、Redis、RabbitMQ、Prometheus、Grafana、ELK 等，无需在本机单独安装。

> 💡 **Windows / macOS 用户**：请先安装并启动 [Docker Desktop](https://www.docker.com/products/docker-desktop/)，否则 `docker` 命令无法连接守护进程。
>
> 💡 **Linux 用户**：确保 `dockerd` 已启动（`sudo systemctl start docker`）。

---

## 2. 克隆项目

```bash
git clone https://github.com/xizhu080-commits/mall-primary-back-end.git

cd mall-primary-back-end
```

---

## 3. 配置环境变量（⚠️ 必做）

复制环境变量模板：

### Windows CMD

```cmd
copy .env.example .env
```

### PowerShell

```powershell
Copy-Item .env.example .env
```

### Linux / macOS

```bash
cp .env.example .env
```

然后根据本地环境修改 `.env`。**`.env` 文件至少需要包含以下变量**：

```env
# ============ 数据库 ============
DB_ROOT_PASSWORD=your_root_password
DB_DATABASE=mall
DB_USER=mall
DB_PASSWORD=your_password

# ============ RabbitMQ ============
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VHOST=/

# ============ JWT ============
JWT_SECRET=your_jwt_secret_at_least_32_chars

# ============ 支付宝沙箱（可选） ============
ALIPAY_APP_ID=
ALIPAY_PRIVATE_KEY=
ALIPAY_PUBLIC_KEY=
ALIPAY_NOTIFY_URL=
```

> ⚠️ `.env` 用于本地真实配置，**不要提交到 Git**。
>
> ⚠️ **支付宝沙箱配置说明**：需要在 [支付宝开放平台沙箱](https://open.alipay.com/develop/sandbox/app) 自行申请后填入。如果不测试支付功能，可以暂时留空，但**支付相关接口会不可用**。

---

# 🐳 Docker Compose 部署

> 后端镜像已发布到 Docker Hub：**`baizhou2026/mall:latest`**，可直接 `docker compose up -d` 使用；如需自行构建，见下方「🐋 Docker 镜像」章节。

## 1. 启动所有服务

```bash
docker compose up -d
```

启动后包含以下容器：

| 容器名 | 服务 | 端口 |
|---|---|---|
| `mall-db` | MySQL | 3307 |
| `mall-redis` | Redis | 6379 |
| `mall-rabbitmq` | RabbitMQ | 5672 / 15672 |
| `mall-backend` | Spring Boot 应用 | 8080 |
| `mall-prometheus` | Prometheus | 9090 |
| `mall-grafana` | Grafana | 3000 |
| `mall-node-exporter` | Node Exporter | 9100 |
| `mall-es` | Elasticsearch | 9201 |
| `mall-logstash` | Logstash | - |
| `mall-kibana` | Kibana | 5601 |

## 2. 查看服务状态

```bash
docker compose ps
```

正常情况下应看到所有容器状态为 `Up`，`db`、`redis`、`rabbitmq`、`elasticsearch` 等带健康检查的容器应为 `healthy`。

## 3. 查看日志

```bash
# 全部日志
docker compose logs -f

# 指定服务日志
docker compose logs -f backend
docker compose logs -f logstash
```

## 4. 停止服务

```bash
docker compose stop
```

## 5. 重启服务

```bash
docker compose restart
```

## 6. 停止并删除容器

```bash
docker compose down
```

## 7. 停止并删除容器及数据卷

```bash
docker compose down -v
```

> ⚠️ `docker compose down -v` 会删除 Docker Volume 中保存的数据，请谨慎使用。

---

## ✅ 验证部署是否成功

按以下步骤逐一确认：

1. **后端健康检查**：访问 `http://localhost:8080/actuator/health`，应返回 `{"status":"UP"}`
2. **Prometheus 指标**：访问 `http://localhost:8080/actuator/prometheus`，应返回大量指标数据
3. **Swagger UI**：访问 `http://localhost:8080/swagger-ui/index.html`，能看到接口列表
4. **RabbitMQ 管理台**：访问 `http://localhost:15672`，用 `.env` 中配置的账号登录（默认 `guest/guest`）
5. **Redis 连通性**：
   ```bash
   docker exec -it mall-redis redis-cli ping
   ```
   应返回 `PONG`
6. **MySQL 连通性**：
   ```bash
   docker exec -it mall-db mysqladmin ping -h localhost
   ```
   应返回 `mysqld is alive`
7. **Prometheus 目标状态**：访问 `http://localhost:9090/targets`，`mall-backend` 应显示为 **UP**
8. **Grafana 仪表盘**：访问 `http://localhost:3000`（`admin/admin`），导入 Dashboard `11378`，能看到 JVM、CPU、HTTP 指标
9. **Kibana 日志查询**：访问 `http://localhost:5601`，创建数据视图 `mall-backend-*`，在 Discover 页面能查到应用日志

---

## 🌐 服务地址

| 服务 | 地址 |
|---|---|
| 后端 API | `http://localhost:8080` |
| 健康检查 | `http://localhost:8080/actuator/health` |
| Prometheus 指标 | `http://localhost:8080/actuator/prometheus` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| RabbitMQ 管理端 | `http://localhost:15672` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |
| Kibana | `http://localhost:5601` |
| Elasticsearch | `http://localhost:9201` |
| MySQL | `localhost:3307` |
| Redis | `localhost:6379` |
| node-exporter | `http://localhost:9100` |

RabbitMQ 默认管理账号（以 `.env` 配置为准）：

```text
用户名：guest
密码：guest
```

Grafana 默认账号：

```text
用户名：admin
密码：admin
```

> ⚠️ **端口冲突提醒**：如果本机 `3307` / `6379` / `5672` / `15672` / `8080` / `9090` / `3000` / `9201` / `5601` 端口已被占用，请修改 `compose.yaml` 中的 `ports` 映射，或先停止本机对应服务。

---

# 📊 可观测性

## 监控：Prometheus + Grafana

### 1. 应用指标暴露

Spring Boot 通过 Actuator + Micrometer 暴露 Prometheus 格式指标：

- `/actuator/prometheus`：Prometheus 抓取端点
- `/actuator/health`：健康检查
- `/actuator/info`：应用信息

### 2. Prometheus 抓取配置

`monitor/prometheus.yml` 中配置了应用抓取目标：

```yaml
scrape_configs:
  - job_name: 'mall-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']
```

### 3. Grafana 仪表盘

推荐导入 Spring Boot 通用面板：

- Dashboard ID：**`11378`**（Spring Boot 2.1+ System Monitor）
- 备选：`4701`、`12900`

操作路径：

1. 左侧菜单 → **Connections** → **Data sources** → 添加 **Prometheus**，URL 填 `http://prometheus:9090`
2. 左侧菜单 → **Dashboards** → **New** → **Import** → 输入 `11378` → 选择数据源 → **Import**

### 4. 系统级监控

`node-exporter` 提供宿主机 CPU、内存、磁盘、网络等指标，可在 Prometheus Targets 中查看。

---

## 日志：ELK

### 1. Spring Boot 输出 JSON 日志

`logback-spring.xml` 通过 `logstash-logback-encoder` 输出 JSON 格式日志到 `/app/logs/mall-backend.json`。

`application.yml` 中指定配置路径：

```yaml
logging:
  config: classpath:logback/logback-spring.xml
```

### 2. Logstash 收集

`monitor/logstash.conf` 监听 `/app/logs/*.json` 并转发到 Elasticsearch：

```ruby
input {
  file {
    path => "/app/logs/*.json"
    start_position => "beginning"
    codec => json
    sincedb_path => "/dev/null"   # 调试阶段使用；稳定后改回持久化路径
  }
}

output {
  elasticsearch {
    hosts => ["http://elasticsearch:9200"]
    index => "mall-backend-%{+YYYY.MM.dd}"
  }
}
```

### 3. Kibana 查询

1. 访问 `http://localhost:5601`
2. 左侧菜单 → **Stack Management** → **Data Views** → **Create data view**
3. 填写：
    - **Name**：`mall-backend-*`
    - **Index pattern**：`mall-backend-*`
    - **Timestamp field**：`@timestamp`
4. 保存后，左侧菜单 → **Discover** → 选择 `mall-backend-*` → 查看日志

### 4. 常用查询示例

在 Discover 搜索框输入：

```text
level: ERROR                  # 只看错误日志
logger_name: com.mall.demo    # 只看业务日志
message: "WebSocket"          # 按关键字搜索
level: (ERROR OR WARN)        # 多级别筛选
```

---

# 🗄️ 数据库

数据库初始化脚本：

```text
sql/init.sql
```

`sql/init.sql` 通过 volume 挂载到 MySQL 容器的 `/docker-entrypoint-initdb.d/` 目录，**仅在数据卷为空时首次启动执行**。

如果修改了 `init.sql` 需要重新初始化，请先执行：

```bash
docker compose down -v
docker compose up -d
```

数据库信息以 `.env` 配置为准。

---

# 🔴 Redis

测试 Redis：

```bash
docker exec -it mall-redis redis-cli ping
```

正常情况下返回：

```text
PONG
```

进入 Redis：

```bash
docker exec -it mall-redis redis-cli
```

---

# 📨 RabbitMQ

RabbitMQ 管理后台：

```text
http://localhost:15672
```

查看队列：

```bash
docker exec -it mall-rabbitmq rabbitmqctl list_queues
```

查看交换机：

```bash
docker exec -it mall-rabbitmq rabbitmqctl list_exchanges
```

---

# 🔨 本地开发（IDEA 调试后端源码）

> 💡 **重要**：如果你要在 IDEA 中调试后端源码，**推荐只启动依赖服务（db、redis、rabbitmq），不要启动 backend 容器**，否则 8080 端口会冲突。

## 1. 只启动依赖服务

```bash
docker compose up -d db redis rabbitmq
```

## 2. 确认依赖服务健康

```bash
docker compose ps
```

确保 `mall-db`、`mall-redis`、`mall-rabbitmq` 状态为 `healthy` 或 `Up`。

## 3. IDEA 中连接的地址

| 服务 | 地址 |
|---|---|
| MySQL | `localhost:3307` |
| Redis | `localhost:6379` |
| RabbitMQ | `localhost:5672` |

> ⚠️ 注意 MySQL 端口是 **3307**（不是默认的 3306），因为容器映射到了宿主机 3307。

## 4. 运行项目

在 IDEA 中直接运行主启动类，或使用 Maven：

### Windows

```cmd
mvnw.cmd spring-boot:run
```

### Linux / macOS

```bash
./mvnw spring-boot:run
```

或者先打包：

```bash
mvn clean package
```

然后运行：

```bash
java -jar target/mall-primary-back-end-0.0.1-SNAPSHOT.jar
```

---

# 🐋 Docker 镜像

项目提供 `Dockerfile`，可以单独构建后端镜像。

构建：

```bash
docker build -t baizhou2026/mall:latest .
```

运行：

```bash
docker run -d \
  --name mall-backend \
  -p 8080:8080 \
  baizhou2026/mall:latest
```

如果使用 Docker Compose，推荐直接：

```bash
docker compose up -d
```

---

# 🔐 配置与安全

项目涉及以下敏感配置：

- 数据库密码
- JWT Secret
- 支付宝 App ID
- 支付宝私钥
- 支付宝公钥
- 支付宝异步通知地址

推荐使用环境变量进行配置：

```text
.env.example      # 示例配置，可以提交
.env              # 真实配置，不提交
```

不要将真实密码、私钥、Token 或其他敏感信息提交到 GitHub。

如果敏感信息已经提交到公开仓库，应及时更换对应密钥。

---

# 📚 API 文档

项目集成 SpringDoc OpenAPI。

启动项目后访问：

```text
http://localhost:8080/swagger-ui/index.html
```

可以查看和调试 RESTful API。

---

# 🧩 核心技术实现

## JWT + Spring Security

使用 JWT 完成用户身份认证，并结合 Spring Security 实现接口鉴权。

## Redis

Redis 主要用于：

- 数据缓存
- 商品库存
- 缓存穿透防护
- 缓存击穿防护
- 分布式锁
- Lua 原子操作

## Redis Lua

将库存检查和扣减操作放入 Lua 脚本中执行，保证库存操作的原子性，降低高并发场景下库存超卖风险。

## Redisson

使用 Redisson 实现分布式锁，控制高并发业务场景下的资源竞争。

## RabbitMQ

使用 RabbitMQ 实现异步消息处理和业务解耦。

## WebSocket

通过 WebSocket 向用户和商家实时推送支付结果等业务通知。

## 支付宝沙箱

集成支付宝沙箱环境，实现：

- 支付订单创建
- 支付二维码生成
- 异步支付通知
- 支付状态处理
- RSA2 签名验证
- 退款处理

## 可观测性实现

**指标采集**：

- `spring-boot-starter-actuator` + `micrometer-registry-prometheus` 暴露指标
- Prometheus 定时抓取 `/actuator/prometheus`
- Grafana 导入 Dashboard 可视化

**日志集中化**：

- `logstash-logback-encoder` 输出 JSON 日志到文件
- Logstash 通过 file input 读取并转发
- Elasticsearch 按天创建索引 `mall-backend-YYYY.MM.dd`
- Kibana 创建数据视图 `mall-backend-*` 查询

---

# 🧪 测试

运行测试：

```bash
mvn test
```

Windows：

```cmd
mvnw.cmd test
```

Linux / macOS：

```bash
./mvnw test
```

---

# ❓ 常见问题（FAQ）

**Q1：`docker compose up -d` 后 backend 一直重启？**

先看日志定位原因：

```bash
docker compose logs -f backend
```

常见原因：`.env` 未配置或变量为空、数据库未初始化完成、端口冲突。

**Q2：端口被占用怎么办？**

修改 `compose.yaml` 中对应服务的 `ports` 映射，例如把 `8080:8080` 改成 `8081:8080`。

**Q3：支付接口报错 / 无法创建支付订单？**

检查 `.env` 中的支付宝沙箱配置是否完整。未配置时支付相关接口不可用，其他功能不受影响。

**Q4：数据库连不上 / 表不存在？**

1. 确认 `mall-db` 容器状态健康：`docker compose ps`
2. 确认 `sql/init.sql` 已执行：首次启动时才会执行，若数据卷已存在需 `docker compose down -v` 后重来
3. 确认 `.env` 中的数据库名、用户名、密码与 `compose.yaml` 一致

**Q5：IDEA 里跑后端，连不上 MySQL？**

MySQL 映射到宿主机的是 **3307**，不是 3306。IDEA 配置里应写 `localhost:3307`。

**Q6：如何完全重置环境？**

```bash
docker compose down -v
docker compose up -d
```

**Q7：Prometheus Targets 里 `mall-backend` 显示 DOWN？**

常见原因：

- `prometheus.yml` 里的 `targets` 地址不对。Windows/macOS Docker Desktop 用 `host.docker.internal:8080`；Linux 用 `172.17.0.1:8080` 或宿主机实际 IP
- 应用没在跑，或 `/actuator/prometheus` 端点未暴露

**Q8：Grafana 里看不到 Prometheus 数据源？**

- 新版 Grafana 可能需要先安装 Prometheus 插件：**Administration → Plugins → Prometheus → Install**
- 安装后重启容器：`docker compose restart grafana`
- 添加数据源时 URL 填 `http://prometheus:9090`（不是 `localhost:9090`）

**Q9：Logstash 报错 `Expected one of [ \t\r\n], "#", "input", "filter", "output"`？**

`logstash.conf` 文件格式不对，可能是：

- 文件里是 XML 内容（应该是 ruby DSL）
- 文件开头有 BOM 字符（用编辑器改成「UTF-8 无 BOM」）

**Q10：Kibana 建数据视图提示「没有匹配的索引」？**

说明 Elasticsearch 里还没有 `mall-backend-*` 索引。检查：

1. `logs/` 目录里有没有 `mall-backend.json`
2. Logstash 是否成功读取（`docker logs mall-logstash`）
3. ES 里是否有索引：`curl "http://localhost:9201/_cat/indices?v"`

**Q11：Logstash 启动成功但读不到日志？**

最常见原因是 `sincedb` 缓存。调试阶段把 `logstash.conf` 里的 `sincedb_path` 改成 `/dev/null`，重启 Logstash。

**Q12：ELK 占用内存太高？**

Elasticsearch 默认需要 2GB 内存。可以在 `compose.yaml` 中调整：

```yaml
environment:
  - ES_JAVA_OPTS=-Xms512m -Xmx512m
```

Logstash 同理：

```yaml
environment:
  - LS_JAVA_OPTS=-Xms256m -Xmx256m
```

---

# 📝 Git 提交规范

推荐使用以下提交格式：

```text
feat: 新增功能
fix: 修复问题
refactor: 重构代码
docs: 修改文档
style: 代码格式调整
test: 添加测试
chore: 构建或工具配置修改
```

例如：

```bash
git add .

git commit -m "feat: add coupon calculation"

git push origin main
```

---

# 🚧 项目状态

项目处于持续开发阶段。

当前主要功能：

- [x] 用户认证
- [x] JWT 登录鉴权
- [x] 商品管理
- [x] SKU 管理
- [x] 商品搜索
- [x] 商品详情
- [x] 订单创建
- [x] 订单管理
- [x] Redis 库存扣减
- [x] 优惠券
- [x] 支付宝沙箱支付
- [x] 退款
- [x] RabbitMQ
- [x] WebSocket
- [x] Docker Compose
- [x] Prometheus + Grafana 监控
- [x] ELK 日志集中化

---

# 📄 License

本项目仅用于学习、研究与技术交流。