"""
快速并发测试 - 仅测试新增接口的并发能力
"""

import asyncio
import aiohttp
import time
import statistics

# API 配置
API_GATEWAY = "http://localhost:8080"
AI_SERVICE = "http://localhost:8089"
ANALYSIS_SERVICE = "http://localhost:8085"

async def test_concurrent_api(name, url, method, concurrency, data=None):
    """测试单个API的并发性能"""
    print(f"\n测试 {name} (并发数: {concurrency})...")
    
    timeout = aiohttp.ClientTimeout(total=30)
    async with aiohttp.ClientSession(timeout=timeout) as session:
        start_time = time.time()
        
        async def make_request():
            try:
                if method == "GET":
                    async with session.get(url) as response:
                        return response.status == 200
                else:
                    async with session.post(url, json=data) as response:
                        return response.status == 200
            except:
                return False
        
        tasks = [make_request() for _ in range(concurrency)]
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        duration = time.time() - start_time
        successful = sum(1 for r in results if r is True)
        qps = concurrency / duration if duration > 0 else 0
        
        print(f"  ✅ 成功: {successful}/{concurrency}")
        print(f"  ⏱️  耗时: {duration:.2f}秒")
        print(f"  📈 QPS: {qps:.2f}")
        
        return {
            "name": name,
            "concurrency": concurrency,
            "successful": successful,
            "total": concurrency,
            "qps": qps,
            "duration": duration
        }

async def main():
    """主测试函数"""
    print("="*80)
    print("🚀 快速并发测试 - 新增接口")
    print("="*80)
    
    # 测试配置
    tests = [
        ("API Gateway健康检查", f"{API_GATEWAY}/actuator/health", "GET", None),
        ("AI Service模型列表", f"{AI_SERVICE}/ai/models", "GET", None),
        ("Analysis Service健康检查", f"{ANALYSIS_SERVICE}/health", "GET", None),
        ("补全帮助", f"{AI_SERVICE}/advanced/completion/help?user_id=test&session_id=test", "POST", 
         {"incomplete_text": "I want to", "user_profile": {"level": "intermediate"}}),
    ]
    
    # 测试不同并发级别
    concurrency_levels = [10, 50, 100]
    
    all_results = []
    
    for concurrency in concurrency_levels:
        print(f"\n{'='*80}")
        print(f"并发级别: {concurrency}")
        print(f"{'='*80}")
        
        for name, url, method, data in tests:
            result = await test_concurrent_api(name, url, method, concurrency, data)
            all_results.append(result)
            await asyncio.sleep(1)  # 避免压垮服务器
    
    # 打印总结
    print(f"\n{'='*80}")
    print("📊 测试总结")
    print(f"{'='*80}")
    
    # 按接口分组
    by_interface = {}
    for result in all_results:
        name = result["name"]
        if name not in by_interface:
            by_interface[name] = []
        by_interface[name].append(result)
    
    for name, results in by_interface.items():
        print(f"\n{name}:")
        for r in results:
            success_rate = (r["successful"] / r["total"] * 100) if r["total"] > 0 else 0
            print(f"  并发{r['concurrency']}: QPS={r['qps']:.2f}, 成功率={success_rate:.1f}%")
    
    # 找出最高QPS
    max_qps_result = max(all_results, key=lambda x: x["qps"])
    print(f"\n🏆 最高QPS: {max_qps_result['qps']:.2f} ({max_qps_result['name']}, 并发{max_qps_result['concurrency']})")

if __name__ == "__main__":
    asyncio.run(main())
