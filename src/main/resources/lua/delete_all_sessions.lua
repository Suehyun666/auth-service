-- KEYS[1] = acct_sessions:{accountId}

local setKey = KEYS[1]
local members = redis.call("SMEMBERS", setKey)

for i, sk in ipairs(members) do
  redis.call("DEL", sk)
end

redis.call("DEL", setKey)

return #members
