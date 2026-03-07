@echo off
echo ========================================
echo SpeakMaster 挑战杯演示 - 环境设置
echo ========================================
echo.

REM 检查 Python 是否安装
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误: 未找到 Python
    echo 请先安装 Python 3.8+
    pause
    exit /b 1
)

echo ✓ Python 已安装
python --version
echo.

REM 检查是否已有虚拟环境
if exist "venv_tests" (
    echo ✓ 虚拟环境已存在
) else (
    echo 创建虚拟环境...
    python -m venv venv_tests
    if %errorlevel% neq 0 (
        echo ❌ 虚拟环境创建失败
        pause
        exit /b 1
    )
    echo ✓ 虚拟环境创建成功
)
echo.

echo 激活虚拟环境...
call venv_tests\Scripts\activate.bat
if %errorlevel% neq 0 (
    echo ❌ 虚拟环境激活失败
    pause
    exit /b 1
)
echo ✓ 虚拟环境已激活
echo.

echo 安装/更新依赖...
pip install -r requirements.txt -q
if %errorlevel% neq 0 (
    echo ❌ 依赖安装失败
    pause
    exit /b 1
)
echo ✓ 依赖安装完成
echo.

echo ========================================
echo 开始运行演示
echo ========================================
echo.

REM 运行技术演示
echo [1/2] 运行技术演示...
python challenge_cup_demo.py
if %errorlevel% neq 0 (
    echo ❌ 技术演示失败
    pause
    exit /b 1
)
echo ✓ 技术演示完成
echo.

REM 生成商业指标
echo [2/2] 生成商业指标...
python business_metrics_generator.py
if %errorlevel% neq 0 (
    echo ❌ 商业指标生成失败
    pause
    exit /b 1
)
echo ✓ 商业指标生成完成
echo.

echo ========================================
echo 所有演示完成！
echo ========================================
echo.
echo 生成的文件：
echo   ✓ challenge_cup_demo_report.json
echo   ✓ business_metrics.xlsx
echo   ✓ business_metrics.json
echo   ✓ business_charts/
echo.
echo 这些文件可用于：
echo   • 商业计划书
echo   • 路演PPT
echo   • 答辩材料
echo.

REM 询问是否打开结果文件夹
echo 是否打开结果文件夹? (Y/N)
set /p open_folder=
if /i "%open_folder%"=="Y" (
    start .
)

pause
