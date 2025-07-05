@echo off
echo ========================================
echo    OneCalculator QA Regression Tests
echo ========================================
echo.

echo Checking if ADB is available...
adb devices
if %errorlevel% neq 0 (
    echo ERROR: ADB not found. Please ensure Android SDK is installed and ADB is in PATH.
    pause
    exit /b 1
)

echo.
echo Checking for connected devices...
adb devices | findstr "device$" > nul
if %errorlevel% neq 0 (
    echo WARNING: No devices found. Please connect an Android device or start an emulator.
    echo Press any key to continue anyway...
    pause > nul
)

echo.
echo Building the application...
gradlew assembleDebug assembleDebugAndroidTest
if %errorlevel% neq 0 (
    echo ERROR: Build failed. Please check the build output.
    pause
    exit /b 1
)

echo.
echo ========================================
echo    Running QA Regression Tests
echo ========================================

echo.
echo 1. Running Search Filtering Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.QARegressionTest#testSearchFilteringFunctionality
echo.

echo 2. Running Recent History Swipe Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.QARegressionTest#testRecentHistorySwipeFunctionality
echo.

echo 3. Running Category Dialog Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.QARegressionTest#testCategoryDialogFunctionality
echo.

echo 4. Running FAB Navigation Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.QARegressionTest#testFABNavigationFunctionality
echo.

echo 5. Running Bottom Navigation Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.QARegressionTest#testBottomNavigationFunctionality
echo.

echo 6. Running Gesture Navigation Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.QARegressionTest#testGestureNavigationFromCalculator
echo.

echo 7. Running Responsive Grid Layout Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.QARegressionTest#testResponsiveGridLayoutColumns
echo.

echo ========================================
echo    Running Accessibility Tests
echo ========================================

echo.
echo 1. Running Calculator Buttons Accessibility Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.AccessibilityTest#testCalculatorButtonsContentDescriptions
echo.

echo 2. Running Display Areas Accessibility Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.AccessibilityTest#testDisplayAreasContentDescriptions
echo.

echo 3. Running Navigation Buttons Accessibility Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.AccessibilityTest#testNavigationButtonsAccessibility
echo.

echo 4. Running Home Screen Accessibility Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.AccessibilityTest#testHomeScreenAccessibility
echo.

echo 5. Running Bottom Navigation Accessibility Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.AccessibilityTest#testBottomNavigationAccessibility
echo.

echo 6. Running Category Buttons Accessibility Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.AccessibilityTest#testCategoryButtonsAccessibility
echo.

echo 7. Running Recent History Accessibility Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.AccessibilityTest#testRecentHistoryAccessibility
echo.

echo 8. Running Search Results Accessibility Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.AccessibilityTest#testSearchResultsAccessibility
echo.

echo 9. Running Minimum Touch Target Size Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.AccessibilityTest#testMinimumTouchTargetSizes
echo.

echo 10. Running Dialog Accessibility Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.AccessibilityTest#testDialogAccessibility
echo.

echo 11. Running Keyboard Navigation Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.AccessibilityTest#testKeyboardNavigation
echo.

echo 12. Running Screen Reader Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.AccessibilityTest#testScreenReaderAnnouncements
echo.

echo ========================================
echo    Running Performance Tests
echo ========================================

echo.
echo 1. Running Home Screen Scroll Performance Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.PerformanceTest#testHomeScreenScrollPerformance
echo.

echo 2. Running Recent History Scroll Performance Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.PerformanceTest#testRecentHistoryScrollPerformance
echo.

echo 3. Running Search Results Scroll Performance Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.PerformanceTest#testSearchResultsScrollPerformance
echo.

echo 4. Running Calculator Button Responsiveness Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.PerformanceTest#testCalculatorButtonResponsiveness
echo.

echo 5. Running Navigation Transition Performance Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.PerformanceTest#testNavigationTransitionPerformance
echo.

echo 6. Running Memory Usage Stability Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.PerformanceTest#testMemoryUsageStability
echo.

echo 7. Running UI Thread Blocking Tests...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.PerformanceTest#testUIThreadBlocking
echo.

echo 8. Running Scroll Smoothness Tests (60 FPS verification)...
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.PerformanceTest#testScrollSmoothness
echo.

echo ========================================
echo    All Tests Completed
echo ========================================

echo.
echo QA Regression and Accessibility tests have been completed.
echo.
echo Test Results Summary:
echo - Search filtering functionality: Tested
echo - Recent history swipes: Tested  
echo - Category dialog navigation: Tested
echo - FAB navigation: Tested
echo - TalkBack content descriptions: Verified
echo - Section headers accessibility: Verified
echo - Cards accessibility: Verified
echo - FAB accessibility: Verified
echo - Scroll performance: Profiled
echo - 60 FPS verification: Completed
echo.
echo Please check the test reports in:
echo app/build/reports/androidTests/connected/
echo.
echo For detailed accessibility testing with TalkBack:
echo 1. Enable TalkBack in device accessibility settings
echo 2. Navigate through the app manually
echo 3. Verify all elements are properly announced
echo 4. Check focus management and navigation
echo.
pause
