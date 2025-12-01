-- KEYS[1] = acct_sessions:{accountId}
-- ARGV[1] = accountId
-- ARGV[2] = ttlSeconds

local setKey = KEYS[1]
local accountId = ARGV[1]
local ttl = tonumber(ARGV[2])

local oldSessions = redis.call("SMEMBERS", setKey)
for i, oldSessionKey in ipairs(oldSessions) do
    redis.call("DEL", oldSessionKey)
end
redis.call("DEL", setKey)

local newSessionId = redis.call("INCR", "session:id_gen")
local sessionKey = "session:" .. newSessionId

redis.call("SET", sessionKey, accountId .. "," .. accountId, "EX", ttl)
redis.call("SADD", setKey, sessionKey)
redis.call("EXPIRE", setKey, ttl)

return tostring(newSessionId)
