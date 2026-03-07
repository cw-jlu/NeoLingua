"""
真实 API 测试脚本 - 挑战杯版
测试实际运行的系统API，生成真实的性能指标
"""

import asyncio
import aiohttp
import time
import json
from datetime import datetime
from typing import List, Dict
import statistics

# API 配置
API_GATEWAY = "http://localhost:8080"
AI_SERVICE = "http://localhost:8089"

class RealAPITester:
    """真实API测试器"""
    
    def __init__(self):
        self.results = {
            "test_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "api_tests": [],
            "performance_summary": {}
        }
        self.session = None
    
    async def setup(self):
        """初始化"""
        self.session = aiohttp.ClientSession()
        print("🚀 初始化测试环境...")
    
    async def teardown(self):
        """清理"""
        if self.session:
            await self.session.close()
        print("🧹 清理测试环境...")
    
    async def test_api(self, name: str, url: str, method: str = "GET", 
                      data: Dict = None, repeat: int = 10):
        """测试单个API"""
        print(f"\n📊 测试 {name} (重复{repeat}次)...")
        
        response_times = []
        success_count = 0
        
        for i in range(repeat):
            start = time.time()
            try:
                if method == "GET":
                    async with self.session.get(url, timeout=aiohttp.ClientTimeout(total=10)) as response:
                        await response.text()
                        success = response.status == 200
                elif method == "POST":
                    async with self.session.post(url, json=data, timeout=aiohttp.ClientTimeout(total=10)) as response:
                        await response.text()
                        success = response.status == 200
                
                duration = (time.time() - start) * 1000  # 转换为毫秒
                response_times.append(duration)
                
                if success:
                    success_count += 1
                
                if (i + 1) % 5 == 0:
                    print(f"  ✓ 完成 {i + 1}/{repeat} 次")
            
            except Exception as e:
                duration = (time.time() - start) * 1000
                response_times.append(duration)
                print(f"  ✗ 请求失败: {str(e)[:50]}")
        
        # 计算统计数据
        if response_times:
            avg_time = statistics.mean(response_times)
            min_time = min(response_times)
            max_time = max(response_times)
            success_rate = (success_count / repeat) * 100
            
            result = {
                "name": name,
                "url": url,
                "method": method,
                "repeat": repeat,
                "avg_response_ms": round(avg_time, 2),
                "min_response_ms": round(min_time, 2),
                "max_response_ms": round(max_time, 2),
                "success_count": success_count,
                "success_rate": round(success_rate, 2)
            }
            
            self.results["api_tests"].append(result)
            
            print(f"  ✅ 平均响应: {avg_time:.2f}ms, 成功率: {success_rate:.2f}%")
            
            return result
        
        return None
    
    async def test_concurrent(self, name: str, url: str, concurrent_users: int):
        """测试并发性能"""
        print(f"\n👥 测试并发性能: {name} ({concurrent_users}并发)...")
        
        start = time.time()
        tasks = []
        
        for _ in range(concurrent_users):
            task = self._single_request(url)
            tasks.append(task)
        
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        duration = time.time() - start
        success_count = sum(1 for r in results if not isinstance(r, Exception) and r)
        success_rate = (success_count / concurrent_users) * 100
        avg_response = (duration / concurrent_users) * 1000
        
        print(f"  ✅ 总耗时: {duration:.2f}s, 平均响应: {avg_response:.2f}ms, 成功率: {success_rate:.2f}%")
        
        return {
            "name": name,
            "concurrent_users": concurrent_users,
            "total_duration_s": round(duration, 2),
            "avg_response_ms": round(avg_response, 2),
            "success_rate": round(success_rate, 2)
        }
    
    async def _single_request(self, url: str) -> bool:
        """单个请求"""
        try:
            async with self.session.get(url, timeout=aiohttp.ClientTimeout(total=10)) as response:
                await response.text()
                return response.status == 200
        except:
            return False
    
    async def run_full_test(self):
        """运行完整测试"""
        print("=" * 80)
        print("🎯 SpeakMaster 真实API性能测试 - 挑战杯版")
        print("=" * 80)
        print(f"测试时间: {self.results['test_time']}")
        print("=" * 80)
        
        await self.setup()
        
        try:
            # 1. 测试 API Gateway 健康检查
            await self.test_api(
                "API Gateway 健康检查",
                f"{API_GATEWAY}/actuator/health",
                repeat=20
            )
            
            # 2. 测试 Practice Service - 获取主题列表
            await self.test_api(
                "Practice Service - 主题列表",
                f"{API_GATEWAY}/user/practice/themes",
                repeat=20
            )
            
            # 3. 测试 Practice Service - 获取角色列表
            await self.test_api(
                "Practice Service - 角色列表",
                f"{API_GATEWAY}/user/practice/roles",
                repeat=20
            )
            
            # 4. 测试 AI Service 健康检查
            await self.test_api(
                "AI Service 健康检查",
                f"{AI_SERVICE}/ai/health",
                repeat=20
            )
            
            # 5. 并发测试
            print("\n" + "=" * 80)
            print("🔥 并发性能测试")
            print("=" * 80)
            
            concurrent_results = []
            for users in [10, 50, 100]:
                result = await self.test_concurrent(
                    f"API Gateway ({users}并发)",
                    f"{API_GATEWAY}/actuator/health",
                    users
                )
                concurrent_results.append(result)
            
            self.results["concurrent_tests"] = concurrent_results
            
        finally:
            await self.teardown()
        
        # 生成总结
        self.generate_summary()
    
    def generate_summary(self):
        """生成测试总结"""
        print("\n" + "=" * 80)
        print("📈 测试总结报告")
        print("=" * 80)
        
        if self.results["api_tests"]:
            all_avg_times = [t["avg_response_ms"] for t in self.results["api_tests"]]
            all_success_rates = [t["success_rate"] for t in self.results["api_tests"]]
            
            summary = {
                "总测试接口数": len(self.results["api_tests"]),
                "平均响应时间": f"{statistics.mean(all_avg_times):.2f}ms",
                "最快响应": f"{min(all_avg_times):.2f}ms",
                "最慢响应": f"{max(all_avg_times):.2f}ms",
                "平均成功率": f"{statistics.mean(all_success_rates):.2f}%"
            }
            
            self.results["performance_summary"] = summary
            
            print("\n核心指标:")
            for key, value in summary.items():
                print(f"  {key:15s}: {value}")
        
        # 保存结果
        with open("real_api_test_results.json", "w", encoding="utf-8") as f:
            json.dump(self.results, f, ensure_ascii=False, indent=2)
        
        print("\n✅ 测试结果已保存到: real_api_test_results.json")
        print("=" * 80)
        
        # 打印详细结果
        print("\n📊 详细测试结果:")
        for test in self.results["api_tests"]:
            print(f"\n  {test['name']}:")
            print(f"    平均响应: {test['avg_response_ms']}ms")
            print(f"    最快: {test['min_response_ms']}ms")
            print(f"    最慢: {test['max_response_ms']}ms")
            print(f"    成功率: {test['success_rate']}%")
        
        if "concurrent_tests" in self.results:
            print("\n👥 并发测试结果:")
            for test in self.results["concurrent_tests"]:
                print(f"\n  {test['name']}:")
                print(f"    并发用户: {test['concurrent_users']}")
                print(f"    平均响应: {test['avg_response_ms']}ms")
                print(f"    成功率: {test['success_rate']}%")


async def main():
    """主函数"""
    tester = RealAPITester()
    await tester.run_full_test()


if __name__ == "__main__":
    print("\n⚠️  注意: 此脚本需要系统正在运行")
    print("请确保以下服务已启动:")
    print("  - API Gateway (8080)")
    print("  - Practice Service (8082)")
    print("  - AI Service (8089)")
    print("\n按 Ctrl+C 取消，或等待 3 秒后开始测试...")
    
    try:
        time.sleep(3)
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n\n❌ 测试已取消")
