-- KEYS[1] = session:{sessionId}
-- KEYS[2] = acct_sessions:{accountId}

local sessionKey = KEYS[1]
local setKey = KEYS[2]

redis.call("DEL", sessionKey)
redis.call("SREM", setKey, sessionKey)

return 1
