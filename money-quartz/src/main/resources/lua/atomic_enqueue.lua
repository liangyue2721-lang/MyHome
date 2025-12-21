-- KEYS:
--   1 -> PENDING_SET (e.g. task:pending_ids)
--   2 -> QUEUE_LIST (e.g. task:queue:global or task:queue:node:normal)
-- ARGV:
--   1 -> taskId
--   2 -> taskJson

if redis.call("SISMEMBER", KEYS[1], ARGV[1]) == 0 then
    redis.call("SADD", KEYS[1], ARGV[1])
    redis.call("LPUSH", KEYS[2], ARGV[2])
    return 1
end
return 0
