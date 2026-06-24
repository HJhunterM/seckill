# 高并发秒杀系统 (Seckill Service)

电商促销秒杀系统，面向C端用户提供限时限量抢购场景（如新品首发、节日大促、爆款秒杀），支持商品预热、实时库存扣减、用户限购管控、订单异步生成，解决瞬时高并发下的超卖、重复下单、系统雪崩问题，保障促销活动平稳运行。

## 技术栈

- **框架**: Spring Boot 2.7.18 + MyBatis
- **数据库**: MySQL 8.0
- **缓存**: Redis 7 (Lua脚本原子操作)
- **消息队列**: Kafka 7.5.0
- **构建工具**: Maven
- **压测工具**: wrk
- **Java版本**: 1.8

## 核心特性

### 三种秒杀模式

| 版本 | 模式 | 实现方式 | 特点 |
|------|------|---------|------|
| **V1** | 同步直写 | 直接扣减MySQL库存 | 简单可靠，性能较低 |
| **V2** | Redis预扣减 | Lua脚本原子扣减Redis库存，异步落库 | 高性能，防超卖 |
| **V3** | 异步Kafka | Redis预扣减 + Kafka异步下单 | 削峰填谷，最高并发 |

### 关键技术点

- **Redis Lua脚本**: 原子性实现库存扣减、用户限购校验、防重复提交
- **分布式锁**: 基于Redis的可重入分布式锁（`ReentrantDistributeLock`）
- **Kafka异步**: 手动ACK模式，消费者组管理，消息可靠性保障
- **数据库设计**: 6张核心表，唯一索引防重，乐观锁扣库存
- **性能压测**: 提供wrk压测脚本，支持QPS、P99延迟测试

## 快速开始

### 前置要求

- Docker & Docker Compose
- JDK 1.8+
- Maven 3.6+
- wrk（可选，用于压测）

### 1. 启动中间件

```bash
docker-compose up -d
```

启动服务：
- MySQL (端口 3307)
- Redis (端口 6379)
- Kafka (端口 9092)
- Kafka UI (端口 8090，可选)

可选启动Kafka UI：
```bash
docker-compose --profile ui up -d
```

### 2. 编译运行

```bash
mvn clean package -DskipTests
java -jar target/seckill-service-0.0.1-SNAPSHOT.jar
```

或使用IDEA运行 `SecKillApplication` 主类。

### 3. 预热Redis库存

```bash
# 例如：
# 设置商品1的库存为100
docker exec seckill-redis redis-cli SET "SK:Stock:1" 100
# 设置商品1的每人限购数量为2
docker exec seckill-redis redis-cli SET "SK:Limit:1" 2
```

### 4. 验证服务

```bash
curl http://localhost:8080/sec_kill/get_goods_info?goodsNum=abc123 \
  -H "Trace-ID: test123" \
  -H "User-ID: 1"
```

## API接口

### 商品查询

**获取商品详情**
```
GET /sec_kill/get_goods_info?goodsNum=abc123
Headers: Trace-ID, User-ID
```

**获取商品列表**
```
GET /sec_kill/get_goods_list?offset=0&limit=20
Headers: Trace-ID, User-ID
```

### 秒杀下单

**V1 - 同步版本**
```
POST /sec_kill/v1/sec_kill
Headers: Trace-ID, User-ID, Content-Type: application/json
Body: {"goodsNum":"abc123", "num":1}
```

**V2 - Redis预扣减版本**
```
POST /sec_kill/v2/sec_kill
Headers: Trace-ID, User-ID, Content-Type: application/json
Body: {"goodsNum":"abc123", "num":1}
```

**V3 - Kafka异步版本**
```
POST /sec_kill/v3/sec_kill
Headers: Trace-ID, User-ID, Content-Type: application/json
Body: {"goodsNum":"abc123", "num":1}
```

## 性能压测

### 安装wrk

```bash
brew install wrk  # macOS
```

### 执行压测（例子，真实压测执行压测脚本即可）

```bash
cd wrkbench

# V1压测 (8线程, 100并发, 10秒)
wrk -t8 -c100 -d10s -T1s --script=sec_kill_v1.lua --latency \
  "http://127.0.0.1:8080/sec_kill/v1/sec_kill"

# V2压测
wrk -t8 -c100 -d10s -T1s --script=sec_kill_v2.lua --latency \
  "http://127.0.0.1:8080/sec_kill/v2/sec_kill"

# V3压测
wrk -t8 -c100 -d10s -T1s --script=sec_kill_v3.lua --latency \
  "http://127.0.0.1:8080/sec_kill/v3/sec_kill"
```

### 压测指标

wrk输出示例：
```
Running 10s test @ http://127.0.0.1:8080/sec_kill/v2/sec_kill
  8 threads and 100 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    45.23ms   12.45ms  120ms   75.32%
    Req/Sec   234.56     45.67   350.00   68.75%
  Latency Distribution
     50%   42.00ms
     75%   50.00ms
     90%   60.00ms
     99%   85.00ms
  18765 requests in 10.02s, 2.34MB read
Requests/sec:   1873.45
```

关键指标：
- **Requests/sec**: QPS（每秒请求数）
- **99% Latency**: P99延迟
- **Socket errors**: 超时/错误数

## 项目结构

```
seckill-mhj/
├── docker-compose.yml          # Docker编排（MySQL/Redis/Kafka）
├── pom.xml                     # Maven配置
├── sql/
│   └── init.sql                # 数据库建表脚本
├── src/main/
│   ├── java/com/darkhj/seckill/
│   │   ├── SecKillApplication.java          # 启动类
│   │   ├── controller/
│   │   │   └── SecKillController.java       # REST API
│   │   ├── service/
│   │   │   ├── SecKillService.java          # 接口
│   │   │   └── impl/SecKillServiceImpl.java # 业务实现+Lua脚本
│   │   ├── mapper/                          # MyBatis Mapper
│   │   ├── model/                           # 实体类
│   │   ├── kafka/
│   │   │   ├── config/KafkaConfig.java      # Kafka配置
│   │   │   └── consumer/SecKillConsumer.java# 异步消费者
│   │   ├── common/redis/
│   │   │   ├── RedisConfig.java             # Redis配置
│   │   │   ├── RedisBase.java               # Redis工具类
│   │   │   └── ReentrantDistributeLock.java # 分布式锁
│   │   └── exception/                       # 全局异常处理
│   └── resources/
│       ├── application.yml                  # 应用配置
│       ├── logback.xml                      # 日志配置
│       ├── lua/                             # Lua脚本
│       │   ├── secKill.lua                  # 预扣减库存
│       │   └── setSecKillSuccess.lua        # 设置成功
│       └── mapper/                          # MyBatis XML
└── wrkbench/                    # wrk压测脚本
    ├── sec_kill_v1.lua          # V1压测脚本
    ├── sec_kill_v2.lua          # V2压测脚本
    └── sec_kill_v3.lua          # V3压测脚本
```

## 数据库设计

### 核心表

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `t_goods` | 商品表 | goods_num(唯一), price, seller |
| `t_seckill_stock` | 秒杀库存表 | goods_id, stock |
| `t_quota` | 全局限额表 | goods_id, num(限购数) |
| `t_user_quota` | 用户限额表 | user_id, goods_id, killed_num |
| `t_order` | 订单表 | order_num(唯一), buyer, seller |
| `t_seckill_record` | 秒杀记录表 | sec_num(唯一), user_id, goods_id |

### 防超卖机制

- **唯一索引**: `order_num`、`sec_num` 防重复下单
- **乐观锁**: `UPDATE stock = stock - num WHERE stock >= num`
- **Redis原子操作**: Lua脚本保证库存扣减原子性

## 关键配置

### application.yml

```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3307/dark-miaosha
  redis:
    host: localhost
    port: 6379
  kafka:
    bootstrap-servers: 127.0.0.1:9092
```

### Docker端口映射

| 服务 | 宿主机端口 | 容器端口 |
|------|-----------|---------|
| MySQL | 3307 | 3306 |
| Redis | 6379 | 6379 |
| Kafka | 9092 | 9092 |
| Kafka UI | 8090 | 8080 |

## 监控与排查

### 查看Kafka消费情况

```bash
# 命令行查看消息
docker exec seckill-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic tp-seckill \
  --from-beginning \
  --max-messages 5

# 查看消费者组状态
docker exec seckill-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group TEST_GROUP
```

### 查看Redis数据

```bash
docker exec seckill-redis redis-cli
> KEYS SK:*
> GET SK:Stock:1
> GET SK:UserSecKilledNum:1:1
```

### 查看数据库

```bash
docker exec seckill-mysql mysql -uroot -p123456 \
  -e "USE dark-miaosha; SELECT COUNT(*) FROM t_order;"
```



## License

MIT
