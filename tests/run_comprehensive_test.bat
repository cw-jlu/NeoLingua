@echo off
chcp 65001 >nul
echo ========================================
echo   SpeakMaster 综合功能测试
echo ========================================
echo.
echo 此测试将验证:
echo   1. 基础API接口
echo   2. 高级功能 (中式英语检测、实时交互、口语评估、补全帮助)
echo   3. 核心技术 (纠错反馈、非母语鲁棒性、全双工交互、评估体系)
echo   4. 并发性能 (10/50/100并发)
echo.
echo 请确保以下服务正在运行:
echo   - API Gateway (8080)
echo   - AI Service (8089)
echo   - Analysis Service (8085)
echo.
pause

echo.
echo 正在运行综合测试...
echo.

cd /d "%~dp0"
python comprehensive_test.py

echo.
echo ========================================
echo   测试完成！
echo ========================================
echo.
echo 测试结果已保存到:
echo   - comprehensive_test_results.json
echo.
echo 查看详细报告:
echo   - COMPREHENSIVE_TEST_REPORT.md
echo   - TEST_SUMMARY.md
echo.
pause
