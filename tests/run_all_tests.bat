@echo off
echo ========================================
echo SpeakMaster 挑战杯测试套件
echo ========================================
echo.

echo [1/3] 检查依赖...
pip install -r requirements.txt -q
if %errorlevel% neq 0 (
    echo 依赖安装失败！
    pause
    exit /b 1
)
echo ✓ 依赖检查完成
echo.

echo [2/4] 运行技术演示...
python challenge_cup_demo.py
if %errorlevel% neq 0 (
    echo 技术演示失败！
    pause
    exit /b 1
)
echo ✓ 技术演示完成
echo.

echo [3/4] 生成商业指标...
python business_metrics_generator.py
if %errorlevel% neq 0 (
    echo 商业指标生成失败！
    pause
    exit /b 1
)
echo ✓ 商业指标生成完成
echo.

echo [4/4] 运行性能测试...
echo 注意：此步骤需要系统运行中...
echo 如果系统未启动，请按 Ctrl+C 取消
timeout /t 5
python performance_metrics_test.py
if %errorlevel% neq 0 (
    echo 性能测试失败（可能是系统未启动）
    echo 商业指标已成功生成，可以继续使用
)
echo.

echo ========================================
echo 测试完成！
echo ========================================
echo.
echo 生成的文件：
echo   - challenge_cup_demo_report.json (技术演示报告)
echo   - business_metrics.xlsx (商业数据表)
echo   - business_metrics.json (商业数据JSON)
echo   - business_charts/ (商业图表)
echo   - performance_metrics.xlsx (性能数据)
echo   - performance_charts/ (性能图表)
echo.
echo 这些文件可用于：
echo   ✓ 商业计划书
echo   ✓ 路演PPT
echo   ✓ 答辩材料
echo.
pause
