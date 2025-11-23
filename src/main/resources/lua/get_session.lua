-- KEYS[1] = session:{sessionId}
-- ARGV[1] = ttlSeconds

local sessionKey = KEYS[1]
local ttl = tonumber(ARGV[1])

local accountId = redis.call("GET", sessionKey)
if accountId then
    redis.call("EXPIRE", sessionKey, ttl)

    local setKey = "acct_sessions:" .. accountId
    redis.call("EXPIRE", setKey, ttl)

    return accountId
else
    return nil
end
