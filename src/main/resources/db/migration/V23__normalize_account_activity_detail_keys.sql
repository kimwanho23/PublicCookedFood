UPDATE account_activity_log
SET detail = REPLACE(detail, 'targetUserId=', 'targetAccountId=')
WHERE detail LIKE '%targetUserId=%';
