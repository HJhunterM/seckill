# 简介
这是一个商城秒杀项目，核心做法是靠 Redis + lua脚本 实现库存原子预扣减

下单等耗时操作通过 Kafka 异步消费处理，以提升系统并发性能

项目在本地跑需要有java8环境哦，java应用在宿主机运行

其他环境通过docker构建 docker-compose up -d 即可

此外，需要提前预热Redis：

1、设置商品1的库存为100

docker exec seckill-redis redis-cli SET "SK:Stock:1" 100

2、设置商品1的每人限购数量为2
docker exec seckill-redis redis-cli SET "SK:Limit:1" 2

（验证）
docker exec seckill-redis redis-cli GET "SK:Stock:1"

docker exec seckill-redis redis-cli GET "SK:Limit:1"


查看Kafka消息：

看最新5条消息：
docker exec seckill-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic tp-seckill --from-beginning --max-messages 5

看消费组状态：
docker exec seckill-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group TEST_GROUP

持续监听新消息：
docker exec seckill-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic tp-seckill