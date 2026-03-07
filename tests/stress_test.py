"""
极限并发压力测试 - 找到系统性能极限
逐步提高并发数，测试系统在不同负载下的表现
"""

import asyncio
import aiohttp
import time
import json
from datetime import datetime
import statistics

# API 配置
API_GATEWAY = "http://localhost:8080"

class StressTester:
    """压力测试器"""
    
    def __init__(self):
        self.results = {
            "test_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "stress_tests": []
        }
        self.session = None
    
    async def setup(self):
        """初始化"""
        # 增加连接池大小以支持高并发
        connector = aiohttp.TCPConnector(limit=2000, limit_per_host=2000)
        timeout = aiohttp.ClientTimeout(total=30)
        self.session = aiohttp.ClientSession(connector=connector, timeout=timeout)
    
    async def teardown(self):
        """清理"""
        if self.session:
            await self.session.close()
    
    async def _single_request(self, url: str):
        """单个请求"""
        start = time.time()
        try:
            async with self.session.get(url) as response:
                await response.text()
                duration = (time.time() - start) * 1000
                return {
                    "success": response.status == 200,
                    "duration_ms": duration,
                    "status": response.status
                }
        except asyncio.TimeoutError:
            duration = (time.time() - start) * 1000
            return {
                "success": False,
                "duration_ms": duration,
                "error": "timeout"
            }
        except Exception as e:
            duration = (time.time() - start) * 1000
            return {
                "success": False,
                "duration_ms": duration,
                "error": str(e)[:50]
            }
    
    async def test_concurrent_load(self, url: str, concurrent_users: int, test_name: str):
        """测试指定并发数的负载"""
        print(f"\n{'='*80}")
        print(f"🔥 测试并发: {concurrent_users} 用户")
        print(f"{'='*80}")
        
        start_time = time.time()
        
        # 创建并发任务
        tasks = [self._single_request(url) for _ in range(concurrent_users)]
        
        # 执行所有任务
        results = await asyncio.gather(*tasks)
        
        total_duration = time.time() - start_time
        
        # 统计结果
        success_count = sum(1 for r in results if r["success"])
        failed_count = concurrent_users - success_count
        success_rate = (success_count / concurrent_users) * 100
        
        # 计算响应时间统计
        durations = [r["duration_ms"] for r in results]
        avg_response = statistics.mean(durations)
        min_response = min(durations)
        max_response = max(durations)
        median_response = statistics.median(durations)
        
        # 计算百分位数
        sorted_durations = sorted(durations)
        p95_index = int(len(sorted_durations) * 0.95)
        p99_index = int(len(sorted_durations) * 0.99)
        p95_response = sorted_durations[p95_index] if p95_index < len(sorted_durations) else max_response
        p99_response = sorted_durations[p99_index] if p99_index < len(sorted_durations) else max_response
        
        # QPS (每秒请求数)
        qps = concurrent_users / total_duration if total_duration > 0 else 0
        
        # 错误统计
        errors = {}
        for r in results:
            if not r["success"]:
                error_type = r.get("error", "unknown")
                errors[error_type] = errors.get(error_type, 0) + 1
        
        result = {
            "test_name": test_name,
            "concurrent_users": concurrent_users,
            "total_duration_s": round(total_duration, 2),
            "success_count": success_count,
            "failed_count": failed_count,
            "success_rate": round(success_rate, 2),
            "qps": round(qps, 2),
            "avg_response_ms": round(avg_response, 2),
            "min_response_ms": round(min_response, 2),
            "max_response_ms": round(max_response, 2),
            "median_response_ms": round(median_response, 2),
            "p95_response_ms": round(p95_response, 2),
            "p99_response_ms": round(p99_response, 2),
            "errors": errors
        }
        
        self.results["stress_tests"].append(result)
        
        # 打印结果
        print(f"\n📊 测试结果:")
        print(f"   总耗时: {total_duration:.2f}s")
        print(f"   成功: {success_count} | 失败: {failed_count} | 成功率: {success_rate:.2f}%")
        print(f"   QPS: {qps:.2f} 请求/秒")
        print(f"\n⏱️  响应时间:")
        print(f"   平均: {avg_response:.2f}ms")
        print(f"   最小: {min_response:.2f}ms")
        print(f"   最大: {max_response:.2f}ms")
        print(f"   中位数: {median_response:.2f}ms")
        print(f"   P95: {p95_response:.2f}ms")
        print(f"   P99: {p99_response:.2f}ms")
        
        if errors:
            print(f"\n❌ 错误统计:")
            for error_type, count in errors.items():
                print(f"   {error_type}: {count} 次")
        
        # 判断系统状态
        if success_rate >= 99:
            status = "✅ 优秀"
        elif success_rate >= 95:
            status = "🟢 良好"
        elif success_rate >= 90:
            status = "🟡 一般"
        elif success_rate >= 80:
            status = "🟠 较差"
        else:
            status = "🔴 极差"
        
        print(f"\n系统状态: {status}")
        
        return result
    
    async def run_stress_test(self):
        """运行完整的压力测试"""
        print("=" * 80)
        print("🚀 SpeakMaster 极限并发压力测试")
        print("=" * 80)
        print(f"测试时间: {self.results['test_time']}")
        print("=" * 80)
        
        await self.setup()
        
        try:
            # 测试目标列表 - 覆盖多个服务
            test_targets = [
                {
                    "url": f"{API_GATEWAY}/actuator/health",
                    "name": "API Gateway 健康检查"
                },
                {
                    "url": f"{API_GATEWAY}/user/practice/roles",
                    "name": "Practice Service - 角色列表"
                },
                {
                    "url": "http://localhost:8089/ai/health",
                    "name": "AI Service 健康检查"
                }
            ]
            
            # 逐步提高并发数
            concurrent_levels = [
                10, 50, 100, 200, 500, 
                1000, 1500, 2000, 3000, 5000
            ]
            
            print(f"\n📋 测试计划:")
            print(f"   测试接口数: {len(test_targets)}")
            for target in test_targets:
                print(f"   - {target['name']}: {target['url']}")
            print(f"   并发级别: {concurrent_levels}")
            print(f"\n开始测试...\n")
            
            # 对每个接口进行测试
            for target in test_targets:
                print(f"\n{'#'*80}")
                print(f"# 测试接口: {target['name']}")
                print(f"{'#'*80}")
                
                for concurrent in concurrent_levels:
                    result = await self.test_concurrent_load(
                        target['url'], 
                        concurrent, 
                        target['name']
                    )
                    
                    # 如果成功率低于80%，停止当前接口测试
                    if result["success_rate"] < 80:
                        print(f"\n⚠️  警告: 成功率低于80%，已达到此接口极限！")
                        print(f"   {target['name']} 极限并发数: ~{concurrent} 用户")
                        break
                    
                    # 短暂休息，让系统恢复
                    print(f"\n⏸️  休息 2 秒...")
                    await asyncio.sleep(2)
                
                # 接口之间休息更长时间
                if target != test_targets[-1]:
                    print(f"\n⏸️  切换接口，休息 5 秒...")
                    await asyncio.sleep(5)
            
        finally:
            await self.teardown()
        
        # 生成总结报告
        self.generate_summary()
    
    def generate_summary(self):
        """生成总结报告"""
        print("\n" + "=" * 80)
        print("📈 压力测试总结报告")
        print("=" * 80)
        
        if not self.results["stress_tests"]:
            print("\n⚠️  没有测试数据")
            return
        
        # 按接口分组统计
        api_groups = {}
        for test in self.results["stress_tests"]:
            api_name = test["test_name"]
            if api_name not in api_groups:
                api_groups[api_name] = []
            api_groups[api_name].append(test)
        
        # 为每个接口生成报告
        for api_name, tests in api_groups.items():
            print(f"\n{'='*80}")
            print(f"📊 接口: {api_name}")
            print(f"{'='*80}")
            
            # 找到最佳性能点
            best_qps = max(tests, key=lambda x: x["qps"])
            best_success_rate = max(tests, key=lambda x: x["success_rate"])
            
            print(f"\n🏆 最佳性能指标:")
            print(f"   最高QPS: {best_qps['qps']} 请求/秒 (并发: {best_qps['concurrent_users']})")
            print(f"   最高成功率: {best_success_rate['success_rate']}% (并发: {best_success_rate['concurrent_users']})")
            
            # 找到系统极限
            successful_tests = [t for t in tests if t["success_rate"] >= 95]
            if successful_tests:
                max_concurrent = max(successful_tests, key=lambda x: x["concurrent_users"])
                print(f"\n💪 承载能力:")
                print(f"   在 {max_concurrent['concurrent_users']} 并发下保持 {max_concurrent['success_rate']}% 成功率")
                print(f"   平均响应: {max_concurrent['avg_response_ms']}ms")
                print(f"   P95响应: {max_concurrent['p95_response_ms']}ms")
                print(f"   P99响应: {max_concurrent['p99_response_ms']}ms")
            
            # 性能趋势
            print(f"\n📊 性能趋势:")
            print(f"{'并发数':<10} {'成功率':<10} {'QPS':<12} {'平均响应':<12} {'P95':<12} {'P99':<12}")
            print("-" * 80)
            for test in tests:
                print(f"{test['concurrent_users']:<10} "
                      f"{test['success_rate']:<10.2f}% "
                      f"{test['qps']:<12.2f} "
                      f"{test['avg_response_ms']:<12.2f}ms "
                      f"{test['p95_response_ms']:<12.2f}ms "
                      f"{test['p99_response_ms']:<12.2f}ms")
        
        # 全局统计
        print(f"\n{'='*80}")
        print(f"🌐 全局统计")
        print(f"{'='*80}")
        
        all_qps = [t["qps"] for t in self.results["stress_tests"]]
        all_success_rates = [t["success_rate"] for t in self.results["stress_tests"]]
        
        print(f"\n   总测试次数: {len(self.results['stress_tests'])}")
        print(f"   测试接口数: {len(api_groups)}")
        print(f"   最高QPS: {max(all_qps):.2f} 请求/秒")
        print(f"   平均成功率: {statistics.mean(all_success_rates):.2f}%")
        
        # 保存结果
        with open("stress_test_results.json", "w", encoding="utf-8") as f:
            json.dump(self.results, f, ensure_ascii=False, indent=2)
        
        print(f"\n✅ 测试结果已保存到: stress_test_results.json")
        print("=" * 80)


async def main():
    """主函数"""
    tester = StressTester()
    
    print("\n⚠️  注意: 此测试将对系统施加高负载")
    print("请确保:")
    print("  1. 系统正在运行")
    print("  2. 没有其他重要任务在执行")
    print("  3. 准备好监控系统资源使用情况")
    print("\n按 Ctrl+C 取消，或等待 5 秒后开始测试...")
    
    try:
        await asyncio.sleep(5)
    except KeyboardInterrupt:
        print("\n\n测试已取消")
        return
    
    await tester.run_stress_test()


if __name__ == "__main__":
    asyncio.run(main())
