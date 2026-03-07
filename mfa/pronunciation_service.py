import json
import logging
import statistics
import subprocess
import shutil
import tempfile
import os
import re
from pathlib import Path
from typing import Dict, List, Optional, Any, Tuple

# Try to import praatio, if not available, some features will fail
try:
    from praatio import textgrid
except ImportError:
    textgrid = None

logger = logging.getLogger(__name__)

class PronunciationService:
    """
    Service for pronunciation assessment and feedback generation.
    """

    def __init__(self):
        self.phoneme_tips = {
            "TH": "舌尖需要伸到上下齿之间，出气而不是爆破。",
            "DH": "像 TH，但要振动声带，可以摸喉咙感受震动。",
            "R":  "舌头向后卷但不要碰到上颚，嘴唇微微收圆。",
            "L":  "舌尖要顶住上齿龈，不能含糊带过。",
            "W":  "嘴唇要明显圆起来，不要弱化成元音。",
            "AE": "嘴巴张大，像中文“啊”，不要读成 E。",
            "IY": "嘴角拉开，保持紧张度，不要太短。",
            "ER": "卷舌 + 中央元音，注意不要读成“额”。",
            "UW": "嘴唇收圆、向前噘，注意长度。",
        }
        
        # Configuration for MFA (can be moved to settings)
        self.mfa_cmd = "mfa"  # Assumes mfa is in PATH
        
        # 使用本地的 MFA 模型路径
        # 假设 mfa 文件夹在项目根目录下
        self.project_root = Path(__file__).resolve().parents[2]
        self.mfa_models_dir = self.project_root / "mfa"
        
        # 字典文件 (单一文件)
        self.dictionary_path = self.mfa_models_dir / "english_us_arpa.dict"
        
        # 声学模型 (目录结构)
        # MFA 2.x 可以直接使用解压后的目录作为模型路径
        # 注意：这里我们指向包含 meta.json 的最内层目录
        self.acoustic_model_path = self.mfa_models_dir / "english_us_arpa" / "english_us_arpa"
        
        # 检查模型是否存在
        if not self.dictionary_path.exists():
             logger.error(f"Critical: MFA dictionary not found at {self.dictionary_path}")
             
        if not (self.acoustic_model_path / "meta.json").exists():
             logger.error(f"Critical: MFA acoustic model meta.json not found at {self.acoustic_model_path}")

    def _clean_text(self, text: str) -> str:
        """
        Clean text for MFA:
        - lowercase
        - remove punctuation
        - keep only a-z and space
        """
        if not text:
            return ""
        text = text.lower()
        # Keep apostrophe for words like "don't"
        text = re.sub(r"[^a-z\s']", "", text)
        text = re.sub(r"\s+", " ", text).strip()
        return text

    def analyze(self, audio_path: Path, text: str) -> Dict[str, Any]:
        """
        Perform full pronunciation analysis.
        
        Args:
            audio_path: Path to the audio file.
            text: The text corresponding to the audio.
            
        Returns:
            Dict containing scores and feedback.
        """
        if not textgrid:
            logger.warning("praatio not installed, skipping pronunciation analysis")
            return self._get_dummy_result()

        try:
            # 1. Align Audio (Get TextGrid)
            tg_path = self.align_audio(audio_path, text)
            if not tg_path:
                return self._get_dummy_result()

            # 2. Parse TextGrid
            phonemes = self.parse_textgrid(tg_path)
            if not phonemes:
                return self._get_dummy_result()

            # 3. Score Pronunciation
            scores = self.score_pronunciation(phonemes)

            # 4. Generate Feedback
            feedback = self.generate_feedback(phonemes, scores)

            return {
                "pronunciation_score": scores["final_score"],
                "detailed_scores": scores,
                "feedback": feedback
            }

        except Exception as e:
            logger.error(f"Pronunciation analysis failed: {e}", exc_info=True)
            return self._get_dummy_result()

    def _get_dummy_result(self):
        """Return a safe fallback result."""
        return {
            "pronunciation_score": 0,
            "detailed_scores": {
                "total_phonemes": 0,
                "duration_score": 0,
                "stress_score": 0,
                "completeness_score": 0,
                "final_score": 0
            },
            "feedback": "Pronunciation analysis unavailable."
        }

    def align_audio(self, audio_path: Path, text: str) -> Optional[Path]:
        """
        Runs MFA alignment by invoking a separate script in a dedicated Conda environment.
        This isolates the heavy dependencies of MFA from the main application.
        """
        # 1. Clean Text
        cleaned_text = self._clean_text(text)
        if not cleaned_text:
            logger.warning("Text is empty after cleaning, skipping alignment")
            return None

        # 2. Prepare MFA Execution
        # 优先从 settings 配置读取 MFA Python 路径，其次从环境变量
        from app.config import settings as app_settings
        mfa_python_path = getattr(app_settings, 'mfa_python_path', None) or os.getenv("MFA_PYTHON_PATH")
        
        # Determine how to run MFA
        run_cmd_base = []
        
        # 初始化 worker_args，但暂时不设置 output_dir（因为 temp_path 还没创建）
        worker_args = {
            "audio_path": str(audio_path),
            "text": cleaned_text,
            "output_dir": "",  # 占位，后面会更新
            "dictionary_path": str(self.dictionary_path),
            "acoustic_model_path": str(self.acoustic_model_path),
            "mfa_cmd": "mfa" # Default, will be overridden below
        }

        if mfa_python_path and os.path.exists(mfa_python_path):
             # Use the dedicated python to run the worker script
             worker_script = Path(__file__).parent / "mfa_worker_script.py"
             run_cmd_base = [mfa_python_path, str(worker_script)]
             logger.info(f"Using dedicated MFA environment: {mfa_python_path}")
             
             # 从 MFA Python 路径推导同环境下的可执行文件路径
             mfa_env_root = Path(mfa_python_path).parent
             
             # 推导 MFA 可执行文件路径
             mfa_exe_path = mfa_env_root / "Scripts" / "mfa.exe"
             if mfa_exe_path.exists():
                 worker_args["mfa_cmd"] = str(mfa_exe_path)
                 logger.info(f"Using explicit MFA executable: {mfa_exe_path}")
             else:
                 mfa_exe_path_bin = mfa_env_root / "bin" / "mfa"
                 if mfa_exe_path_bin.exists():
                     worker_args["mfa_cmd"] = str(mfa_exe_path_bin)
                 else:
                     logger.warning(f"Could not find mfa executable relative to {mfa_python_path}, falling back to 'mfa' in PATH")
             
             # 推导 ffmpeg 可执行文件路径（conda 环境中 ffmpeg 通常在 Library/bin 下）
             ffmpeg_path = mfa_env_root / "Library" / "bin" / "ffmpeg.exe"
             if not ffmpeg_path.exists():
                 # Linux/macOS 备用路径
                 ffmpeg_path = mfa_env_root / "bin" / "ffmpeg"
             if ffmpeg_path.exists():
                 worker_args["ffmpeg_path"] = str(ffmpeg_path)
                 logger.info(f"Using explicit ffmpeg: {ffmpeg_path}")
             else:
                 logger.warning(f"Could not find ffmpeg in MFA environment, worker will try system PATH")

        else:
             # Fallback: Try to use 'mfa' command directly if available in PATH
            # This is for when the user has installed mfa in the current environment or system path
            if not shutil.which("mfa"):
                logger.warning("MFA command not found and MFA_PYTHON_PATH not set. Skipping alignment.")
                return None
            
            # Even if we use 'mfa' command, we wrap it in our worker script for consistent argument handling?
            # No, the worker script is designed to be run BY python.
            # If we don't have a separate python, we can run the worker script with the CURRENT python
            # IF the current python has mfa installed.
            import sys
            worker_script = Path(__file__).parent / "mfa_worker_script.py"
            run_cmd_base = [sys.executable, str(worker_script)]
            logger.info("Using current Python environment for MFA")

        # 3. Create Temp Directory for this job
        with tempfile.TemporaryDirectory() as temp_dir:
            temp_path = Path(temp_dir)
            # Worker script expects 'output_dir' where it will create 'input' and 'output' subdirs
            
            # 更新 worker_args 中的 output_dir（现在 temp_path 已经定义了）
            worker_args["output_dir"] = str(temp_path)
            
            try:
                # Pass arguments as a JSON string
                cmd = run_cmd_base + [json.dumps(worker_args)]
                
                logger.info(f"Invoking MFA worker script: {' '.join(str(c)[:80] for c in cmd[:3])}")
                
                # 构建环境变量，确保 mfa_env 的路径优先
                env = os.environ.copy()
                if mfa_python_path and os.path.exists(mfa_python_path):
                    mfa_env_root = Path(mfa_python_path).parent
                    # 将 mfa_env 的关键路径加入 PATH
                    extra_paths = [
                        str(mfa_env_root / "Scripts"),
                        str(mfa_env_root / "Library" / "bin"),
                        str(mfa_env_root),
                    ]
                    env["PATH"] = ";".join(extra_paths) + ";" + env.get("PATH", "")
                    env["CONDA_PREFIX"] = str(mfa_env_root)
                
                try:
                    result = subprocess.run(
                        cmd, 
                        capture_output=True, 
                        text=True, 
                        encoding='utf-8',
                        errors='replace',
                        env=env,
                        timeout=150  # 最多等待150秒（比worker内部的120秒多一点余量）
                    )
                except subprocess.TimeoutExpired:
                    logger.error("MFA worker script timed out after 150 seconds")
                    return None
                
                if result.returncode != 0:
                    # 打印完整的 stderr 和 stdout，方便排查
                    logger.error(f"MFA worker failed:\n{result.stderr}\n{result.stdout}")
                    return None
                
                # Parse output from worker
                # The worker prints a JSON object to stdout
                try:
                    output_data = json.loads(result.stdout.strip())
                    if "error" in output_data:
                        logger.error(f"MFA worker reported error: {output_data['error']}")
                        return None
                    
                    tg_path_str = output_data.get("tg_path")
                    if tg_path_str and os.path.exists(tg_path_str):
                        # Copy to persistent temp file in MAIN environment
                        fd, persistent_tg_path = tempfile.mkstemp(suffix=".TextGrid")
                        os.close(fd)
                        shutil.copy(tg_path_str, persistent_tg_path)
                        return Path(persistent_tg_path)
                        
                except json.JSONDecodeError:
                    logger.error(f"Failed to parse MFA worker output: {result.stdout}")
                    return None

            except Exception as e:
                logger.error(f"Error executing MFA worker: {e}")
                return None
        
        return None

    def parse_textgrid(self, tg_path: Path) -> List[Dict]:
        """
        Parse TextGrid file to get phoneme timeline.
        """
        try:
            # Check if praatio is available
            if not textgrid:
                logger.error("praatio library not available")
                return []

            tg = textgrid.openTextgrid(str(tg_path), includeEmptyIntervals=True)
            
            phone_tier = None
            for tier in tg.tiers:
                if tier.name.lower() == "phones":
                    phone_tier = tier
                    break
            
            if phone_tier is None:
                # Try to find any interval tier if 'phones' not found
                # praatio 6.x uses .tiers as list of tier objects
                if len(tg.tiers) > 0:
                     phone_tier = tg.tiers[0]
                else:
                    logger.error("No tiers found in TextGrid")
                    return []

            phonemes = []
            for interval in phone_tier.entries:
                label = interval.label.strip()
                if label == "" or label.lower() in {"sil", "sp", ""}:
                    continue
                
                phonemes.append({
                    "phoneme": label,
                    "start": round(interval.start, 3),
                    "end": round(interval.end, 3),
                    "duration": round(interval.end - interval.start, 3)
                })
            
            # Clean up the temp file if it was created by align_audio
            try:
                os.remove(tg_path)
            except:
                pass
            
            return phonemes
            
        except Exception as e:
            logger.error(f"Error parsing TextGrid: {e}")
            return []

    def score_pronunciation(self, phonemes: List[Dict]) -> Dict[str, Any]:
        """
        基于音素时间线计算发音评分。
        
        评分维度：
        1. 节奏/时长分 (40%) - 基于音素时长的变异系数，越接近母语者越好
        2. 重音分 (30%) - 基于重音标记的比例和分布
        3. 完整度分 (30%) - 基于是否有吞音（过短音素）
        """
        total_phonemes = len(phonemes)
        if total_phonemes == 0:
            return {
                "total_phonemes": 0,
                "duration_score": 0,
                "stress_score": 0,
                "completeness_score": 0,
                "final_score": 0
            }

        # 1. 节奏/时长评分
        # 使用变异系数（CV = std/mean）衡量节奏稳定性
        # 母语者的 CV 通常在 0.4-0.7 之间（英语是重音计时语言，有一定变化）
        durations = [p["duration"] for p in phonemes if p["duration"] > 0]
        if len(durations) >= 2:
            mean_dur = statistics.mean(durations)
            std_dur = statistics.stdev(durations)
            cv = std_dur / mean_dur if mean_dur > 0 else 0
            
            # 语速评分：正常语速 0.05-0.12s/音素
            if 0.05 <= mean_dur <= 0.12:
                speed_score = 100
            elif mean_dur < 0.05:
                # 太快，可能吞音
                speed_score = max(40, 100 - (0.05 - mean_dur) * 2000)
            else:
                # 太慢，可能犹豫
                speed_score = max(40, 100 - (mean_dur - 0.12) * 500)
            
            # 节奏评分：CV 在 0.3-0.8 之间最好（英语有自然的重音变化）
            if 0.3 <= cv <= 0.8:
                rhythm_score = 95
            elif cv < 0.3:
                # 太平，像机器人
                rhythm_score = max(50, 95 - (0.3 - cv) * 200)
            else:
                # 太不稳定
                rhythm_score = max(50, 95 - (cv - 0.8) * 150)
            
            duration_score = speed_score * 0.5 + rhythm_score * 0.5
        else:
            duration_score = 50.0

        # 2. 重音评分
        # 检查元音是否带有重音标记（0=无重音, 1=主重音, 2=次重音）
        vowels_with_stress = [p for p in phonemes if any(c.isdigit() for c in p["phoneme"])]
        consonants = [p for p in phonemes if not any(c.isdigit() for c in p["phoneme"])]
        
        if total_phonemes > 0 and len(vowels_with_stress) > 0:
            # 主重音元音应该比无重音元音更长
            primary_stressed = [p for p in vowels_with_stress if "1" in p["phoneme"]]
            unstressed = [p for p in vowels_with_stress if "0" in p["phoneme"]]
            
            if primary_stressed and unstressed:
                avg_stressed_dur = statistics.mean([p["duration"] for p in primary_stressed])
                avg_unstressed_dur = statistics.mean([p["duration"] for p in unstressed])
                
                # 重音元音应该比非重音元音长 1.2-2.0 倍
                if avg_unstressed_dur > 0:
                    stress_ratio = avg_stressed_dur / avg_unstressed_dur
                    if 1.2 <= stress_ratio <= 2.5:
                        stress_score = 90 + min(10, (stress_ratio - 1.2) * 10)
                    elif stress_ratio < 1.2:
                        # 重音不够明显
                        stress_score = max(55, 90 - (1.2 - stress_ratio) * 100)
                    else:
                        # 重音过度
                        stress_score = max(60, 90 - (stress_ratio - 2.5) * 30)
                else:
                    stress_score = 70
            else:
                # 有重音标记但缺少对比
                stress_score = 75
        else:
            stress_score = 60

        # 3. 完整度评分
        # 检查过短音素（可能是吞音）和过长停顿
        very_short = [p for p in phonemes if p["duration"] < 0.02]
        very_long = [p for p in phonemes if p["duration"] > 0.3]
        
        short_penalty = len(very_short) * 5  # 每个吞音扣5分
        long_penalty = len(very_long) * 3    # 每个过长停顿扣3分
        completeness_score = max(30.0, 100 - short_penalty - long_penalty)

        # 最终加权评分
        final_score = (
            0.4 * duration_score +
            0.3 * stress_score +
            0.3 * completeness_score
        )

        return {
            "total_phonemes": total_phonemes,
            "duration_score": round(duration_score, 1),
            "stress_score": round(stress_score, 1),
            "completeness_score": round(completeness_score, 1),
            "final_score": round(final_score, 1)
        }

    def _normalize_phoneme(self, p: str) -> str:
        """Remove stress digits (e.g., AH0 -> AH)"""
        return ''.join([c for c in p if not c.isdigit()])

    def generate_feedback(self, phonemes: List[Dict], scores: Dict) -> str:
        """
        生成自然语言反馈建议。
        根据评分维度给出具体的改进方向。
        """
        feedback_lines = []
        final = scores['final_score']
        
        # 总体评价
        if final >= 85:
            feedback_lines.append(f"总体得分：{final}/100 — 发音不错，继续保持！")
        elif final >= 70:
            feedback_lines.append(f"总体得分：{final}/100 — 还不错，有一些可以改进的地方。")
        else:
            feedback_lines.append(f"总体得分：{final}/100 — 需要多加练习，注意以下建议。")

        # 1. 节奏/语速反馈
        durations = [p["duration"] for p in phonemes if p["duration"] > 0]
        if durations:
            mean_dur = statistics.mean(durations)
            if mean_dur < 0.05:
                feedback_lines.append("⚡ 语速偏快，试着放慢一点，让每个音素都发清楚。")
            elif mean_dur > 0.12:
                feedback_lines.append("🐢 语速偏慢，可以尝试更流畅地连读。")

        # 2. 吞音检测
        short = [p for p in phonemes if p["duration"] < 0.02]
        if short:
            short_phonemes = [self._normalize_phoneme(p["phoneme"]) for p in short[:3]]
            feedback_lines.append(f"🔇 检测到 {len(short)} 个音素过短（{', '.join(short_phonemes)}），可能存在吞音。")

        # 3. 重音反馈
        if scores.get("stress_score", 0) < 70:
            feedback_lines.append("🎵 重音不够明显，英语中重读音节应该更长、更响亮。")

        # 4. 具体音素建议（最多3条）
        phoneme_counts = {}
        for p in phonemes:
            base = self._normalize_phoneme(p["phoneme"])
            phoneme_counts[base] = phoneme_counts.get(base, 0) + 1
            
        tips_added = 0
        for phoneme, count in phoneme_counts.items():
            if phoneme in self.phoneme_tips and tips_added < 3:
                feedback_lines.append(f"💬 /{phoneme}/: {self.phoneme_tips[phoneme]}")
                tips_added += 1

        return "\n".join(feedback_lines)

# Global instance
pronunciation_service = PronunciationService()