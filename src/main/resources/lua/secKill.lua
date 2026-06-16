-- KEYS[1]: 用户ID
-- KEYS[2]: 商品ID
-- KEYS[3]: 抢购数量
-- KEYS[4]: 秒杀单号(secNum)
-- KEYS[5]: 秒杀记录(PreSecKillRecord的JSON)

-- 构造RedisKey
local keyLimit = "SK:Limit:" .. KEYS[2]
local keyUserGoodsSecNum = "SK:UserGoodsSecNum:" .. KEYS[1] .. ":" .. KEYS[2]
local keyUserSecKilledNum = "SK:UserSecKilledNum:" .. KEYS[1] .. ":" .. KEYS[2]

local retAry = {0, ""}

-- 1. 判断用户是否已经在秒杀中（防重复提交）
local alreadySecNum = redis.call('get', keyUserGoodsSecNum)
if alreadySecNum and string.len(alreadySecNum) ~= 0 then
   retAry[1] = -1
   retAry[2] = alreadySecNum
   return retAry
end

-- 2. 判断用户是否超过限额
local limit = redis.call('get', keyLimit)
local userSecKilledNum = redis.call('get', keyUserSecKilledNum)
if limit and userSecKilledNum and tonumber(userSecKilledNum) + tonumber(KEYS[3]) > tonumber(limit) then
   retAry[1] = -2
   return retAry
end

-- 3. 检查库存是否充足
local stockKey = "SK:Stock:" .. KEYS[2]
local stock = redis.call('get', stockKey)
if not stock or tonumber(stock) < tonumber(KEYS[3]) then
   retAry[1] = -3
   return retAry
end

-- 4. 库存充足，扣减操作
redis.call('decrby', stockKey, KEYS[3])
redis.call('incrby', keyUserSecKilledNum, KEYS[3])
redis.call('set', keyUserGoodsSecNum, KEYS[4])
redis.call('set', KEYS[4], KEYS[5])
return retAry
