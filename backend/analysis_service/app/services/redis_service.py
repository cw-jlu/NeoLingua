"""
Redis服务 - 排名和缓存
使用Sorted Set实现排名功能
"""
import json
import logging
from typing import Optional, List, Dict
import redis.asyncio as aioredis
from app.config import settings

logger = logging.getLogger(__name__)

# Redis键前缀
RANKING_KEY = "analysis:ranking:overall"  # 总排名Sorted Set
USER_STATS_KEY = "analysis:user:stats:{user_id}"  # 用户统计缓存
DAILY_STATS_KEY = "analysis:daily:{date}"  # 每日统计缓存


class RedisService:
    """Redis服务"""

    def __init__(self):
        self.redis: Optional[aioredis.Redis] = None

    async def connect(self):
        """连接Redis"""
        try:
            self.redis = aioredis.Redis(
                host=settings.REDIS_HOST,
                port=settings.REDIS_PORT,
                password=settings.REDIS_PASSWORD or None,
                db=settings.REDIS_DB,
                decode_responses=True
            )
            await self.redis.ping()
            logger.info("Redis连接成功")
        except Exception as e:
            logger.error(f"Redis连接失败: {e}")
            self.redis = None

    async def disconnect(self):
        """断开Redis"""
        if self.redis:
            await self.redis.close()

    # ===== 排名功能 (Sorted Set) =====

    async def update_ranking(self, user_id: int, score: float):
        """更新用户排名分数"""
        if not self.redis:
            return
        await self.redis.zadd(RANKING_KEY, {str(user_id): score})

    async def get_user_rank(self, user_id: int) -> int:
        """获取用户排名（从高到低，1开始）"""
        if not self.redis:
            return 0
        rank = await self.redis.zrevrank(RANKING_KEY, str(user_id))
        return (rank + 1) if rank is not None else 0

    async def get_top_rankings(self, top_n: int = 50) -> List[Dict]:
        """获取排名前N的用户"""
        if not self.redis:
            return []
        results = await self.redis.zrevrange(RANKING_KEY, 0, top_n - 1, withscores=True)
        rankings = []
        for i, (user_id, score) in enumerate(results):
            rankings.append({
                "rank": i + 1,
                "user_id": int(user_id),
                "ranking_score": round(score, 2)
            })
        return rankings

    async def get_ranking_count(self) -> int:
        """获取排名总人数"""
        if not self.redis:
            return 0
        return await self.redis.zcard(RANKING_KEY)

    # ===== 缓存功能 =====

    async def cache_user_stats(self, user_id: int, stats: dict, ttl: int = 300):
        """缓存用户统计数据（5分钟）"""
        if not self.redis:
            return
        key = USER_STATS_KEY.format(user_id=user_id)
        await self.redis.setex(key, ttl, json.dumps(stats, default=str))

    async def get_cached_user_stats(self, user_id: int) -> Optional[dict]:
        """获取缓存的用户统计"""
        if not self.redis:
            return None
        key = USER_STATS_KEY.format(user_id=user_id)
        data = await self.redis.get(key)
        return json.loads(data) if data else None

    async def health_check(self) -> bool:
        """健康检查"""
        try:
            if self.redis:
                await self.redis.ping()
                return True
        except Exception:
            pass
        return False


redis_service = RedisService()
