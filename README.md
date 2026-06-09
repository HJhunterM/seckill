
本项目需要有java8环境哦，项目在宿主机运行，
其他环境通过docker构建 docker-compose up -d 即可

此外，需要提前预热Redis：

#设置商品1的库存为100
docker exec seckill-redis redis-cli SET "SK:Stock:1" 100

#验证
docker exec seckill-redis redis-cli GET "SK:Stock:1"

