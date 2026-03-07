"""
SpeakMaster 快速极限并发测试
快速测试所有接口的大致极限并发能力，不需要太精确
"""

import asyncio
import aiohttp
import json
import time
from datetime import datetime
from typing import Dict, List

# API 配置
API_GATEWAY = "http://localhost:8080"
AI_SERVICE = "http://localhost:8089"
ANALYSIS_SERVICE = "http://localhost:8085"

class QuickMaxConcurrencyTester:
    """快速极限并发测试器"""
    
    def __init__(self):
        self.results = []
        self.session = None
    
    async def setup(self):
        """初始化"""
        timeout = aiohttp.ClientTimeout(total=60)
        self.session = aiohttp.ClientSession(timeout=timeout)
        print("🚀 初始化测试环境...")
    
    async def teardown(self):
        """清理"""
        if self.session:
            await self.session.close()
        print("🧹 清理测试环境...")
    
    async def test_max_concurrency(self, name: str, url: str, method: str = "GET", 
                                   data: Dict = None, url_params_func=None, headers: Dict = None):
        """快速测试接口的极限并发 - 准确到百位"""
        print(f"\n📊 测试 {name} 极限并发...")
        
        # 测试并发级别：100, 200, 300, ..., 3000 (每次增加100)
        test_levels = list(range(100, 3100, 100))
        max_successful = 0
        
        for concurrency in test_levels:
            print(f"  测试并发数: {concurrency}...", end=" ")
            
            start_time = time.time()
            
            # 创建并发任务
            async def make_request(index):
                test_url = url
                if url_params_func:
                    test_url = url + url_params_func(index)
                
                try:
                    if method == "GET":
                        async with self.session.get(test_url, headers=headers) as response:
                            return response.status
                    elif method == "POST":
                        async with self.session.post(test_url, json=data, headers=headers) as response:
                            return response.status
                    elif method == "PUT":
                        async with self.session.put(test_url, json=data, headers=headers) as response:
                            return response.status
                    elif method == "DELETE":
                        async with self.session.delete(test_url, headers=headers) as response:
                            return response.status
                    else:
                        return None
                except Exception:
                    return None
            
            tasks = [make_request(i) for i in range(concurrency)]
            
            try:
                responses = await asyncio.gather(*tasks, return_exceptions=True)
                duration = time.time() - start_time
                
                # 统计成功率
                successful = sum(1 for r in responses 
                               if not isinstance(r, Exception) and r == 200)
                success_rate = successful / concurrency * 100
                qps = concurrency / duration if duration > 0 else 0
                
                print(f"成功率: {success_rate:.1f}%, QPS: {qps:.2f}")
                
                # 如果成功率 >= 90%，记录这个并发数
                if success_rate >= 90:
                    max_successful = concurrency
                else:
                    # 成功率低于90%，停止测试更高并发
                    break
                
                # 等待一下
                await asyncio.sleep(0.5)
                
            except Exception as e:
                print(f"失败: {str(e)[:50]}")
                break
        
        # 保存结果
        self.results.append({
            "interface": name,
            "max_concurrency": max_successful,
            "url": url
        })
        
        print(f"  🏆 {name} 极限并发: {max_successful}")
        return max_successful
    
    async def run_all_tests(self):
        """运行所有用户接口的极限并发测试"""
        print("\n" + "="*80)
        print("🚀 快速极限并发测试 - 所有用户接口")
        print("="*80)
        print("\n测试策略: 从100开始，每次增加100并发，找出成功率>=90%的最高并发数（精确到百位）")
        print("="*80)
        
        # ==================== AI聊天相关 ====================
        
        # 1. AI聊天
        await self.test_max_concurrency(
            "AI聊天",
            f"{AI_SERVICE}/ai/chat",
            "POST",
            {
                "session_id": f"quick_test_{int(time.time())}",
                "user_id": "test_user",
                "message": "Hello",
                "role_prompt": "You are a helpful teacher."
            }
        )
        
        # 2. AI模型列表
        await self.test_max_concurrency(
            "AI模型列表",
            f"{AI_SERVICE}/ai/models",
            "GET"
        )
        
        # 3. 停止生成
        await self.test_max_concurrency(
            "停止生成",
            f"{AI_SERVICE}/ai/chat/stop",
            "POST",
            {"session_id": f"stop_test_{int(time.time())}"}
        )
        
        # 4. 生成反馈
        await self.test_max_concurrency(
            "生成反馈",
            f"{AI_SERVICE}/ai/feedback",
            "POST",
            {
                "user_id": "test_user",
                "session_id": f"feedback_{int(time.time())}",
                "message": "I goed to school yesterday",
                "context": "grammar correction"
            }
        )
        
        # 5. 发音分析
        await self.test_max_concurrency(
            "发音分析",
            f"{AI_SERVICE}/ai/pronunciation",
            "POST",
            {
                "user_id": "test_user",
                "text": "Hello world",
                "reference_text": "Hello world"
            }
        )
        
        # ==================== 高级功能 ====================
        
        # 6. 中式英语检测
        await self.test_max_concurrency(
            "中式英语检测",
            f"{AI_SERVICE}/advanced/chinglish/detect",
            "POST",
            {"text": "I very like this book"}
        )
        
        # 7. 实时交互启动
        await self.test_max_concurrency(
            "实时交互启动",
            f"{AI_SERVICE}/advanced/realtime/start",
            "POST",
            {
                "mode": "half_duplex",
                "user_profile": {"level": "intermediate"}
            },
            lambda i: f"?user_id=test_{i}&session_id=quick_{int(time.time())}_{i}"
        )
        
        # 8. 实时音频处理
        await self.test_max_concurrency(
            "实时音频处理",
            f"{AI_SERVICE}/advanced/realtime/audio",
            "POST",
            {
                "energy_level": 0.7,
                "pitch_variance": 0.5,
                "speech_rate": 150,
                "pause_duration": 0.5,
                "voice_activity": True,
                "confidence_score": 0.85,
                "transcribed_text": "Hello"
            },
            lambda i: f"?session_id=audio_{int(time.time())}_{i}"
        )
        
        # 9. 会话状态
        await self.test_max_concurrency(
            "会话状态",
            f"{AI_SERVICE}/advanced/session/status/test_session",
            "GET"
        )
        
        # 10. 更新交互模式
        await self.test_max_concurrency(
            "更新交互模式",
            f"{AI_SERVICE}/advanced/session/mode/test_session",
            "PUT",
            None,
            lambda i: f"?new_mode=full_duplex"
        )
        
        # 11. 结束会话
        await self.test_max_concurrency(
            "结束会话",
            f"{AI_SERVICE}/advanced/realtime/end",
            "POST",
            None,
            lambda i: f"?session_id=end_{int(time.time())}_{i}"
        )
        
        # 12. 口语评估
        await self.test_max_concurrency(
            "口语评估",
            f"{AI_SERVICE}/advanced/assessment/speaking",
            "POST",
            {
                "user_message": "I think learning English is important.",
                "audio_analysis": {
                    "speech_rate": 150,
                    "pause_count": 2,
                    "pronunciation_score": 75
                },
                "assessment_type": "ielts"
            },
            lambda i: f"?user_id=test_{i}&session_id=assess_{int(time.time())}_{i}"
        )
        
        # 13. 补全帮助
        await self.test_max_concurrency(
            "补全帮助",
            f"{AI_SERVICE}/advanced/completion/help",
            "POST",
            {
                "incomplete_text": "I want to",
                "user_profile": {"level": "intermediate"}
            },
            lambda i: f"?user_id=test_{i}&session_id=comp_{int(time.time())}_{i}"
        )
        
        # 14. 进度追踪
        await self.test_max_concurrency(
            "进度追踪",
            f"{AI_SERVICE}/advanced/progress/test_user",
            "GET",
            None,
            lambda i: f"?days=30"
        )
        
        # 15. 批量评估
        await self.test_max_concurrency(
            "批量评估",
            f"{AI_SERVICE}/advanced/batch/assess?user_id=test_user&assessment_type=ielts",
            "POST",
            {
                "sessions": ["session1", "session2"]
            }
        )
        
        # 16. 分析概览
        await self.test_max_concurrency(
            "分析概览",
            f"{AI_SERVICE}/advanced/analytics/overview/test_user",
            "GET",
            None,
            lambda i: f"?days=30"
        )
        
        # ==================== RAG知识库 ====================
        
        # 17. 列出全局文档
        await self.test_max_concurrency(
            "列出全局文档",
            f"{AI_SERVICE}/ai/rag/documents",
            "GET"
        )
        
        # 18. 搜索文档 (需要query参数)
        await self.test_max_concurrency(
            "搜索文档",
            f"{AI_SERVICE}/ai/rag/search?query=English%20learning&top_k=5",
            "POST",
            {}
        )
        
        # 19. 列出用户文档 (需要X-User-Id header)
        await self.test_max_concurrency(
            "列出用户文档",
            f"{AI_SERVICE}/ai/rag/user/documents",
            "GET",
            None,
            None,
            {"X-User-Id": "test_user"}
        )
        
        # 20. 列出用户角色 (需要X-User-Id header)
        await self.test_max_concurrency(
            "列出用户角色",
            f"{AI_SERVICE}/ai/rag/user/roles",
            "GET",
            None,
            None,
            {"X-User-Id": "test_user"}
        )
        
        # 21. 用户搜索文档 (需要X-User-Id header)
        await self.test_max_concurrency(
            "用户搜索文档",
            f"{AI_SERVICE}/ai/rag/user/search?query=English%20learning",
            "POST",
            {
                "top_k": 5,
                "include_global": True
            },
            None,
            {"X-User-Id": "test_user"}
        )
    
    def generate_summary(self):
        """生成测试总结"""
        print("\n" + "="*80)
        print("📈 快速极限并发测试结果汇总")
        print("="*80)
        
        if not self.results:
            print("\n⚠️  没有测试结果")
            return
        
        # 按极限并发数排序
        sorted_results = sorted(self.results, key=lambda x: x["max_concurrency"], reverse=True)
        
        print(f"\n各接口极限并发能力 (成功率>=90%):\n")
        print(f"{'接口名称':<30} {'极限并发':<15} {'URL'}")
        print("-" * 100)
        
        for result in sorted_results:
            print(f"{result['interface']:<30} {result['max_concurrency']:<15} {result['url'][:50]}")
        
        # 找出最高和最低
        if sorted_results:
            highest = sorted_results[0]
            lowest = sorted_results[-1]
            
            print(f"\n🏆 最高并发能力: {highest['interface']} - {highest['max_concurrency']} 并发")
            print(f"⚠️  最低并发能力: {lowest['interface']} - {lowest['max_concurrency']} 并发")
        
        # 保存结果到JSON
        output = {
            "test_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "test_strategy": "从100开始，每次增加100并发，找出成功率>=90%的最高并发数（精确到百位）",
            "results": sorted_results
        }
        
        with open("quick_max_concurrency_results.json", "w", encoding="utf-8") as f:
            json.dump(output, f, ensure_ascii=False, indent=2)
        
        print(f"\n✅ 测试结果已保存到: quick_max_concurrency_results.json")
        print("="*80)

async def main():
    """主函数"""
    tester = QuickMaxConcurrencyTester()
    
    try:
        await tester.setup()
        await tester.run_all_tests()
        tester.generate_summary()
    finally:
        await tester.teardown()

if __name__ == "__main__":
    asyncio.run(main())
