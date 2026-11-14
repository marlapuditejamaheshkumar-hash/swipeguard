-- KEYS[1] = vel:{cardRef}   (sorted set: member = transactionId, score = timestamp millis)
-- ARGV[1] = window start (millis), ARGV[2] = request timestamp (millis)
-- ARGV[3] = transactionId,         ARGV[4] = TTL (millis)
redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', '(' .. ARGV[1])   -- trim old entries
redis.call('ZADD', KEYS[1], ARGV[2], ARGV[3])                      -- add this request
redis.call('PEXPIRE', KEYS[1], ARGV[4])                            -- idle cards leave no keys
return redis.call('ZCARD', KEYS[1])                                -- count