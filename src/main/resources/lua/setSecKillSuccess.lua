-- KEYS[1]: 用户ID
-- KEYS[2]: 商品ID
-- KEYS[3]: 秒杀单号(secNum)
-- KEYS[4]: 秒杀记录(PreSecKillRecord的JSON)

-- 构造RedisKey
local keyUserGoodsSecNum = "SK:UserGoodsSecNum:" .. KEYS[1] .. ":" .. KEYS[2]

local retAry = {0, ""}

-- 清空用户在秒杀中的标记（设为空字符串）
redis.call('set', keyUserGoodsSecNum, "")
-- 存储最终秒杀记录JSON
redis.call('set', KEYS[3], KEYS[4])

return retAry
