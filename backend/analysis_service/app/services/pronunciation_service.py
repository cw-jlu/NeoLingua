"""
发音分析服务
通过子进程调用独立MFA环境，避免依赖冲突
参考 mfa/ 文件夹中的实现
"""
import json
import logging
import os
import re
import shutil
import statistics
import subprocess
import tempfile
from pathlib import Path
from typing import Dict, List, Optional, Any

from app.config import settings

logger = logging.getLogger(__name__)

# 尝试导入praatio（轻量级，不会冲突）
try:
    from praatio import textgrid
except ImportError:
    textgrid = None
    logger.warning("praatio未安装，发音分析功能不可用")


class PronunciationService:
    """
    发音评估服务 - 增强版
    MFA通过独立conda环境的子进程调用，避免与主服务依赖冲突
    新增功能：
    1. 智能纠错反馈策略与时机判断
    2. 多模态反馈设计
    3. 个性化反馈深度调整
    4. 中式英语识别与处理
    """

    def __init__(self):
        # 中国学生常见发音问题提示 - 完整版
        self.phoneme_tips = {
            # 辅音发音问题
            "TH": "舌尖需要伸到上下齿之间，出气而不是爆破。练习：think, thank, three",
            "DH": "像TH，但要振动声带，可以摸喉咙感受震动。练习：this, that, they",
            "R": "舌头向后卷但不要碰到上颚，嘴唇微微收圆。练习：red, right, very",
            "L": "舌尖要顶住上齿龈，不能含糊带过。练习：light, love, hello",
            "W": "嘴唇要明显圆起来，不要弱化成元音。练习：water, what, when",
            "V": "上齿轻咬下唇，不要发成W。练习：very, voice, have",
            "F": "上齿轻咬下唇，强烈送气。练习：face, phone, laugh",
            "P": "双唇紧闭后爆破，送气要强。练习：pen, paper, stop",
            "B": "双唇紧闭后爆破，声带振动。练习：book, baby, job",
            "T": "舌尖顶上齿龈，爆破送气。练习：time, water, cat",
            "D": "舌尖顶上齿龈，声带振动。练习：day, good, red",
            "K": "舌根抵软腭，爆破送气。练习：cat, book, make",
            "G": "舌根抵软腭，声带振动。练习：go, big, dog",
            "S": "舌尖接近上齿龈，气流通过。练习：see, house, yes",
            "Z": "像S但声带振动。练习：zoo, easy, has",
            "SH": "舌尖上翘，嘴唇收圆。练习：she, fish, wash",
            "ZH": "像SH但声带振动。练习：measure, vision",
            "CH": "舌尖贴上齿龈后快速分开。练习：chair, much, teach",
            "JH": "像CH但声带振动。练习：job, age, bridge",
            "M": "双唇闭合，气流从鼻腔出。练习：man, home, swim",
            "N": "舌尖顶上齿龈，不要发成L。练习：no, pen, sun",
            "NG": "舌根抵软腭，鼻音。练习：sing, long, think",
            "H": "声门摩擦音，不要太重。练习：he, house, behind",
            "Y": "舌面贴硬腭，快速滑向元音。练习：yes, you, beyond",
            
            # 元音发音问题
            "AE": "嘴巴张大，像中文'啊'，不要读成E。练习：cat, bad, man",
            "EH": "嘴巴半开，舌位中等。练习：bed, red, ten",
            "IH": "嘴巴微开，舌位较高。练习：bit, sit, big",
            "IY": "嘴角拉开，保持紧张度，不要太短。练习：see, eat, me",
            "AA": "嘴巴大开，舌位最低。练习：hot, got, father",
            "AO": "嘴巴圆开，舌位较低。练习：saw, law, caught",
            "UH": "嘴巴微开，舌位中后。练习：book, good, put",
            "UW": "嘴唇收圆、向前噘，注意长度。练习：food, blue, do",
            "AH": "嘴巴自然张开，舌位中央。练习：but, cup, love",
            "ER": "卷舌+中央元音，注意不要读成'额'。练习：bird, work, her",
            "AX": "弱读元音，很轻很短。练习：about, sofa, China",
            
            # 双元音
            "EY": "从E滑向I，两个音都要清楚。练习：day, make, say",
            "AY": "从A滑向I，张嘴要大。练习：my, time, buy",
            "OY": "从O滑向I，嘴形变化明显。练习：boy, toy, voice",
            "AW": "从A滑向U，嘴形收圆。练习：how, now, house",
            "OW": "从O滑向U，圆唇动作。练习：go, know, show",
            
            # 特殊组合
            "TR": "T和R连读，舌尖快速移动。练习：tree, try, street",
            "DR": "D和R连读，声带振动。练习：dream, drive, address",
            "ST": "S和T连读，不要加元音。练习：stop, fast, best",
            "SK": "S和K连读，保持清晰。练习：school, ask, desk",
            "SP": "S和P连读，P要送气。练习：speak, sport, grasp",
        }
        
        # 发音练习建议
        self.practice_suggestions = {
            "rhythm": [
                "多听英语歌曲，跟着节拍练习",
                "使用节拍器练习句子重音",
                "录音对比，注意语调起伏"
            ],
            "stress": [
                "重读音节要更长、更响、音调更高",
                "弱读音节要轻快，不要拖长",
                "练习单词重音：'PHOto vs phoTOgraphy"
            ],
            "intonation": [
                "陈述句用降调：I like apples↘",
                "一般疑问句用升调：Do you like apples↗",
                "特殊疑问句用降调：What do you like↘"
            ],
            "linking": [
                "辅音+元音连读：an apple → a-napple",
                "相同辅音连读：big game → bi-game", 
                "元音+元音加/j/或/w/：go out → go-wout"
            ]
        }

        # MFA路径配置
        self.mfa_python_path = settings.MFA_PYTHON_PATH
        self.dictionary_path = settings.MFA_DICTIONARY_PATH
        self.acoustic_model_path = settings.MFA_ACOUSTIC_MODEL_PATH

        # 如果未配置，尝试从项目根目录的mfa文件夹查找
        project_root = Path(__file__).resolve().parents[3]
        mfa_dir = project_root / "mfa"

        if not self.dictionary_path and (mfa_dir / "english_us_arpa.dict").exists():
            self.dictionary_path = str(mfa_dir / "english_us_arpa.dict")
        if not self.acoustic_model_path and (mfa_dir / "english_us_arpa" / "english_us_arpa" / "meta.json").exists():
            self.acoustic_model_path = str(mfa_dir / "english_us_arpa" / "english_us_arpa")

        # 新增：纠错反馈策略配置
        self.feedback_strategies = {
            "immediate": {
                "threshold": 0.3,  # 错误严重度阈值
                "description": "即时纠错 - 发现严重错误立即反馈"
            },
            "delayed": {
                "threshold": 0.6,  # 累积错误阈值
                "description": "延时纠错 - 累积一定错误后统一反馈"
            },
            "adaptive": {
                "description": "自适应纠错 - 根据用户水平动态调整"
            }
        }
        
        # 中式英语常见错误模式
        self.chinglish_patterns = {
            # 发音错误
            "th_confusion": {
                "pattern": r"(think|thank|three|this|that|they)",
                "common_error": "用/s/或/z/替代/θ/和/ð/",
                "correction": "舌尖伸到上下齿之间，轻触但不咬紧"
            },
            "r_l_confusion": {
                "pattern": r"(right|light|very|hello|red|led)",
                "common_error": "R/L音混淆",
                "correction": "R音舌头后卷不碰上颚，L音舌尖顶住上齿龈"
            },
            "v_w_confusion": {
                "pattern": r"(very|voice|have|water|what|when)",
                "common_error": "V/W音混淆",
                "correction": "V音上齿轻咬下唇，W音双唇收圆"
            },
            # 语调错误
            "flat_intonation": {
                "pattern": r"(\?|!)",
                "common_error": "语调平淡，缺乏起伏",
                "correction": "疑问句用升调，感叹句用降调"
            },
            # 节奏错误
            "syllable_timing": {
                "pattern": r"\b\w{3,}\b",
                "common_error": "按音节计时而非重音计时",
                "correction": "英语是重音计时语言，重读音节间间隔相等"
            }
        }
        
        # 用户学习曲线跟踪
        self.learning_curve_factors = {
            "beginner": {"error_tolerance": 0.8, "feedback_detail": "high"},
            "intermediate": {"error_tolerance": 0.6, "feedback_detail": "medium"},
            "advanced": {"error_tolerance": 0.4, "feedback_detail": "low"}
        }

    def _clean_text(self, text: str) -> str:
        """清理文本，只保留英文字母和空格"""
        if not text:
            return ""
        text = text.lower()
        text = re.sub(r"[^a-z\s']", "", text)
        text = re.sub(r"\s+", " ", text).strip()
        return text

    def analyze(self, audio_path: Path, text: str, user_profile: Optional[Dict] = None) -> Dict[str, Any]:
        """
        完整发音分析流程 - 增强版
        1. MFA对齐 -> 2. 解析TextGrid -> 3. 评分 -> 4. 智能反馈生成
        新增：用户画像驱动的个性化分析
        """
        if not textgrid:
            return self._get_dummy_result()
        try:
            tg_path = self._align_audio(audio_path, text)
            if not tg_path:
                return self._get_dummy_result()
            phonemes = self._parse_textgrid(tg_path)
            if not phonemes:
                return self._get_dummy_result()
            
            # 增强评分：包含中式英语检测
            scores = self._score_pronunciation(phonemes, text)
            chinglish_analysis = self._detect_chinglish_patterns(text, phonemes)
            
            # 智能反馈生成
            feedback_strategy = self._determine_feedback_strategy(scores, user_profile)
            feedback = self._generate_intelligent_feedback(
                phonemes, scores, chinglish_analysis, feedback_strategy, user_profile
            )
            
            return {
                "pronunciation_score": scores["final_score"],
                "detailed_scores": scores,
                "feedback": feedback,
                "chinglish_analysis": chinglish_analysis,
                "feedback_strategy": feedback_strategy,
                "multimodal_feedback": self._generate_multimodal_feedback(scores, chinglish_analysis)
            }
        except Exception as e:
            logger.error(f"发音分析失败: {e}", exc_info=True)
            return self._get_dummy_result()

    def _get_dummy_result(self):
        """返回安全的默认结果"""
        return {
            "pronunciation_score": 0,
            "detailed_scores": {"total_phonemes": 0, "duration_score": 0, "stress_score": 0, "completeness_score": 0, "final_score": 0},
            "feedback": "发音分析暂不可用"
        }

    def _align_audio(self, audio_path: Path, text: str) -> Optional[Path]:
        """
        通过子进程调用MFA对齐
        使用独立的MFA conda环境，避免依赖冲突
        """
        cleaned_text = self._clean_text(text)
        if not cleaned_text:
            return None

        # 确定MFA worker脚本路径（复用mfa文件夹中的脚本）
        project_root = Path(__file__).resolve().parents[3]
        worker_script = project_root / "mfa" / "mfa_worker_script.py"
        if not worker_script.exists():
            logger.error(f"MFA worker脚本不存在: {worker_script}")
            return None

        # 确定Python路径
        python_path = self.mfa_python_path
        if not python_path or not os.path.exists(python_path):
            if not shutil.which("mfa"):
                logger.warning("MFA未配置且系统PATH中找不到mfa命令，跳过对齐")
                return None
            import sys
            python_path = sys.executable

        with tempfile.TemporaryDirectory() as temp_dir:
            worker_args = {
                "audio_path": str(audio_path),
                "text": cleaned_text,
                "output_dir": temp_dir,
                "dictionary_path": self.dictionary_path,
                "acoustic_model_path": self.acoustic_model_path,
                "mfa_cmd": "mfa"
            }

            # 推导MFA可执行文件路径
            if self.mfa_python_path and os.path.exists(self.mfa_python_path):
                mfa_env_root = Path(self.mfa_python_path).parent
                for candidate in [mfa_env_root / "Scripts" / "mfa.exe", mfa_env_root / "bin" / "mfa"]:
                    if candidate.exists():
                        worker_args["mfa_cmd"] = str(candidate)
                        break
                ffmpeg_candidates = [mfa_env_root / "Library" / "bin" / "ffmpeg.exe", mfa_env_root / "bin" / "ffmpeg"]
                for fp in ffmpeg_candidates:
                    if fp.exists():
                        worker_args["ffmpeg_path"] = str(fp)
                        break

            cmd = [python_path, str(worker_script), json.dumps(worker_args)]
            env = os.environ.copy()
            if self.mfa_python_path and os.path.exists(self.mfa_python_path):
                mfa_env_root = Path(self.mfa_python_path).parent
                extra = [str(mfa_env_root / "Scripts"), str(mfa_env_root / "Library" / "bin"), str(mfa_env_root)]
                env["PATH"] = ";".join(extra) + ";" + env.get("PATH", "")

            try:
                result = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace", env=env, timeout=150)
            except subprocess.TimeoutExpired:
                logger.error("MFA worker超时(150s)")
                return None

            if result.returncode != 0:
                logger.error(f"MFA worker失败: {result.stderr[:500]}")
                return None

            try:
                output = json.loads(result.stdout.strip())
                if "error" in output:
                    logger.error(f"MFA worker报错: {output['error']}")
                    return None
                tg_str = output.get("tg_path")
                if tg_str and os.path.exists(tg_str):
                    fd, persistent = tempfile.mkstemp(suffix=".TextGrid")
                    os.close(fd)
                    shutil.copy(tg_str, persistent)
                    return Path(persistent)
            except json.JSONDecodeError:
                logger.error(f"MFA worker输出解析失败: {result.stdout[:200]}")
        return None

    def _parse_textgrid(self, tg_path: Path) -> List[Dict]:
        """解析TextGrid文件获取音素时间线"""
        try:
            tg = textgrid.openTextgrid(str(tg_path), includeEmptyIntervals=True)
            phone_tier = None
            for tier in tg.tiers:
                if tier.name.lower() == "phones":
                    phone_tier = tier
                    break
            if not phone_tier and tg.tiers:
                phone_tier = tg.tiers[0]
            if not phone_tier:
                return []

            phonemes = []
            for interval in phone_tier.entries:
                label = interval.label.strip()
                if not label or label.lower() in {"sil", "sp", ""}:
                    continue
                phonemes.append({
                    "phoneme": label,
                    "start": round(interval.start, 3),
                    "end": round(interval.end, 3),
                    "duration": round(interval.end - interval.start, 3)
                })
            try:
                os.remove(tg_path)
            except:
                pass
            return phonemes
        except Exception as e:
            logger.error(f"TextGrid解析失败: {e}")
            return []

    def _normalize_phoneme(self, p: str) -> str:
        """去除重音数字 AH0 -> AH"""
        return "".join(c for c in p if not c.isdigit())

    def _score_pronunciation(self, phonemes: List[Dict]) -> Dict[str, Any]:
        """基于音素时间线计算发音评分"""
        total = len(phonemes)
        if total == 0:
            return {"total_phonemes": 0, "duration_score": 0, "stress_score": 0, "completeness_score": 0, "final_score": 0}

        durations = [p["duration"] for p in phonemes if p["duration"] > 0]
        # 节奏/时长评分
        if len(durations) >= 2:
            mean_dur = statistics.mean(durations)
            std_dur = statistics.stdev(durations)
            cv = std_dur / mean_dur if mean_dur > 0 else 0
            speed_score = 100 if 0.05 <= mean_dur <= 0.12 else max(40, 100 - abs(mean_dur - 0.085) * 1000)
            rhythm_score = 95 if 0.3 <= cv <= 0.8 else max(50, 95 - abs(cv - 0.55) * 150)
            duration_score = speed_score * 0.5 + rhythm_score * 0.5
        else:
            duration_score = 50.0

        # 重音评分
        primary = [p for p in phonemes if "1" in p["phoneme"]]
        unstressed = [p for p in phonemes if "0" in p["phoneme"]]
        if primary and unstressed:
            avg_s = statistics.mean([p["duration"] for p in primary])
            avg_u = statistics.mean([p["duration"] for p in unstressed])
            ratio = avg_s / avg_u if avg_u > 0 else 1
            stress_score = min(100, max(55, 90 + (ratio - 1.2) * 10)) if 1.2 <= ratio <= 2.5 else max(55, 90 - abs(ratio - 1.85) * 30)
        else:
            stress_score = 65.0

        # 完整度评分
        short_count = sum(1 for p in phonemes if p["duration"] < 0.02)
        long_count = sum(1 for p in phonemes if p["duration"] > 0.3)
        completeness_score = max(30.0, 100 - short_count * 5 - long_count * 3)

        final = 0.4 * duration_score + 0.3 * stress_score + 0.3 * completeness_score
        return {
            "total_phonemes": total,
            "duration_score": round(duration_score, 1),
            "stress_score": round(stress_score, 1),
            "completeness_score": round(completeness_score, 1),
            "final_score": round(final, 1)
        }

    def _generate_feedback(self, phonemes: List[Dict], scores: Dict) -> str:
        """生成自然语言反馈"""
        lines = []
        final = scores["final_score"]
        if final >= 85:
            lines.append(f"总体得分：{final}/100 — 发音不错，继续保持！")
        elif final >= 70:
            lines.append(f"总体得分：{final}/100 — 还不错，有一些可以改进的地方。")
        else:
            lines.append(f"总体得分：{final}/100 — 需要多加练习，注意以下建议。")

        durations = [p["duration"] for p in phonemes if p["duration"] > 0]
        if durations:
            mean_dur = statistics.mean(durations)
            if mean_dur < 0.05:
                lines.append("⚡ 语速偏快，试着放慢一点。")
            elif mean_dur > 0.12:
                lines.append("🐢 语速偏慢，可以尝试更流畅地连读。")

        short = [p for p in phonemes if p["duration"] < 0.02]
        if short:
            names = [self._normalize_phoneme(p["phoneme"]) for p in short[:3]]
            lines.append(f"🔇 检测到{len(short)}个音素过短（{', '.join(names)}），可能存在吞音。")

        if scores.get("stress_score", 0) < 70:
            lines.append("🎵 重音不够明显，英语中重读音节应该更长、更响亮。")

        counts = {}
        for p in phonemes:
            base = self._normalize_phoneme(p["phoneme"])
            counts[base] = counts.get(base, 0) + 1
        tips_added = 0
        for ph in counts:
            if ph in self.phoneme_tips and tips_added < 3:
                lines.append(f"💬 /{ph}/: {self.phoneme_tips[ph]}")
                tips_added += 1

        return "\n".join(lines)

    def _detect_chinglish_patterns(self, text: str, phonemes: List[Dict]) -> Dict[str, Any]:
        """检测中式英语发音模式"""
        import re
        detected_issues = []
        
        for pattern_name, pattern_info in self.chinglish_patterns.items():
            if "pattern" in pattern_info:
                matches = re.findall(pattern_info["pattern"], text.lower())
                if matches:
                    detected_issues.append({
                        "type": pattern_name,
                        "words": matches,
                        "error_description": pattern_info["common_error"],
                        "correction_advice": pattern_info["correction"]
                    })
        
        # 基于音素分析的额外检测
        phoneme_issues = self._analyze_phoneme_patterns(phonemes)
        detected_issues.extend(phoneme_issues)
        
        return {
            "total_issues": len(detected_issues),
            "issues": detected_issues,
            "severity": "high" if len(detected_issues) > 3 else "medium" if len(detected_issues) > 1 else "low"
        }

    def _analyze_phoneme_patterns(self, phonemes: List[Dict]) -> List[Dict]:
        """基于音素时长和分布分析发音问题"""
        issues = []
        
        # 检测TH音问题（时长异常短可能是发成了S/Z）
        th_phonemes = [p for p in phonemes if p["phoneme"].startswith(("TH", "DH"))]
        for th in th_phonemes:
            if th["duration"] < 0.08:  # TH音正常应该较长
                issues.append({
                    "type": "th_substitution",
                    "phoneme": th["phoneme"],
                    "error_description": "TH音可能发成了S或Z音",
                    "correction_advice": "舌尖伸到齿间，保持摩擦音"
                })
        
        # 检测R音问题（频谱特征异常）
        r_phonemes = [p for p in phonemes if p["phoneme"].startswith("R")]
        if len(r_phonemes) > 0:
            avg_r_duration = sum(r["duration"] for r in r_phonemes) / len(r_phonemes)
            if avg_r_duration < 0.06:  # R音时长过短
                issues.append({
                    "type": "r_pronunciation",
                    "error_description": "R音发音不充分",
                    "correction_advice": "舌头后卷，保持足够时长"
                })
        
        return issues

    def _determine_feedback_strategy(self, scores: Dict, user_profile: Optional[Dict] = None) -> str:
        """智能确定反馈策略"""
        if not user_profile:
            return "adaptive"
        
        user_level = user_profile.get("level", "intermediate")
        error_count = user_profile.get("recent_error_count", 0)
        learning_preference = user_profile.get("feedback_preference", "balanced")
        
        # 基于用户水平和偏好决定策略
        if learning_preference == "immediate" or user_level == "beginner":
            return "immediate"
        elif learning_preference == "delayed" or error_count > 5:
            return "delayed"
        else:
            return "adaptive"

    def _generate_intelligent_feedback(self, phonemes: List[Dict], scores: Dict, 
                                     chinglish_analysis: Dict, strategy: str,
                                     user_profile: Optional[Dict] = None) -> str:
        """生成智能化个性化反馈"""
        lines = []
        final = scores["final_score"]
        user_level = user_profile.get("level", "intermediate") if user_profile else "intermediate"
        
        # 根据策略调整反馈详细程度
        detail_level = self.learning_curve_factors[user_level]["feedback_detail"]
        
        # 总体评价
        if final >= 85:
            lines.append(f"🎉 总体得分：{final}/100 — 发音很棒！继续保持这个水平。")
        elif final >= 70:
            lines.append(f"👍 总体得分：{final}/100 — 不错的进步，还有提升空间。")
        else:
            lines.append(f"💪 总体得分：{final}/100 — 需要加强练习，但不要气馁！")
        
        # 中式英语问题反馈
        if chinglish_analysis["total_issues"] > 0:
            lines.append(f"\n🔍 检测到 {chinglish_analysis['total_issues']} 个中式英语问题：")
            for issue in chinglish_analysis["issues"][:3]:  # 最多显示3个
                if detail_level == "high":
                    lines.append(f"• {issue['error_description']}")
                    lines.append(f"  💡 {issue['correction_advice']}")
                elif detail_level == "medium":
                    lines.append(f"• {issue['correction_advice']}")
        
        # 基于策略的具体建议
        if strategy == "immediate":
            lines.append(self._generate_immediate_feedback(phonemes, scores))
        elif strategy == "delayed":
            lines.append(self._generate_delayed_feedback(phonemes, scores, user_profile))
        else:  # adaptive
            lines.append(self._generate_adaptive_feedback(phonemes, scores, user_profile))
        
        return "\n".join(lines)

    def _generate_immediate_feedback(self, phonemes: List[Dict], scores: Dict) -> str:
        """即时反馈：立即指出关键问题"""
        feedback = "\n⚡ 即时反馈："
        
        # 找出最严重的问题
        durations = [p["duration"] for p in phonemes if p["duration"] > 0]
        if durations:
            mean_dur = statistics.mean(durations)
            if mean_dur < 0.05:
                feedback += "\n• 语速过快，请放慢速度，清晰发音"
            elif mean_dur > 0.12:
                feedback += "\n• 语速偏慢，可以适当加快"
        
        short_phonemes = [p for p in phonemes if p["duration"] < 0.02]
        if len(short_phonemes) > 2:
            feedback += f"\n• 有{len(short_phonemes)}个音素发音过短，注意不要吞音"
        
        return feedback

    def _generate_delayed_feedback(self, phonemes: List[Dict], scores: Dict, user_profile: Optional[Dict]) -> str:
        """延时反馈：累积问题统一反馈"""
        feedback = "\n📊 综合分析："
        
        # 基于历史数据的趋势分析
        if user_profile and "error_history" in user_profile:
            recent_errors = user_profile["error_history"][-5:]  # 最近5次错误
            common_errors = {}
            for error in recent_errors:
                error_type = error.get("type", "unknown")
                common_errors[error_type] = common_errors.get(error_type, 0) + 1
            
            if common_errors:
                most_common = max(common_errors.items(), key=lambda x: x[1])
                feedback += f"\n• 最需要改进：{most_common[0]}（出现{most_common[1]}次）"
        
        # 整体建议
        if scores.get("stress_score", 0) < 70:
            feedback += "\n• 重音练习：多听标准发音，注意重读音节"
        
        return feedback

    def _generate_adaptive_feedback(self, phonemes: List[Dict], scores: Dict, user_profile: Optional[Dict]) -> str:
        """自适应反馈：根据用户水平动态调整"""
        if not user_profile:
            return self._generate_immediate_feedback(phonemes, scores)
        
        user_level = user_profile.get("level", "intermediate")
        feedback = f"\n🎯 个性化建议（{user_level}水平）："
        
        if user_level == "beginner":
            feedback += "\n• 重点关注基础音素的准确性"
            feedback += "\n• 建议：每天练习15分钟基础发音"
        elif user_level == "intermediate":
            feedback += "\n• 注意语调和节奏的自然性"
            feedback += "\n• 建议：多进行连读和弱读练习"
        else:  # advanced
            feedback += "\n• 追求更自然的语音语调"
            feedback += "\n• 建议：模仿母语者的语音特征"
        
        return feedback

    def _generate_multimodal_feedback(self, scores: Dict, chinglish_analysis: Dict) -> Dict[str, Any]:
        """生成多模态反馈（文本+语音+视觉）"""
        return {
            "text_feedback": {
                "summary": f"发音得分：{scores['final_score']}/100",
                "key_points": [
                    f"节奏评分：{scores.get('duration_score', 0)}/100",
                    f"重音评分：{scores.get('stress_score', 0)}/100",
                    f"完整度：{scores.get('completeness_score', 0)}/100"
                ]
            },
            "audio_feedback": {
                "tts_text": self._generate_audio_example_text(chinglish_analysis),
                "speed": 0.8,  # 慢速示范
                "emphasis": True
            },
            "visual_feedback": {
                "score_chart": {
                    "type": "radar",
                    "data": {
                        "节奏": scores.get("duration_score", 0),
                        "重音": scores.get("stress_score", 0),
                        "完整度": scores.get("completeness_score", 0)
                    }
                },
                "mouth_shape_guide": self._get_mouth_shape_guide(chinglish_analysis),
                "progress_indicator": {
                    "current_score": scores["final_score"],
                    "target_score": min(100, scores["final_score"] + 10),
                    "improvement_tips": 3
                }
            },
            "emotional_feedback": {
                "encouragement_level": "high" if scores["final_score"] < 60 else "medium",
                "celebration_level": "high" if scores["final_score"] > 85 else "low",
                "emoji": "🎉" if scores["final_score"] > 85 else "💪" if scores["final_score"] > 60 else "🌟"
            }
        }

    def _generate_audio_example_text(self, chinglish_analysis: Dict) -> str:
        """为TTS生成示范发音文本"""
        if chinglish_analysis["total_issues"] == 0:
            return "Great job! Your pronunciation is improving."
        
        # 根据检测到的问题生成针对性示范
        issues = chinglish_analysis["issues"]
        if any(issue["type"] == "th_confusion" for issue in issues):
            return "Think about this: the thick thread through the thin cloth."
        elif any(issue["type"] == "r_l_confusion" for issue in issues):
            return "Really lovely red roses grow in the light."
        else:
            return "Practice makes perfect. Keep up the good work!"

    def _get_mouth_shape_guide(self, chinglish_analysis: Dict) -> List[Dict]:
        """获取口型指导"""
        guides = []
        for issue in chinglish_analysis.get("issues", []):
            if issue["type"] == "th_confusion":
                guides.append({
                    "phoneme": "TH",
                    "description": "舌尖轻触上下齿",
                    "image_url": "/static/mouth_shapes/th.png",
                    "animation": "tongue_between_teeth"
                })
            elif issue["type"] == "r_l_confusion":
                guides.append({
                    "phoneme": "R",
                    "description": "舌头后卷，不碰上颚",
                    "image_url": "/static/mouth_shapes/r.png",
                    "animation": "tongue_curl_back"
                })
        return guides


pronunciation_service = PronunciationService()
