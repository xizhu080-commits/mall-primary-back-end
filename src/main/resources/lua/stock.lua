
--[[

local stock = tonumber(redis.call('GET', KEYS[1]))

if stock == nil then
    return -1
end

if stock < tonumber(ARGV[1]) then
    return -2
end

redis.call('DECRBY', KEYS[1], ARGV[1])

return 1

 ]]

local stock = redis.call('GET', KEYS[1])
if stock == false then
    return -1   -- 不存在
end
stock = tonumber(stock)
if stock < tonumber(ARGV[1]) then
    return -2   -- 库存不足
end
redis.call('DECRBY', KEYS[1], ARGV[1])
return 1        -- 扣减成功