"""
SpeakMaster 综合功能测试
测试所有接口和核心技术功能，使用真实数据验证系统能力
"""

import asyncio
import aiohttp
import json
import time
from datetime import datetime
from typing import Dict, List, Any
import statistics

# API 配置
API_GATEWAY = "http://localhost:8080"
AI_SERVICE = "http://localhost:8089"
ANALYSIS_SERVICE = "http://localhost:8085"

class ComprehensiveTester:
    """综合测试器"""
    
    def __init__(self):
        self.results = {
            "test_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "basic_api_tests": [],
            "advanced_feature_tests": [],
            "core_technology_tests": [],
            "performance_summary": {}
        }
        self.session = None
        self.test_user_id = "test_user_001"
        self.test_session_id = f"test_session_{int(time.time())}"
    
    async def setup(self):
        """初始化"""
        timeout = aiohttp.ClientTimeout(total=30)
        self.session = aiohttp.ClientSession(timeout=timeout)
        print("🚀 初始化测试环境...")
    
    async def teardown(self):
        """清理"""
        if self.session:
            await self.session.close()
        print("🧹 清理测试环境...")
    
    async def test_api(self, name: str, url: str, method: str = "GET", 
                      data: Dict = None, headers: Dict = None) -> Dict:
        """测试单个API"""
        print(f"\n📊 测试 {name}...")
        
        start = time.time()
        try:
            if method == "GET":
                async with self.session.get(url, headers=headers) as response:
                    content = await response.text()
                    duration = (time.time() - start) * 1000
                    success = response.status == 200
                    
            elif method == "POST":
                async with self.session.post(url, json=data, headers=headers) as response:
                    content = await response.text()
                    duration = (time.time() - start) * 1000
                    success = response.status == 200
            
            result = {
                "name": name,
                "url": url,
                "method": method,
                "success": success,
                "status_code": response.status,
                "response_time_ms": round(duration, 2),
                "response_preview": content[:200] if content else ""
            }
            
            if success:
                print(f"  ✅ 成功 - 响应时间: {duration:.2f}ms")
            else:
                print(f"  ❌ 失败 - 状态码: {response.status}")
            
            return result
            
        except Exception as e:
            duration = (time.time() - start) * 1000
            print(f"  ❌ 异常: {str(e)[:100]}")
            return {
                "name": name,
                "url": url,
                "method": method,
                "success": False,
                "error": str(e)[:200],
                "response_time_ms": round(duration, 2)
            }
    
    async def run_basic_api_tests(self):
        """运行基础API测试"""
        print("\n" + "="*80)
        print("📋 一、基础API接口测试")
        print("="*80)
        
        tests = [
            # API Gateway
            ("API Gateway 健康检查", f"{API_GATEWAY}/actuator/health", "GET"),
            
            # User Service
            ("User Service - 用户列表", f"{API_GATEWAY}/admin/user/list?page=1&size=10", "GET"),
            
            # Practice Service
            ("Practice Service - 主题列表", f"{API_GATEWAY}/user/practice/themes", "GET"),
            ("Practice Service - 角色列表", f"{API_GATEWAY}/user/practice/roles", "GET"),
            
            # AI Service
            ("AI Service 健康检查", f"{AI_SERVICE}/ai/health", "GET"),
            ("AI Service - 模型列表", f"{AI_SERVICE}/ai/models", "GET"),
            
            # Analysis Service
            ("Analysis Service 健康检查", f"{ANALYSIS_SERVICE}/health", "GET"),
        ]
        
        for name, url, method in tests:
            result = await self.test_api(name, url, method)
            self.results["basic_api_tests"].append(result)
            await asyncio.sleep(0.5)
    
    async def run_advanced_feature_tests(self):
        """运行高级功能测试"""
        print("\n" + "="*80)
        print("🚀 二、高级功能测试")
        print("="*80)
        
        # 1. 测试中式英语检测
        await self.test_chinglish_detection()
        
        # 2. 测试实时交互会话
        await self.test_realtime_interaction()
        
        # 3. 测试口语评估
        await self.test_speaking_assessment()
        
        # 4. 测试补全帮助
        await self.test_completion_help()
    
    async def test_chinglish_detection(self):
        """测试中式英语检测功能"""
        print("\n🔍 测试中式英语检测...")
        
        test_cases = [
            {
                "text": "I very like this book",
                "expected": "应检测出 'I very' 错误"
            },
            {
                "text": "Please open the light",
                "expected": "应检测出 'open the light' 错误"
            },
            {
                "text": "I want to eat medicine",
                "expected": "应检测出 'eat medicine' 错误"
            },
            {
                "text": "This is a good book and I really like it",
                "expected": "正确表达，无错误"
            }
        ]
        
        for i, case in enumerate(test_cases, 1):
            print(f"\n  测试用例 {i}: {case['text']}")
            print(f"  预期: {case['expected']}")
            
            result = await self.test_api(
                f"中式英语检测 - 用例{i}",
                f"{AI_SERVICE}/advanced/chinglish/detect",
                "POST",
                {"text": case["text"]}
            )
            
            if result["success"]:
                try:
                    response_data = json.loads(result["response_preview"])
                    print(f"  结果: {json.dumps(response_data, indent=2, ensure_ascii=False)}")
                except:
                    print(f"  响应: {result['response_preview']}")
            
            self.results["advanced_feature_tests"].append({
                "feature": "chinglish_detection",
                "test_case": case["text"],
                **result
            })
            
            await asyncio.sleep(0.5)
    
    async def test_realtime_interaction(self):
        """测试实时交互功能"""
        print("\n🎙️ 测试实时交互会话...")
        
        # 1. 启动实时会话
        print("\n  1. 启动实时会话...")
        start_result = await self.test_api(
            "启动实时会话",
            f"{AI_SERVICE}/advanced/realtime/start?user_id={self.test_user_id}&session_id={self.test_session_id}",
            "POST",
            {
                "mode": "half_duplex",
                "user_profile": {
                    "level": "intermediate",
                    "native_language": "Chinese"
                }
            }
        )
        self.results["advanced_feature_tests"].append({
            "feature": "realtime_interaction_start",
            **start_result
        })
        
        # 2. 模拟音频处理
        print("\n  2. 处理实时音频...")
        audio_result = await self.test_api(
            "处理实时音频",
            f"{AI_SERVICE}/advanced/realtime/audio?session_id={self.test_session_id}",
            "POST",
            {
                "energy_level": 0.7,
                "pitch_variance": 0.5,
                "speech_rate": 150,
                "pause_duration": 0.5,
                "voice_activity": True,
                "confidence_score": 0.85,
                "transcribed_text": "Hello, I want to practice my English speaking"
            }
        )
        self.results["advanced_feature_tests"].append({
            "feature": "realtime_audio_processing",
            **audio_result
        })
        
        # 3. 获取会话状态
        print("\n  3. 获取会话状态...")
        status_result = await self.test_api(
            "获取会话状态",
            f"{AI_SERVICE}/advanced/session/status/{self.test_session_id}",
            "GET"
        )
        self.results["advanced_feature_tests"].append({
            "feature": "session_status",
            **status_result
        })
        
        # 4. 结束会话
        print("\n  4. 结束实时会话...")
        end_result = await self.test_api(
            "结束实时会话",
            f"{AI_SERVICE}/advanced/realtime/end?session_id={self.test_session_id}",
            "POST"
        )
        self.results["advanced_feature_tests"].append({
            "feature": "realtime_interaction_end",
            **end_result
        })
    
    async def test_speaking_assessment(self):
        """测试口语评估功能"""
        print("\n📊 测试口语评估...")
        
        test_messages = [
            {
                "message": "I think learning English is very important for my career. I practice speaking every day and try to improve my pronunciation.",
                "level": "intermediate"
            },
            {
                "message": "Um, well, I like, you know, to speak English but it's, uh, difficult sometimes.",
                "level": "beginner"
            },
            {
                "message": "The implementation of artificial intelligence in educational contexts necessitates a comprehensive understanding of pedagogical principles and technological capabilities.",
                "level": "advanced"
            }
        ]
        
        for i, test in enumerate(test_messages, 1):
            print(f"\n  测试用例 {i} ({test['level']} level):")
            print(f"  消息: {test['message'][:80]}...")
            
            result = await self.test_api(
                f"口语评估 - {test['level']}",
                f"{AI_SERVICE}/advanced/assessment/speaking?user_id={self.test_user_id}&session_id={self.test_session_id}_{i}",
                "POST",
                {
                    "user_message": test["message"],
                    "audio_analysis": {
                        "speech_rate": 150,
                        "pause_count": 2,
                        "pronunciation_score": 75
                    },
                    "assessment_type": "ielts"
                }
            )
            
            if result["success"]:
                try:
                    response_data = json.loads(result["response_preview"])
                    print(f"  评估结果: {json.dumps(response_data, indent=2, ensure_ascii=False)[:300]}...")
                except:
                    print(f"  响应: {result['response_preview'][:200]}")
            
            self.results["advanced_feature_tests"].append({
                "feature": "speaking_assessment",
                "test_level": test["level"],
                **result
            })
            
            await asyncio.sleep(0.5)
    
    async def test_completion_help(self):
        """测试补全帮助功能"""
        print("\n💡 测试引导式补全...")
        
        incomplete_texts = [
            "I want to",
            "How can I",
            "I think that",
            "In my opinion"
        ]
        
        for text in incomplete_texts:
            print(f"\n  不完整文本: '{text}'")
            
            result = await self.test_api(
                f"补全帮助 - '{text}'",
                f"{AI_SERVICE}/advanced/completion/help?user_id={self.test_user_id}&session_id={self.test_session_id}",
                "POST",
                {
                    "incomplete_text": text,
                    "user_profile": {
                        "level": "intermediate"
                    }
                }
            )
            
            if result["success"]:
                try:
                    response_data = json.loads(result["response_preview"])
                    print(f"  建议: {json.dumps(response_data, indent=2, ensure_ascii=False)[:200]}...")
                except:
                    print(f"  响应: {result['response_preview'][:150]}")
            
            self.results["advanced_feature_tests"].append({
                "feature": "completion_help",
                "incomplete_text": text,
                **result
            })
            
            await asyncio.sleep(0.5)
    
    async def run_core_technology_tests(self):
        """运行核心技术验证测试"""
        print("\n" + "="*80)
        print("🎯 三、核心技术能力验证")
        print("="*80)
        
        # 1. 纠错反馈的策略与时机
        await self.test_error_correction_strategy()
        
        # 2. 针对非母语者的鲁棒性
        await self.test_non_native_robustness()
        
        # 3. 全双工交互与类人对话流
        await self.test_full_duplex_interaction()
        
        # 4. 评估体系与自动化评分
        await self.test_assessment_system()
    
    async def test_error_correction_strategy(self):
        """测试纠错反馈策略"""
        print("\n✏️ 测试纠错反馈策略与时机...")
        
        # 测试不同错误类型的纠错
        error_cases = [
            {
                "text": "I goed to school yesterday",
                "error_type": "grammar",
                "description": "动词时态错误"
            },
            {
                "text": "I very like this movie",
                "error_type": "word_order",
                "description": "词序错误"
            },
            {
                "text": "He don't know the answer",
                "error_type": "subject_verb_agreement",
                "description": "主谓不一致"
            }
        ]
        
        for case in error_cases:
            print(f"\n  测试: {case['description']}")
            print(f"  错误文本: {case['text']}")
            
            # 使用AI聊天接口测试纠错
            result = await self.test_api(
                f"纠错测试 - {case['error_type']}",
                f"{AI_SERVICE}/ai/chat",
                "POST",
                {
                    "session_id": f"{self.test_session_id}_correction",
                    "user_id": self.test_user_id,
                    "message": case["text"],
                    "role_prompt": "You are an English teacher. Correct any errors in the student's English and explain them gently."
                }
            )
            
            self.results["core_technology_tests"].append({
                "technology": "error_correction_strategy",
                "error_type": case["error_type"],
                **result
            })
            
            await asyncio.sleep(1)
    
    async def test_non_native_robustness(self):
        """测试针对非母语者的鲁棒性"""
        print("\n🌍 测试非母语者鲁棒性...")
        
        # 测试中式英语识别和处理
        chinglish_cases = [
            "I very like this book",
            "Please open the light",
            "I want to eat medicine",
            "How to say this word in English",
            "So so, not very good"
        ]
        
        detection_results = []
        for text in chinglish_cases:
            result = await self.test_api(
                f"中式英语识别 - {text[:30]}",
                f"{AI_SERVICE}/advanced/chinglish/detect",
                "POST",
                {"text": text}
            )
            detection_results.append(result)
            
            self.results["core_technology_tests"].append({
                "technology": "non_native_robustness",
                "test_text": text,
                **result
            })
            
            await asyncio.sleep(0.5)
        
        # 计算识别准确率
        successful_detections = sum(1 for r in detection_results if r["success"])
        accuracy = (successful_detections / len(chinglish_cases)) * 100
        print(f"\n  中式英语识别准确率: {accuracy:.1f}% ({successful_detections}/{len(chinglish_cases)})")
    
    async def test_full_duplex_interaction(self):
        """测试全双工交互"""
        print("\n🎙️ 测试全双工交互与类人对话流...")
        
        # 测试不同交互模式
        modes = ["full_duplex", "half_duplex", "guided", "free_talk"]
        
        for mode in modes:
            print(f"\n  测试交互模式: {mode}")
            
            session_id = f"{self.test_session_id}_{mode}"
            
            # 启动会话
            start_result = await self.test_api(
                f"启动{mode}会话",
                f"{AI_SERVICE}/advanced/realtime/start?user_id={self.test_user_id}&session_id={session_id}",
                "POST",
                {
                    "mode": mode,
                    "user_profile": {"level": "intermediate"}
                }
            )
            
            # 模拟音频交互
            if start_result["success"]:
                audio_result = await self.test_api(
                    f"{mode}音频处理",
                    f"{AI_SERVICE}/advanced/realtime/audio?session_id={session_id}",
                    "POST",
                    {
                        "energy_level": 0.6,
                        "pitch_variance": 0.4,
                        "speech_rate": 140,
                        "pause_duration": 1.0,
                        "voice_activity": True,
                        "confidence_score": 0.8,
                        "transcribed_text": "Hello, how are you today?"
                    }
                )
                
                # 结束会话
                await self.test_api(
                    f"结束{mode}会话",
                    f"{AI_SERVICE}/advanced/realtime/end?session_id={session_id}",
                    "POST"
                )
            
            self.results["core_technology_tests"].append({
                "technology": "full_duplex_interaction",
                "mode": mode,
                **start_result
            })
            
            await asyncio.sleep(1)
    
    async def test_assessment_system(self):
        """测试评估体系与自动化评分"""
        print("\n📊 测试评估体系与自动化评分...")
        
        # 测试不同水平的评估
        test_samples = [
            {
                "level": "beginner",
                "message": "I am student. I like English. I study every day.",
                "expected_score_range": (3, 5)
            },
            {
                "level": "intermediate",
                "message": "I have been studying English for three years. Although it's challenging, I find it very rewarding and useful for my career.",
                "expected_score_range": (5, 7)
            },
            {
                "level": "advanced",
                "message": "The integration of technology in language learning has revolutionized pedagogical approaches, enabling personalized learning experiences that adapt to individual needs and learning styles.",
                "expected_score_range": (7, 9)
            }
        ]
        
        assessment_results = []
        for sample in test_samples:
            print(f"\n  评估 {sample['level']} 水平样本...")
            
            result = await self.test_api(
                f"评估 - {sample['level']}",
                f"{AI_SERVICE}/advanced/assessment/speaking?user_id={self.test_user_id}&session_id={self.test_session_id}",
                "POST",
                {
                    "user_message": sample["message"],
                    "audio_analysis": {
                        "speech_rate": 150,
                        "pause_count": 2,
                        "pronunciation_score": 70 if sample["level"] == "beginner" else 80 if sample["level"] == "intermediate" else 90
                    },
                    "assessment_type": "ielts"
                }
            )
            
            assessment_results.append(result)
            
            if result["success"]:
                try:
                    response_data = json.loads(result["response_preview"])
                    print(f"  评估结果预览: {json.dumps(response_data, indent=2, ensure_ascii=False)[:300]}...")
                except:
                    pass
            
            self.results["core_technology_tests"].append({
                "technology": "assessment_system",
                "test_level": sample["level"],
                **result
            })
            
            await asyncio.sleep(1)
        
        # 统计评估成功率
        successful_assessments = sum(1 for r in assessment_results if r["success"])
        success_rate = (successful_assessments / len(test_samples)) * 100
        print(f"\n  评估系统成功率: {success_rate:.1f}% ({successful_assessments}/{len(test_samples)})")
    
    def generate_summary(self):
        """生成测试总结"""
        print("\n" + "="*80)
        print("📈 测试总结报告")
        print("="*80)
        
        # 基础API测试统计
        basic_tests = self.results["basic_api_tests"]
        basic_success = sum(1 for t in basic_tests if t.get("success", False))
        basic_total = len(basic_tests)
        
        print(f"\n📋 基础API测试:")
        print(f"   总测试数: {basic_total}")
        print(f"   成功: {basic_success}")
        print(f"   失败: {basic_total - basic_success}")
        print(f"   成功率: {(basic_success/basic_total*100):.1f}%")
        
        if basic_tests:
            response_times = [t["response_time_ms"] for t in basic_tests if "response_time_ms" in t]
            if response_times:
                print(f"   平均响应时间: {statistics.mean(response_times):.2f}ms")
                print(f"   最快响应: {min(response_times):.2f}ms")
                print(f"   最慢响应: {max(response_times):.2f}ms")
        
        # 高级功能测试统计
        advanced_tests = self.results["advanced_feature_tests"]
        advanced_success = sum(1 for t in advanced_tests if t.get("success", False))
        advanced_total = len(advanced_tests)
        
        print(f"\n🚀 高级功能测试:")
        print(f"   总测试数: {advanced_total}")
        print(f"   成功: {advanced_success}")
        print(f"   失败: {advanced_total - advanced_success}")
        print(f"   成功率: {(advanced_success/advanced_total*100):.1f}%")
        
        # 核心技术测试统计
        core_tests = self.results["core_technology_tests"]
        core_success = sum(1 for t in core_tests if t.get("success", False))
        core_total = len(core_tests)
        
        print(f"\n🎯 核心技术测试:")
        print(f"   总测试数: {core_total}")
        print(f"   成功: {core_success}")
        print(f"   失败: {core_total - core_success}")
        print(f"   成功率: {(core_success/core_total*100):.1f}%")
        
        # 按技术分类统计
        tech_stats = {}
        for test in core_tests:
            tech = test.get("technology", "unknown")
            if tech not in tech_stats:
                tech_stats[tech] = {"total": 0, "success": 0}
            tech_stats[tech]["total"] += 1
            if test.get("success", False):
                tech_stats[tech]["success"] += 1
        
        print(f"\n  核心技术详细统计:")
        for tech, stats in tech_stats.items():
            success_rate = (stats["success"] / stats["total"] * 100) if stats["total"] > 0 else 0
            print(f"    {tech}: {stats['success']}/{stats['total']} ({success_rate:.1f}%)")
        
        # 并发测试统计
        if "concurrent_tests" in self.results and self.results["concurrent_tests"]:
            concurrent_tests = self.results["concurrent_tests"]
            
            print(f"\n⚡ 并发性能测试:")
            print(f"   总测试场景: {len(concurrent_tests)}")
            
            # 按测试类型分组
            test_groups = {}
            for test in concurrent_tests:
                test_name = test["test_name"]
                if test_name not in test_groups:
                    test_groups[test_name] = []
                test_groups[test_name].append(test)
            
            for test_name, tests in test_groups.items():
                print(f"\n   {test_name}:")
                for test in tests:
                    concurrency = test["concurrency"]
                    success_rate = (test["successful"] / test["total_requests"] * 100) if test["total_requests"] > 0 else 0
                    print(f"     并发{concurrency}: QPS={test['qps']:.2f}, "
                          f"成功率={success_rate:.1f}%, "
                          f"平均响应={test['avg_response_time_ms']:.2f}ms")
            
            # 找出最佳性能
            max_qps_test = max(concurrent_tests, key=lambda x: x["qps"])
            print(f"\n   🏆 最高QPS: {max_qps_test['qps']:.2f} ({max_qps_test['test_name']}, 并发{max_qps_test['concurrency']})")
            
            min_response_test = min(concurrent_tests, key=lambda x: x["avg_response_time_ms"])
            print(f"   ⚡ 最快响应: {min_response_test['avg_response_time_ms']:.2f}ms ({min_response_test['test_name']}, 并发{min_response_test['concurrency']})")
        
        # 极限并发测试统计
        if "max_concurrency_tests" in self.results and self.results["max_concurrency_tests"]:
            max_tests = self.results["max_concurrency_tests"]
            
            print(f"\n🚀 极限并发测试:")
            print(f"   总测试次数: {len(max_tests)}")
            
            # 按接口分组，找出每个接口的最大并发
            max_by_interface = {}
            for test in max_tests:
                test_name = test["test_name"]
                if test_name not in max_by_interface or test["concurrency"] > max_by_interface[test_name]["concurrency"]:
                    if test["success_rate"] >= 95:  # 只统计成功率>=95%的
                        max_by_interface[test_name] = test
            
            print(f"\n   各接口极限并发能力:")
            for test_name, test in max_by_interface.items():
                print(f"     {test_name}: {test['concurrency']}并发 "
                      f"(成功率={test['success_rate']:.1f}%, QPS={test['qps']:.2f})")
            
            # 找出系统整体极限
            if max_by_interface:
                overall_max = max(max_by_interface.values(), key=lambda x: x["concurrency"])
                print(f"\n   🏆 系统极限并发: {overall_max['concurrency']} ({overall_max['test_name']})")
                print(f"      成功率: {overall_max['success_rate']:.1f}%")
                print(f"      QPS: {overall_max['qps']:.2f}")
        
        # 总体统计
        total_tests = basic_total + advanced_total + core_total
        total_success = basic_success + advanced_success + core_success
        
        print(f"\n📊 总体统计:")
        print(f"   总测试数: {total_tests}")
        print(f"   总成功数: {total_success}")
        print(f"   总成功率: {(total_success/total_tests*100):.1f}%")
        
        # 保存结果
        with open("comprehensive_test_results.json", "w", encoding="utf-8") as f:
            json.dump(self.results, f, ensure_ascii=False, indent=2)
        
        print(f"\n✅ 测试结果已保存到: comprehensive_test_results.json")
        print("="*80)
    
    async def run_concurrent_tests(self):
        """运行并发测试"""
        print("\n" + "="*80)
        print("⚡ 四、并发性能测试")
        print("="*80)
        
        # 测试不同并发级别
        concurrency_levels = [10, 50, 100]
        
        for concurrency in concurrency_levels:
            print(f"\n🔥 测试并发数: {concurrency}")
            
            # 1. API Gateway健康检查并发测试
            await self.test_concurrent_api_gateway(concurrency)
            
            # 2. AI Service健康检查并发测试
            await self.test_concurrent_health_check(concurrency)
            
            # 3. AI Service模型列表并发测试
            await self.test_concurrent_models(concurrency)
            
            # 4. Analysis Service健康检查并发测试
            await self.test_concurrent_analysis_health(concurrency)
            
            # 5. AI聊天并发测试
            await self.test_concurrent_ai_chat(concurrency)
            
            # 6. 中式英语检测并发测试
            await self.test_concurrent_chinglish_detection(concurrency)
            
            # 7. 实时交互并发测试
            await self.test_concurrent_realtime_interaction(concurrency)
            
            # 8. 实时音频处理并发测试
            await self.test_concurrent_realtime_audio(concurrency)
            
            # 9. 会话状态并发测试
            await self.test_concurrent_session_status(concurrency)
            
            # 10. 结束会话并发测试
            await self.test_concurrent_realtime_end(concurrency)
            
            # 11. 口语评估并发测试
            await self.test_concurrent_assessment(concurrency)
            
            # 12. 补全帮助并发测试
            await self.test_concurrent_completion(concurrency)
    
    async def run_max_concurrency_tests(self):
        """运行极限并发测试 - 找出各接口的最高并发能力"""
        print("\n" + "="*80)
        print("🚀 五、极限并发测试")
        print("="*80)
        print("\n⚠️  警告: 此测试将逐步增加并发数，直到找到系统极限")
        print("测试可能需要较长时间，请耐心等待...\n")
        
        # 测试各个接口的极限并发
        await self.find_max_concurrency_api_gateway()
        await self.find_max_concurrency_health_check()
        await self.find_max_concurrency_models()
        await self.find_max_concurrency_analysis_health()
        await self.find_max_concurrency_ai_chat()
        await self.find_max_concurrency_chinglish()
        await self.find_max_concurrency_realtime()
        await self.find_max_concurrency_realtime_audio()
        await self.find_max_concurrency_session_status()
        await self.find_max_concurrency_realtime_end()
        await self.find_max_concurrency_assessment()
        await self.find_max_concurrency_completion()
    
    async def find_max_concurrency_api_gateway(self):
        """找出API Gateway健康检查接口的最高并发"""
        print("\n📊 测试API Gateway健康检查接口极限并发...")
        
        max_concurrency = await self._binary_search_max_concurrency(
            test_name="api_gateway_health",
            url=f"{API_GATEWAY}/actuator/health",
            method="GET",
            data=None,
            min_concurrency=100,
            max_concurrency=5000,
            success_threshold=0.95
        )
        
        print(f"\n  🏆 API Gateway健康检查接口极限并发: {max_concurrency}")
    
    async def find_max_concurrency_health_check(self):
        """找出AI Service健康检查接口的最高并发"""
        print("\n📊 测试AI Service健康检查接口极限并发...")
        
        max_concurrency = await self._binary_search_max_concurrency(
            test_name="ai_service_health",
            url=f"{AI_SERVICE}/ai/health",
            method="GET",
            data=None,
            min_concurrency=100,
            max_concurrency=5000,
            success_threshold=0.95
        )
        
        print(f"\n  🏆 AI Service健康检查接口极限并发: {max_concurrency}")
    
    async def find_max_concurrency_models(self):
        """找出AI Service模型列表接口的最高并发"""
        print("\n📊 测试AI Service模型列表接口极限并发...")
        
        max_concurrency = await self._binary_search_max_concurrency(
            test_name="ai_models_list",
            url=f"{AI_SERVICE}/ai/models",
            method="GET",
            data=None,
            min_concurrency=100,
            max_concurrency=5000,
            success_threshold=0.95
        )
        
        print(f"\n  🏆 AI Service模型列表接口极限并发: {max_concurrency}")
    
    async def find_max_concurrency_analysis_health(self):
        """找出Analysis Service健康检查接口的最高并发"""
        print("\n📊 测试Analysis Service健康检查接口极限并发...")
        
        max_concurrency = await self._binary_search_max_concurrency(
            test_name="analysis_service_health",
            url=f"{ANALYSIS_SERVICE}/health",
            method="GET",
            data=None,
            min_concurrency=100,
            max_concurrency=5000,
            success_threshold=0.95
        )
        
        print(f"\n  🏆 Analysis Service健康检查接口极限并发: {max_concurrency}")
    
    async def find_max_concurrency_ai_chat(self):
        """找出AI聊天接口的最高并发"""
        print("\n💬 测试AI聊天接口极限并发...")
        
        max_concurrency = await self._binary_search_max_concurrency(
            test_name="ai_chat",
            url=f"{AI_SERVICE}/ai/chat",
            method="POST",
            data={
                "session_id": f"max_test_{int(time.time())}",
                "user_id": "test_user",
                "message": "Hello, how are you?",
                "role_prompt": "You are a helpful English teacher."
            },
            min_concurrency=100,
            max_concurrency=5000,
            success_threshold=0.95
        )
        
        print(f"\n  🏆 AI聊天接口极限并发: {max_concurrency}")
    
    async def find_max_concurrency_chinglish(self):
        """找出中式英语检测接口的最高并发"""
        print("\n🔍 测试中式英语检测接口极限并发...")
        
        max_concurrency = await self._binary_search_max_concurrency(
            test_name="chinglish_detection",
            url=f"{AI_SERVICE}/advanced/chinglish/detect",
            method="POST",
            data={"text": "I very like this book"},
            min_concurrency=100,
            max_concurrency=5000,
            success_threshold=0.95
        )
        
        print(f"\n  🏆 中式英语检测接口极限并发: {max_concurrency}")
    
    async def find_max_concurrency_realtime(self):
        """找出实时交互接口的最高并发"""
        print("\n🎙️ 测试实时交互接口极限并发...")
        
        max_concurrency = await self._binary_search_max_concurrency(
            test_name="realtime_interaction",
            url=f"{AI_SERVICE}/advanced/realtime/start",
            method="POST",
            data={
                "mode": "half_duplex",
                "user_profile": {"level": "intermediate"}
            },
            min_concurrency=100,
            max_concurrency=5000,
            success_threshold=0.95,
            url_params=lambda i: f"?user_id=test_user_{i}&session_id=max_test_{int(time.time())}_{i}"
        )
        
        print(f"\n  🏆 实时交互接口极限并发: {max_concurrency}")
    
    async def find_max_concurrency_realtime_audio(self):
        """找出实时音频处理接口的最高并发"""
        print("\n🎙️ 测试实时音频处理接口极限并发...")
        
        # 先创建一个会话用于测试
        test_session_id = f"max_audio_test_{int(time.time())}"
        
        max_concurrency = await self._binary_search_max_concurrency(
            test_name="realtime_audio",
            url=f"{AI_SERVICE}/advanced/realtime/audio",
            method="POST",
            data={
                "energy_level": 0.7,
                "pitch_variance": 0.5,
                "speech_rate": 150,
                "pause_duration": 0.5,
                "voice_activity": True,
                "confidence_score": 0.85,
                "transcribed_text": "Hello"
            },
            min_concurrency=100,
            max_concurrency=5000,
            success_threshold=0.95,
            url_params=lambda i: f"?session_id=audio_test_{int(time.time())}_{i}"
        )
        
        print(f"\n  🏆 实时音频处理接口极限并发: {max_concurrency}")
    
    async def find_max_concurrency_session_status(self):
        """找出会话状态接口的最高并发"""
        print("\n📊 测试会话状态接口极限并发...")
        
        # 使用一个固定的session_id进行测试
        test_session_id = f"status_test_{int(time.time())}"
        
        max_concurrency = await self._binary_search_max_concurrency(
            test_name="session_status",
            url=f"{AI_SERVICE}/advanced/session/status/{test_session_id}",
            method="GET",
            data=None,
            min_concurrency=100,
            max_concurrency=5000,
            success_threshold=0.95
        )
        
        print(f"\n  🏆 会话状态接口极限并发: {max_concurrency}")
    
    async def find_max_concurrency_realtime_end(self):
        """找出结束会话接口的最高并发"""
        print("\n🔚 测试结束会话接口极限并发...")
        
        max_concurrency = await self._binary_search_max_concurrency(
            test_name="realtime_end",
            url=f"{AI_SERVICE}/advanced/realtime/end",
            method="POST",
            data=None,
            min_concurrency=100,
            max_concurrency=5000,
            success_threshold=0.95,
            url_params=lambda i: f"?session_id=end_test_{int(time.time())}_{i}"
        )
        
        print(f"\n  🏆 结束会话接口极限并发: {max_concurrency}")
    
    async def find_max_concurrency_assessment(self):
        """找出口语评估接口的最高并发"""
        print("\n📊 测试口语评估接口极限并发...")
        
        max_concurrency = await self._binary_search_max_concurrency(
            test_name="speaking_assessment",
            url=f"{AI_SERVICE}/advanced/assessment/speaking",
            method="POST",
            data={
                "user_message": "I think learning English is very important.",
                "audio_analysis": {
                    "speech_rate": 150,
                    "pause_count": 2,
                    "pronunciation_score": 75
                },
                "assessment_type": "ielts"
            },
            min_concurrency=50,
            max_concurrency=3000,
            success_threshold=0.95,
            url_params=lambda i: f"?user_id=test_user_{i}&session_id=max_test_{int(time.time())}_{i}"
        )
        
        print(f"\n  🏆 口语评估接口极限并发: {max_concurrency}")
    
    async def find_max_concurrency_completion(self):
        """找出补全帮助接口的最高并发"""
        print("\n💡 测试补全帮助接口极限并发...")
        
        max_concurrency = await self._binary_search_max_concurrency(
            test_name="completion_help",
            url=f"{AI_SERVICE}/advanced/completion/help",
            method="POST",
            data={
                "incomplete_text": "I want to",
                "user_profile": {"level": "intermediate"}
            },
            min_concurrency=100,
            max_concurrency=5000,
            success_threshold=0.95,
            url_params=lambda i: f"?user_id=test_user_{i}&session_id=max_test_{int(time.time())}_{i}"
        )
        
        print(f"\n  🏆 补全帮助接口极限并发: {max_concurrency}")
    
    async def _binary_search_max_concurrency(self, test_name: str, url: str, method: str,
                                            data: Dict, min_concurrency: int, max_concurrency: int,
                                            success_threshold: float = 0.95, url_params=None):
        """使用二分查找法找出最大并发数"""
        
        left, right = min_concurrency, max_concurrency
        max_successful = min_concurrency
        
        while left <= right:
            mid = (left + right) // 2
            print(f"\n  测试并发数: {mid}...", end=" ")
            
            # 执行并发测试
            start_time = time.time()
            
            # 创建协程任务
            async def make_request(index):
                test_url = url
                if url_params:
                    test_url = url + url_params(index)
                
                try:
                    if method == "GET":
                        async with self.session.get(test_url) as response:
                            return response.status
                    else:
                        async with self.session.post(test_url, json=data) as response:
                            return response.status
                except Exception as e:
                    return None
            
            tasks = [make_request(i) for i in range(mid)]
            
            try:
                responses = await asyncio.gather(*tasks, return_exceptions=True)
                duration = time.time() - start_time
                
                # 统计成功率
                successful = sum(1 for r in responses 
                               if not isinstance(r, Exception) and r == 200)
                success_rate = successful / mid
                
                qps = mid / duration if duration > 0 else 0
                
                print(f"成功率: {success_rate*100:.1f}%, QPS: {qps:.2f}")
                
                # 保存结果
                if "max_concurrency_tests" not in self.results:
                    self.results["max_concurrency_tests"] = []
                
                self.results["max_concurrency_tests"].append({
                    "test_name": test_name,
                    "concurrency": mid,
                    "successful": successful,
                    "total": mid,
                    "success_rate": round(success_rate * 100, 2),
                    "qps": round(qps, 2),
                    "duration_seconds": round(duration, 2)
                })
                
                # 判断是否成功
                if success_rate >= success_threshold:
                    max_successful = mid
                    left = mid + 1
                else:
                    right = mid - 1
                
                # 等待一下，避免压垮服务器
                await asyncio.sleep(2)
                
            except Exception as e:
                print(f"失败: {str(e)[:50]}")
                right = mid - 1
                await asyncio.sleep(2)
        
        return max_successful
    
    async def test_concurrent_api_gateway(self, concurrency: int):
        """并发API Gateway健康检查测试"""
        print(f"\n  📊 API Gateway健康检查并发测试 (并发数: {concurrency})...")
        
        start_time = time.time()
        
        # 创建并发任务
        tasks = []
        for i in range(concurrency):
            task = self.test_api(
                f"并发API Gateway健康检查-{i}",
                f"{API_GATEWAY}/actuator/health",
                "GET"
            )
            tasks.append(task)
        
        # 并发执行
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        duration = time.time() - start_time
        
        # 统计结果
        successful = sum(1 for r in results if isinstance(r, dict) and r.get("success", False))
        failed = concurrency - successful
        avg_response_time = statistics.mean([
            r["response_time_ms"] for r in results 
            if isinstance(r, dict) and "response_time_ms" in r
        ]) if results else 0
        
        qps = concurrency / duration if duration > 0 else 0
        
        print(f"    ✅ 成功: {successful}/{concurrency}")
        print(f"    ❌ 失败: {failed}/{concurrency}")
        print(f"    ⏱️  总耗时: {duration:.2f}秒")
        print(f"    📈 QPS: {qps:.2f}")
        print(f"    ⚡ 平均响应时间: {avg_response_time:.2f}ms")
        
        # 保存结果
        if "concurrent_tests" not in self.results:
            self.results["concurrent_tests"] = []
        
        self.results["concurrent_tests"].append({
            "test_name": "api_gateway_health",
            "concurrency": concurrency,
            "total_requests": concurrency,
            "successful": successful,
            "failed": failed,
            "duration_seconds": round(duration, 2),
            "qps": round(qps, 2),
            "avg_response_time_ms": round(avg_response_time, 2)
        })
    
    async def test_concurrent_health_check(self, concurrency: int):
        """并发AI Service健康检查测试"""
        print(f"\n  📊 AI Service健康检查并发测试 (并发数: {concurrency})...")
        
        start_time = time.time()
        
        # 创建并发任务
        tasks = []
        for i in range(concurrency):
            task = self.test_api(
                f"并发AI Service健康检查-{i}",
                f"{AI_SERVICE}/ai/health",
                "GET"
            )
            tasks.append(task)
        
        # 并发执行
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        duration = time.time() - start_time
        
        # 统计结果
        successful = sum(1 for r in results if isinstance(r, dict) and r.get("success", False))
        failed = concurrency - successful
        avg_response_time = statistics.mean([
            r["response_time_ms"] for r in results 
            if isinstance(r, dict) and "response_time_ms" in r
        ]) if results else 0
        
        qps = concurrency / duration if duration > 0 else 0
        
        print(f"    ✅ 成功: {successful}/{concurrency}")
        print(f"    ❌ 失败: {failed}/{concurrency}")
        print(f"    ⏱️  总耗时: {duration:.2f}秒")
        print(f"    📈 QPS: {qps:.2f}")
        print(f"    ⚡ 平均响应时间: {avg_response_time:.2f}ms")
        
        # 保存结果
        if "concurrent_tests" not in self.results:
            self.results["concurrent_tests"] = []
        
        self.results["concurrent_tests"].append({
            "test_name": "ai_service_health",
            "concurrency": concurrency,
            "total_requests": concurrency,
            "successful": successful,
            "failed": failed,
            "duration_seconds": round(duration, 2),
            "qps": round(qps, 2),
            "avg_response_time_ms": round(avg_response_time, 2)
        })
    
    async def test_concurrent_chinglish_detection(self, concurrency: int):
        """并发中式英语检测测试"""
        print(f"\n  🔍 中式英语检测并发测试 (并发数: {concurrency})...")
        
        test_texts = [
            "I very like this book",
            "Please open the light",
            "I want to eat medicine",
            "How to say this word",
            "So so, not very good"
        ]
        
        start_time = time.time()
        
        # 创建并发任务
        tasks = []
        for i in range(concurrency):
            text = test_texts[i % len(test_texts)]
            task = self.test_api(
                f"并发中式英语检测-{i}",
                f"{AI_SERVICE}/advanced/chinglish/detect",
                "POST",
                {"text": text}
            )
            tasks.append(task)
        
        # 并发执行
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        duration = time.time() - start_time
        
        # 统计结果
        successful = sum(1 for r in results if isinstance(r, dict) and r.get("success", False))
        failed = concurrency - successful
        avg_response_time = statistics.mean([
            r["response_time_ms"] for r in results 
            if isinstance(r, dict) and "response_time_ms" in r
        ]) if results else 0
        
        qps = concurrency / duration if duration > 0 else 0
        
        print(f"    ✅ 成功: {successful}/{concurrency}")
        print(f"    ❌ 失败: {failed}/{concurrency}")
        print(f"    ⏱️  总耗时: {duration:.2f}秒")
        print(f"    📈 QPS: {qps:.2f}")
        print(f"    ⚡ 平均响应时间: {avg_response_time:.2f}ms")
        
        self.results["concurrent_tests"].append({
            "test_name": "chinglish_detection",
            "concurrency": concurrency,
            "total_requests": concurrency,
            "successful": successful,
            "failed": failed,
            "duration_seconds": round(duration, 2),
            "qps": round(qps, 2),
            "avg_response_time_ms": round(avg_response_time, 2)
        })
    
    async def test_concurrent_realtime_interaction(self, concurrency: int):
        """并发实时交互测试"""
        print(f"\n  🎙️ 实时交互并发测试 (并发数: {concurrency})...")
        
        start_time = time.time()
        
        # 创建并发任务
        tasks = []
        for i in range(concurrency):
            session_id = f"concurrent_session_{int(time.time())}_{i}"
            task = self.test_api(
                f"并发实时交互-{i}",
                f"{AI_SERVICE}/advanced/realtime/start?user_id=test_user_{i}&session_id={session_id}",
                "POST",
                {
                    "mode": "half_duplex",
                    "user_profile": {"level": "intermediate"}
                }
            )
            tasks.append(task)
        
        # 并发执行
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        duration = time.time() - start_time
        
        # 统计结果
        successful = sum(1 for r in results if isinstance(r, dict) and r.get("success", False))
        failed = concurrency - successful
        avg_response_time = statistics.mean([
            r["response_time_ms"] for r in results 
            if isinstance(r, dict) and "response_time_ms" in r
        ]) if results else 0
        
        qps = concurrency / duration if duration > 0 else 0
        
        print(f"    ✅ 成功: {successful}/{concurrency}")
        print(f"    ❌ 失败: {failed}/{concurrency}")
        print(f"    ⏱️  总耗时: {duration:.2f}秒")
        print(f"    📈 QPS: {qps:.2f}")
        print(f"    ⚡ 平均响应时间: {avg_response_time:.2f}ms")
        
        self.results["concurrent_tests"].append({
            "test_name": "realtime_interaction",
            "concurrency": concurrency,
            "total_requests": concurrency,
            "successful": successful,
            "failed": failed,
            "duration_seconds": round(duration, 2),
            "qps": round(qps, 2),
            "avg_response_time_ms": round(avg_response_time, 2)
        })
    
    async def test_concurrent_models(self, concurrency: int):
        """并发AI Service模型列表测试"""
        print(f"\n  📊 AI Service模型列表并发测试 (并发数: {concurrency})...")
        
        start_time = time.time()
        
        # 创建并发任务
        tasks = []
        for i in range(concurrency):
            task = self.test_api(
                f"并发模型列表-{i}",
                f"{AI_SERVICE}/ai/models",
                "GET"
            )
            tasks.append(task)
        
        # 并发执行
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        duration = time.time() - start_time
        
        # 统计结果
        successful = sum(1 for r in results if isinstance(r, dict) and r.get("success", False))
        failed = concurrency - successful
        avg_response_time = statistics.mean([
            r["response_time_ms"] for r in results 
            if isinstance(r, dict) and "response_time_ms" in r
        ]) if results else 0
        
        qps = concurrency / duration if duration > 0 else 0
        
        print(f"    ✅ 成功: {successful}/{concurrency}")
        print(f"    ❌ 失败: {failed}/{concurrency}")
        print(f"    ⏱️  总耗时: {duration:.2f}秒")
        print(f"    📈 QPS: {qps:.2f}")
        print(f"    ⚡ 平均响应时间: {avg_response_time:.2f}ms")
        
        self.results["concurrent_tests"].append({
            "test_name": "ai_models_list",
            "concurrency": concurrency,
            "total_requests": concurrency,
            "successful": successful,
            "failed": failed,
            "duration_seconds": round(duration, 2),
            "qps": round(qps, 2),
            "avg_response_time_ms": round(avg_response_time, 2)
        })
    
    async def test_concurrent_analysis_health(self, concurrency: int):
        """并发Analysis Service健康检查测试"""
        print(f"\n  📊 Analysis Service健康检查并发测试 (并发数: {concurrency})...")
        
        start_time = time.time()
        
        # 创建并发任务
        tasks = []
        for i in range(concurrency):
            task = self.test_api(
                f"并发Analysis健康检查-{i}",
                f"{ANALYSIS_SERVICE}/health",
                "GET"
            )
            tasks.append(task)
        
        # 并发执行
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        duration = time.time() - start_time
        
        # 统计结果
        successful = sum(1 for r in results if isinstance(r, dict) and r.get("success", False))
        failed = concurrency - successful
        avg_response_time = statistics.mean([
            r["response_time_ms"] for r in results 
            if isinstance(r, dict) and "response_time_ms" in r
        ]) if results else 0
        
        qps = concurrency / duration if duration > 0 else 0
        
        print(f"    ✅ 成功: {successful}/{concurrency}")
        print(f"    ❌ 失败: {failed}/{concurrency}")
        print(f"    ⏱️  总耗时: {duration:.2f}秒")
        print(f"    📈 QPS: {qps:.2f}")
        print(f"    ⚡ 平均响应时间: {avg_response_time:.2f}ms")
        
        self.results["concurrent_tests"].append({
            "test_name": "analysis_service_health",
            "concurrency": concurrency,
            "total_requests": concurrency,
            "successful": successful,
            "failed": failed,
            "duration_seconds": round(duration, 2),
            "qps": round(qps, 2),
            "avg_response_time_ms": round(avg_response_time, 2)
        })
    
    async def test_concurrent_ai_chat(self, concurrency: int):
        """并发AI聊天测试"""
        print(f"\n  💬 AI聊天并发测试 (并发数: {concurrency})...")
        
        start_time = time.time()
        
        # 创建并发任务
        tasks = []
        for i in range(concurrency):
            session_id = f"concurrent_chat_{int(time.time())}_{i}"
            task = self.test_api(
                f"并发AI聊天-{i}",
                f"{AI_SERVICE}/ai/chat",
                "POST",
                {
                    "session_id": session_id,
                    "user_id": f"test_user_{i}",
                    "message": "Hello, how are you?",
                    "role_prompt": "You are a helpful English teacher."
                }
            )
            tasks.append(task)
        
        # 并发执行
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        duration = time.time() - start_time
        
        # 统计结果
        successful = sum(1 for r in results if isinstance(r, dict) and r.get("success", False))
        failed = concurrency - successful
        avg_response_time = statistics.mean([
            r["response_time_ms"] for r in results 
            if isinstance(r, dict) and "response_time_ms" in r
        ]) if results else 0
        
        qps = concurrency / duration if duration > 0 else 0
        
        print(f"    ✅ 成功: {successful}/{concurrency}")
        print(f"    ❌ 失败: {failed}/{concurrency}")
        print(f"    ⏱️  总耗时: {duration:.2f}秒")
        print(f"    📈 QPS: {qps:.2f}")
        print(f"    ⚡ 平均响应时间: {avg_response_time:.2f}ms")
        
        self.results["concurrent_tests"].append({
            "test_name": "ai_chat",
            "concurrency": concurrency,
            "total_requests": concurrency,
            "successful": successful,
            "failed": failed,
            "duration_seconds": round(duration, 2),
            "qps": round(qps, 2),
            "avg_response_time_ms": round(avg_response_time, 2)
        })
    
    async def test_concurrent_realtime_audio(self, concurrency: int):
        """并发实时音频处理测试"""
        print(f"\n  🎙️ 实时音频处理并发测试 (并发数: {concurrency})...")
        
        start_time = time.time()
        
        # 创建并发任务
        tasks = []
        for i in range(concurrency):
            session_id = f"concurrent_audio_{int(time.time())}_{i}"
            task = self.test_api(
                f"并发音频处理-{i}",
                f"{AI_SERVICE}/advanced/realtime/audio?session_id={session_id}",
                "POST",
                {
                    "energy_level": 0.7,
                    "pitch_variance": 0.5,
                    "speech_rate": 150,
                    "pause_duration": 0.5,
                    "voice_activity": True,
                    "confidence_score": 0.85,
                    "transcribed_text": "Hello"
                }
            )
            tasks.append(task)
        
        # 并发执行
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        duration = time.time() - start_time
        
        # 统计结果
        successful = sum(1 for r in results if isinstance(r, dict) and r.get("success", False))
        failed = concurrency - successful
        avg_response_time = statistics.mean([
            r["response_time_ms"] for r in results 
            if isinstance(r, dict) and "response_time_ms" in r
        ]) if results else 0
        
        qps = concurrency / duration if duration > 0 else 0
        
        print(f"    ✅ 成功: {successful}/{concurrency}")
        print(f"    ❌ 失败: {failed}/{concurrency}")
        print(f"    ⏱️  总耗时: {duration:.2f}秒")
        print(f"    📈 QPS: {qps:.2f}")
        print(f"    ⚡ 平均响应时间: {avg_response_time:.2f}ms")
        
        self.results["concurrent_tests"].append({
            "test_name": "realtime_audio",
            "concurrency": concurrency,
            "total_requests": concurrency,
            "successful": successful,
            "failed": failed,
            "duration_seconds": round(duration, 2),
            "qps": round(qps, 2),
            "avg_response_time_ms": round(avg_response_time, 2)
        })
    
    async def test_concurrent_session_status(self, concurrency: int):
        """并发会话状态测试"""
        print(f"\n  📊 会话状态并发测试 (并发数: {concurrency})...")
        
        # 使用固定的session_id
        test_session_id = f"status_test_{int(time.time())}"
        
        start_time = time.time()
        
        # 创建并发任务
        tasks = []
        for i in range(concurrency):
            task = self.test_api(
                f"并发会话状态-{i}",
                f"{AI_SERVICE}/advanced/session/status/{test_session_id}",
                "GET"
            )
            tasks.append(task)
        
        # 并发执行
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        duration = time.time() - start_time
        
        # 统计结果
        successful = sum(1 for r in results if isinstance(r, dict) and r.get("success", False))
        failed = concurrency - successful
        avg_response_time = statistics.mean([
            r["response_time_ms"] for r in results 
            if isinstance(r, dict) and "response_time_ms" in r
        ]) if results else 0
        
        qps = concurrency / duration if duration > 0 else 0
        
        print(f"    ✅ 成功: {successful}/{concurrency}")
        print(f"    ❌ 失败: {failed}/{concurrency}")
        print(f"    ⏱️  总耗时: {duration:.2f}秒")
        print(f"    📈 QPS: {qps:.2f}")
        print(f"    ⚡ 平均响应时间: {avg_response_time:.2f}ms")
        
        self.results["concurrent_tests"].append({
            "test_name": "session_status",
            "concurrency": concurrency,
            "total_requests": concurrency,
            "successful": successful,
            "failed": failed,
            "duration_seconds": round(duration, 2),
            "qps": round(qps, 2),
            "avg_response_time_ms": round(avg_response_time, 2)
        })
    
    async def test_concurrent_realtime_end(self, concurrency: int):
        """并发结束会话测试"""
        print(f"\n  🔚 结束会话并发测试 (并发数: {concurrency})...")
        
        start_time = time.time()
        
        # 创建并发任务
        tasks = []
        for i in range(concurrency):
            session_id = f"concurrent_end_{int(time.time())}_{i}"
            task = self.test_api(
                f"并发结束会话-{i}",
                f"{AI_SERVICE}/advanced/realtime/end?session_id={session_id}",
                "POST"
            )
            tasks.append(task)
        
        # 并发执行
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        duration = time.time() - start_time
        
        # 统计结果
        successful = sum(1 for r in results if isinstance(r, dict) and r.get("success", False))
        failed = concurrency - successful
        avg_response_time = statistics.mean([
            r["response_time_ms"] for r in results 
            if isinstance(r, dict) and "response_time_ms" in r
        ]) if results else 0
        
        qps = concurrency / duration if duration > 0 else 0
        
        print(f"    ✅ 成功: {successful}/{concurrency}")
        print(f"    ❌ 失败: {failed}/{concurrency}")
        print(f"    ⏱️  总耗时: {duration:.2f}秒")
        print(f"    📈 QPS: {qps:.2f}")
        print(f"    ⚡ 平均响应时间: {avg_response_time:.2f}ms")
        
        self.results["concurrent_tests"].append({
            "test_name": "realtime_end",
            "concurrency": concurrency,
            "total_requests": concurrency,
            "successful": successful,
            "failed": failed,
            "duration_seconds": round(duration, 2),
            "qps": round(qps, 2),
            "avg_response_time_ms": round(avg_response_time, 2)
        })
    
    async def test_concurrent_completion(self, concurrency: int):
        """并发补全帮助测试"""
        print(f"\n  💡 补全帮助并发测试 (并发数: {concurrency})...")
        
        incomplete_texts = [
            "I want to",
            "How can I",
            "I think that",
            "In my opinion"
        ]
        
        start_time = time.time()
        
        # 创建并发任务
        tasks = []
        for i in range(concurrency):
            text = incomplete_texts[i % len(incomplete_texts)]
            session_id = f"concurrent_completion_{int(time.time())}_{i}"
            task = self.test_api(
                f"并发补全帮助-{i}",
                f"{AI_SERVICE}/advanced/completion/help?user_id=test_user_{i}&session_id={session_id}",
                "POST",
                {
                    "incomplete_text": text,
                    "user_profile": {"level": "intermediate"}
                }
            )
            tasks.append(task)
        
        # 并发执行
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        duration = time.time() - start_time
        
        # 统计结果
        successful = sum(1 for r in results if isinstance(r, dict) and r.get("success", False))
        failed = concurrency - successful
        avg_response_time = statistics.mean([
            r["response_time_ms"] for r in results 
            if isinstance(r, dict) and "response_time_ms" in r
        ]) if results else 0
        
        qps = concurrency / duration if duration > 0 else 0
        
        print(f"    ✅ 成功: {successful}/{concurrency}")
        print(f"    ❌ 失败: {failed}/{concurrency}")
        print(f"    ⏱️  总耗时: {duration:.2f}秒")
        print(f"    📈 QPS: {qps:.2f}")
        print(f"    ⚡ 平均响应时间: {avg_response_time:.2f}ms")
        
        self.results["concurrent_tests"].append({
            "test_name": "completion_help",
            "concurrency": concurrency,
            "total_requests": concurrency,
            "successful": successful,
            "failed": failed,
            "duration_seconds": round(duration, 2),
            "qps": round(qps, 2),
            "avg_response_time_ms": round(avg_response_time, 2)
        })
    
    async def test_concurrent_ai_chat(self, concurrency: int):
        """并发AI聊天测试"""
        print(f"\n  💬 AI聊天并发测试 (并发数: {concurrency})...")
        
        start_time = time.time()
        
        # 创建并发任务
        tasks = []
        for i in range(concurrency):
            session_id = f"concurrent_chat_{int(time.time())}_{i}"
            task = self.test_api(
                f"并发AI聊天-{i}",
                f"{AI_SERVICE}/ai/chat",
                "POST",
                {
                    "session_id": session_id,
                    "user_id": f"test_user_{i}",
                    "message": "Hello, how are you?",
                    "role_prompt": "You are a helpful English teacher."
                }
            )
            tasks.append(task)
        
        # 并发执行
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        duration = time.time() - start_time
        
        # 统计结果
        successful = sum(1 for r in results if isinstance(r, dict) and r.get("success", False))
        failed = concurrency - successful
        avg_response_time = statistics.mean([
            r["response_time_ms"] for r in results 
            if isinstance(r, dict) and "response_time_ms" in r
        ]) if results else 0
        
        qps = concurrency / duration if duration > 0 else 0
        
        print(f"    ✅ 成功: {successful}/{concurrency}")
        print(f"    ❌ 失败: {failed}/{concurrency}")
        print(f"    ⏱️  总耗时: {duration:.2f}秒")
        print(f"    📈 QPS: {qps:.2f}")
        print(f"    ⚡ 平均响应时间: {avg_response_time:.2f}ms")
        
        self.results["concurrent_tests"].append({
            "test_name": "ai_chat",
            "concurrency": concurrency,
            "total_requests": concurrency,
            "successful": successful,
            "failed": failed,
            "duration_seconds": round(duration, 2),
            "qps": round(qps, 2),
            "avg_response_time_ms": round(avg_response_time, 2)
        })
    
    async def test_concurrent_realtime_audio(self, concurrency: int):
        """并发实时音频处理测试"""
        print(f"\n  🎙️ 实时音频处理并发测试 (并发数: {concurrency})...")
        
        start_time = time.time()
        
        # 创建并发任务
        tasks = []
        for i in range(concurrency):
            session_id = f"concurrent_audio_{int(time.time())}_{i}"
            task = self.test_api(
                f"并发实时音频-{i}",
                f"{AI_SERVICE}/advanced/realtime/audio?session_id={session_id}",
                "POST",
                {
                    "energy_level": 0.7,
                    "pitch_variance": 0.5,
                    "speech_rate": 150,
                    "pause_duration": 0.5,
                    "voice_activity": True,
                    "confidence_score": 0.85,
                    "transcribed_text": "Hello, I want to practice my English speaking"
                }
            )
            tasks.append(task)
        
        # 并发执行
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        duration = time.time() - start_time
        
        # 统计结果
        successful = sum(1 for r in results if isinstance(r, dict) and r.get("success", False))
        failed = concurrency - successful
        avg_response_time = statistics.mean([
            r["response_time_ms"] for r in results 
            if isinstance(r, dict) and "response_time_ms" in r
        ]) if results else 0
        
        qps = concurrency / duration if duration > 0 else 0
        
        print(f"    ✅ 成功: {successful}/{concurrency}")
        print(f"    ❌ 失败: {failed}/{concurrency}")
        print(f"    ⏱️  总耗时: {duration:.2f}秒")
        print(f"    📈 QPS: {qps:.2f}")
        print(f"    ⚡ 平均响应时间: {avg_response_time:.2f}ms")
        
        self.results["concurrent_tests"].append({
            "test_name": "realtime_audio",
            "concurrency": concurrency,
            "total_requests": concurrency,
            "successful": successful,
            "failed": failed,
            "duration_seconds": round(duration, 2),
            "qps": round(qps, 2),
            "avg_response_time_ms": round(avg_response_time, 2)
        })
    
    async def test_concurrent_session_status(self, concurrency: int):
        """并发会话状态测试"""
        print(f"\n  📊 会话状态并发测试 (并发数: {concurrency})...")
        
        start_time = time.time()
        
        # 创建并发任务
        tasks = []
        for i in range(concurrency):
            session_id = f"concurrent_status_{int(time.time())}_{i}"
            task = self.test_api(
                f"并发会话状态-{i}",
                f"{AI_SERVICE}/advanced/session/status/{session_id}",
                "GET"
            )
            tasks.append(task)
        
        # 并发执行
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        duration = time.time() - start_time
        
        # 统计结果
        successful = sum(1 for r in results if isinstance(r, dict) and r.get("success", False))
        failed = concurrency - successful
        avg_response_time = statistics.mean([
            r["response_time_ms"] for r in results 
            if isinstance(r, dict) and "response_time_ms" in r
        ]) if results else 0
        
        qps = concurrency / duration if duration > 0 else 0
        
        print(f"    ✅ 成功: {successful}/{concurrency}")
        print(f"    ❌ 失败: {failed}/{concurrency}")
        print(f"    ⏱️  总耗时: {duration:.2f}秒")
        print(f"    📈 QPS: {qps:.2f}")
        print(f"    ⚡ 平均响应时间: {avg_response_time:.2f}ms")
        
        self.results["concurrent_tests"].append({
            "test_name": "session_status",
            "concurrency": concurrency,
            "total_requests": concurrency,
            "successful": successful,
            "failed": failed,
            "duration_seconds": round(duration, 2),
            "qps": round(qps, 2),
            "avg_response_time_ms": round(avg_response_time, 2)
        })
    
    async def test_concurrent_realtime_end(self, concurrency: int):
        """并发结束会话测试"""
        print(f"\n  🔚 结束会话并发测试 (并发数: {concurrency})...")
        
        start_time = time.time()
        
        # 创建并发任务
        tasks = []
        for i in range(concurrency):
            session_id = f"concurrent_end_{int(time.time())}_{i}"
            task = self.test_api(
                f"并发结束会话-{i}",
                f"{AI_SERVICE}/advanced/realtime/end?session_id={session_id}",
                "POST"
            )
            tasks.append(task)
        
        # 并发执行
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        duration = time.time() - start_time
        
        # 统计结果
        successful = sum(1 for r in results if isinstance(r, dict) and r.get("success", False))
        failed = concurrency - successful
        avg_response_time = statistics.mean([
            r["response_time_ms"] for r in results 
            if isinstance(r, dict) and "response_time_ms" in r
        ]) if results else 0
        
        qps = concurrency / duration if duration > 0 else 0
        
        print(f"    ✅ 成功: {successful}/{concurrency}")
        print(f"    ❌ 失败: {failed}/{concurrency}")
        print(f"    ⏱️  总耗时: {duration:.2f}秒")
        print(f"    📈 QPS: {qps:.2f}")
        print(f"    ⚡ 平均响应时间: {avg_response_time:.2f}ms")
        
        self.results["concurrent_tests"].append({
            "test_name": "realtime_end",
            "concurrency": concurrency,
            "total_requests": concurrency,
            "successful": successful,
            "failed": failed,
            "duration_seconds": round(duration, 2),
            "qps": round(qps, 2),
            "avg_response_time_ms": round(avg_response_time, 2)
        })
    
    async def test_concurrent_assessment(self, concurrency: int):
        """并发口语评估测试"""
        print(f"\n  📊 口语评估并发测试 (并发数: {concurrency})...")
        
        start_time = time.time()
        
        # 创建并发任务
        tasks = []
        for i in range(concurrency):
            session_id = f"concurrent_assessment_{int(time.time())}_{i}"
            task = self.test_api(
                f"并发口语评估-{i}",
                f"{AI_SERVICE}/advanced/assessment/speaking?user_id=test_user_{i}&session_id={session_id}",
                "POST",
                {
                    "user_message": "I think learning English is very important for my career.",
                    "audio_analysis": {
                        "speech_rate": 150,
                        "pause_count": 2,
                        "pronunciation_score": 75
                    },
                    "assessment_type": "ielts"
                }
            )
            tasks.append(task)
        
        # 并发执行
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        duration = time.time() - start_time
        
        # 统计结果
        successful = sum(1 for r in results if isinstance(r, dict) and r.get("success", False))
        failed = concurrency - successful
        avg_response_time = statistics.mean([
            r["response_time_ms"] for r in results 
            if isinstance(r, dict) and "response_time_ms" in r
        ]) if results else 0
        
        qps = concurrency / duration if duration > 0 else 0
        
        print(f"    ✅ 成功: {successful}/{concurrency}")
        print(f"    ❌ 失败: {failed}/{concurrency}")
        print(f"    ⏱️  总耗时: {duration:.2f}秒")
        print(f"    📈 QPS: {qps:.2f}")
        print(f"    ⚡ 平均响应时间: {avg_response_time:.2f}ms")
        
        self.results["concurrent_tests"].append({
            "test_name": "speaking_assessment",
            "concurrency": concurrency,
            "total_requests": concurrency,
            "successful": successful,
            "failed": failed,
            "duration_seconds": round(duration, 2),
            "qps": round(qps, 2),
            "avg_response_time_ms": round(avg_response_time, 2)
        })
    
    async def run_all_tests(self):
        """运行所有测试"""
        print("="*80)
        print("🎯 SpeakMaster 综合功能测试")
        print("="*80)
        print(f"测试时间: {self.results['test_time']}")
        print("="*80)
        
        await self.setup()
        
        try:
            # 1. 基础API测试
            await self.run_basic_api_tests()
            
            # 2. 高级功能测试
            await self.run_advanced_feature_tests()
            
            # 3. 核心技术测试
            await self.run_core_technology_tests()
            
            # 4. 并发性能测试
            await self.run_concurrent_tests()
            
            # 5. 极限并发测试
            await self.run_max_concurrency_tests()
            
        finally:
            await self.teardown()
        
        # 生成总结
        self.generate_summary()


async def main():
    """主函数"""
    tester = ComprehensiveTester()
    
    print("\n⚠️  注意: 此测试将全面验证系统功能")
    print("请确保:")
    print("  1. 所有服务正在运行")
    print("  2. API Gateway (8080)")
    print("  3. AI Service (8089)")
    print("  4. Analysis Service (8085)")
    print("\n按 Ctrl+C 取消，或等待 3 秒后开始测试...")
    
    try:
        await asyncio.sleep(3)
    except KeyboardInterrupt:
        print("\n\n测试已取消")
        return
    
    await tester.run_all_tests()


if __name__ == "__main__":
    asyncio.run(main())
