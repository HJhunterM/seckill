# 简介
这是一个商城秒杀项目，核心做法是靠 Redis + lua脚本 实现库存原子预扣减

下单等耗时操作通过 Kafka 异步消费处理，以提升系统并发性能

项目在本地跑需要有java8环境哦，java应用在宿主机运行

其他环境通过docker构建 docker-compose up -d 即可

此外，需要提前预热Redis：

比如，设置商品1的库存为100

docker exec seckill-redis redis-cli SET "SK:Stock:1" 100

验证

docker exec seckill-redis redis-cli GET "SK:Stock:1"
