import sys
import json
import logging
import shutil
import os
import subprocess
from pathlib import Path

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def main():
    """
    独立运行的 MFA Worker 脚本。
    参数通过命令行传入（JSON 字符串）。
    """
    try:
        if len(sys.argv) < 2:
            print(json.dumps({"error": "No arguments provided"}))
            sys.exit(1)

        args = json.loads(sys.argv[1])
        
        audio_path = Path(args.get("audio_path"))
        text = args.get("text")
        output_dir = Path(args.get("output_dir"))
        dictionary_path = args.get("dictionary_path")
        acoustic_model_path = args.get("acoustic_model_path")
        mfa_cmd = args.get("mfa_cmd", "mfa")

        if not audio_path.exists():
            print(json.dumps({"error": f"Audio file not found: {audio_path}"}))
            sys.exit(1)

        # 1. 准备输入目录
        input_dir = output_dir / "input"
        input_dir.mkdir(parents=True, exist_ok=True)
        
        # 转换音频为 MFA 要求的 WAV 格式（16kHz, 单声道, 16bit PCM）
        # MFA 只支持标准 WAV，不支持 webm/opus 等格式
        target_wav = input_dir / "sample.wav"
        source_ext = audio_path.suffix.lower()
        
        if source_ext != ".wav":
            # 使用 ffmpeg 转换格式
            # 优先使用传入的 ffmpeg 绝对路径，其次从 PATH 查找
            ffmpeg_cmd = args.get("ffmpeg_path") or shutil.which("ffmpeg")
            if not ffmpeg_cmd:
                print(json.dumps({"error": "ffmpeg not found, cannot convert audio to WAV"}))
                sys.exit(1)
            
            convert_cmd = [
                ffmpeg_cmd, "-y",           # 覆盖输出
                "-i", str(audio_path),      # 输入文件
                "-ar", "16000",             # 采样率 16kHz
                "-ac", "1",                 # 单声道
                "-sample_fmt", "s16",       # 16bit PCM
                str(target_wav)
            ]
            logger.info(f"Converting audio: {source_ext} -> WAV")
            conv_result = subprocess.run(
                convert_cmd,
                capture_output=True,
                text=True,
                encoding='utf-8',
                errors='replace'
            )
            if conv_result.returncode != 0:
                logger.error(f"ffmpeg conversion failed: {conv_result.stderr}")
                print(json.dumps({"error": f"Audio conversion failed: {conv_result.stderr[:500]}"}))
                sys.exit(1)
            logger.info(f"Audio converted successfully: {target_wav}")
        else:
            # 已经是 WAV，直接复制
            shutil.copy(audio_path, target_wav)
        
        # 写入对应的文本文件（MFA 要求同名 .txt）
        with open(input_dir / "sample.txt", "w", encoding="utf-8") as f:
            f.write(text)

        # 2. 构建 MFA 命令
        # 注意：这里我们只负责构造参数，实际执行由 subprocess 在外部完成，或者我们在这里调用 mfa 命令行
        # 由于我们是在 MFA 环境中运行，理论上可以直接调用 mfa 命令
        
        align_output_dir = output_dir / "output"
        
        cmd = [
            mfa_cmd, "align",
            str(input_dir),
            dictionary_path,
            acoustic_model_path,
            str(align_output_dir),
            "--clean", "--overwrite",
            "--single_speaker",   # 单说话人模式，避免不必要的聚类
            "--num_jobs", "1",    # 单进程执行，防止 subprocess 中多进程死锁
            "--verbose"           # 获取更多日志
        ]
        
        logger.info(f"Executing MFA: {' '.join(cmd)}")
        
        # 3. 执行 MFA
        # 设置环境变量，确保 MFA 能找到 conda 环境中的依赖
        env = os.environ.copy()
        
        # 从 mfa_cmd 路径推导 conda 环境根目录
        # 例如: D:\...\mfa_env\Scripts\mfa.exe -> D:\...\mfa_env
        mfa_exe = Path(mfa_cmd)
        if mfa_exe.is_absolute():
            conda_env_root = mfa_exe.parent.parent  # Scripts/mfa.exe -> mfa_env
            env["CONDA_PREFIX"] = str(conda_env_root)
            # 确保 conda 环境的 Library\bin 在 PATH 中（Windows 上 MFA 依赖的 kaldi 等工具在这里）
            library_bin = conda_env_root / "Library" / "bin"
            scripts_dir = conda_env_root / "Scripts"
            env["PATH"] = f"{scripts_dir};{library_bin};{conda_env_root};{env.get('PATH', '')}"
            logger.info(f"Set CONDA_PREFIX={conda_env_root}")
        
        # 防止 MFA/numpy/openblas 多线程死锁（在 subprocess 中常见）
        env["OPENBLAS_NUM_THREADS"] = "1"
        env["MKL_NUM_THREADS"] = "1"
        env["OMP_NUM_THREADS"] = "1"
        env["NUMEXPR_NUM_THREADS"] = "1"
        # MFA 3.x 使用 --single_speaker 或 num_jobs=1 来避免多进程问题
        # 我们通过命令行参数 --num_jobs 1 来控制
        
        logger.info(f"MFA command: {cmd}")
        logger.info(f"Starting MFA execution (timeout=120s)...")
        
        try:
            result = subprocess.run(
                cmd, 
                capture_output=True, 
                text=True, 
                encoding='utf-8',
                errors='replace',  # 防止编码错误
                env=env,
                timeout=120  # 最多等待120秒，防止MFA卡死
            )
        except subprocess.TimeoutExpired:
            logger.error("MFA execution timed out after 120 seconds")
            print(json.dumps({"error": "MFA execution timed out (120s)"}))
            sys.exit(1)
        
        logger.info(f"MFA return code: {result.returncode}")
        logger.info(f"MFA stdout length: {len(result.stdout)}, stderr length: {len(result.stderr)}")
        
        if result.stdout:
            logger.info(f"MFA stdout: {result.stdout[:2000]}")
        if result.stderr:
            logger.info(f"MFA stderr: {result.stderr[:2000]}")
        
        if result.returncode != 0:
            print(json.dumps({"error": "MFA execution failed", "stderr": result.stderr[:1000], "stdout": result.stdout[:1000]}))
            sys.exit(1)

        # 4. 检查结果
        tg_files = list(align_output_dir.glob("*.TextGrid"))
        if not tg_files:
            print(json.dumps({"error": "No TextGrid generated"}))
            sys.exit(1)

        # 返回生成的 TextGrid 路径
        print(json.dumps({"success": True, "tg_path": str(tg_files[0])}))

    except Exception as e:
        logger.exception("Unexpected error in MFA worker")
        print(json.dumps({"error": str(e)}))
        sys.exit(1)

if __name__ == "__main__":
    main()
