可以，下面这个**直接复制到 `README.md`** 即可：

````markdown
# 🛒 Mall Primary Backend

> 基于 Spring Boot 3 + MyBatis-Plus 构建的电商后端系统，涵盖用户认证、商品管理、订单交易、库存控制、优惠券、支付、消息队列等核心业务，并支持 Docker Compose 一键部署。

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

---

## 🏗️ 项目结构

```text
mall-primary-back-end/
├── .mvn/
│   └── wrapper/                 # Maven Wrapper
│
├── lib/                         # 本地依赖库
│
├── sql/
│   └── init.sql                 # 数据库初始化脚本
│
├── src/
│   ├── main/
│   │   ├── java/                # Java 源代码
│   │   └── resources/
│   │       └── application.yml  # Spring Boot 配置
│   │
│   └── test/                    # 测试代码
│
├── .dockerignore                # Docker 构建忽略文件
├── .env.example                 # 环境变量模板
├── .gitattributes               # Git 属性配置
├── .gitignore                   # Git 忽略配置
├── DESIGN.md                    # 项目设计文档
├── Dockerfile                   # Docker 镜像构建文件
├── compose.yaml                 # Docker Compose 配置
├── pom.xml                      # Maven 配置
├── mvnw                         # Maven Wrapper
├── mvnw.cmd                     # Windows Maven Wrapper
└── README.md                    # 项目说明
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

使用 Docker Compose 部署时，可以直接通过容器运行 MySQL、Redis、RabbitMQ，无需在本机单独安装这些服务。

---

## 2. 克隆项目

```bash
git clone https://github.com/xizhu080-commits/mall-primary-back-end.git

cd mall-primary-back-end
```

---

## 3. 配置环境变量

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

然后根据本地环境修改 `.env`。

> ⚠️ `.env` 用于本地真实配置，不要提交到 Git。

---

# 🐳 Docker Compose 部署

## 1. 启动所有服务

```bash
docker compose up -d
```

## 2. 查看服务状态

```bash
docker compose ps
```

## 3. 查看日志

```bash
docker compose logs -f
```

查看后端日志：

```bash
docker compose logs -f backend
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

## 🌐 服务地址

| 服务 | 地址 |
|---|---|
| 后端 API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| RabbitMQ 管理端 | `http://localhost:15672` |
| MySQL | `localhost:3307` |
| Redis | `localhost:6379` |

RabbitMQ 默认管理账号：

```text
用户名：guest
密码：guest
```

---

# 🗄️ 数据库

数据库初始化脚本：

```text
sql/init.sql
```

首次启动 MySQL 容器时，会自动执行初始化 SQL。

数据库信息根据 `.env` 配置为准。

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

# 🔨 本地开发

如果不使用 Docker 运行后端，可以直接使用 Maven。

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
docker build -t mall-backend:latest .
```

运行：

```bash
docker run -d \
  --name mall-backend \
  -p 8080:8080 \
  mall-backend:latest
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
http://localhost:8080/swagger-ui.html
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

---

# 📄 License

本项目仅用于学习、研究与技术交流。
````
