# QA Regression & Accessibility Testing Guide

## Overview
This guide covers comprehensive QA regression and accessibility testing for the OneCalculator app, including automated tests and manual TalkBack verification.

## Test Coverage

### 🔍 Search Filtering Tests
- **Test File**: `QARegressionTest.kt`
- **Coverage**:
  - Search input activation and text entry
  - Real-time search result filtering
  - Search with calculation history
  - Search with category names
  - View transitions during search
  - Search result interaction

### 📱 Recent History Swipe Tests
- **Test File**: `QARegressionTest.kt`
- **Coverage**:
  - Horizontal scroll performance on recent history
  - Left/right swipe gestures
  - History item interaction
  - Navigation back to calculator

### 🏠 Category Dialog Tests
- **Test File**: `QARegressionTest.kt`
- **Coverage**:
  - Category button interactions
  - Dialog opening and closing
  - Dialog content verification
  - Multiple calculator types

### ⚡ FAB Navigation Tests
- **Test File**: `QARegressionTest.kt`
- **Coverage**:
  - FAB visibility and accessibility
  - FAB click functionality
  - Navigation to main calculator
  - Scroll behavior (hide/show)

### ♿ Accessibility Tests
- **Test File**: `AccessibilityTest.kt`
- **Coverage**:
  - Content descriptions for all interactive elements
  - TalkBack announcements
  - Keyboard navigation
  - Minimum touch target sizes (48dp)
  - Screen reader compatibility
  - Focus management

### 🚀 Performance Tests
- **Test File**: `PerformanceTest.kt`
- **Coverage**:
  - Scroll performance (60 FPS verification)
  - Button responsiveness
  - Navigation transition times
  - Memory usage stability
  - UI thread blocking prevention

## Running Tests

### Automated Tests

#### Prerequisites
1. Android device or emulator connected
2. USB debugging enabled
3. Android SDK and ADB in PATH

#### Quick Start
```bash
# Run all tests
.\run_qa_tests.bat

# Run specific test categories
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.QARegressionTest
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.AccessibilityTest
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.PerformanceTest
```

#### Individual Test Execution
```bash
# Search filtering
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.QARegressionTest#testSearchFilteringFunctionality

# Recent history swipes
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.QARegressionTest#testRecentHistorySwipeFunctionality

# Category dialogs
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.QARegressionTest#testCategoryDialogFunctionality

# FAB navigation
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.QARegressionTest#testFABNavigationFunctionality

# Performance tests
gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shaadow.onecalculator.PerformanceTest#testScrollSmoothness
```

### Manual TalkBack Testing

#### Setup
1. **Enable TalkBack**:
   - Go to Settings > Accessibility > TalkBack
   - Turn on TalkBack
   - Complete the tutorial

2. **TalkBack Gestures**:
   - **Swipe right**: Next element
   - **Swipe left**: Previous element
   - **Double tap**: Activate element
   - **Swipe down then right**: Read from top
   - **Two-finger swipe up**: Read all

#### Testing Checklist

##### ✅ Calculator Screen
- [ ] All calculator buttons have meaningful content descriptions
- [ ] Number buttons announced as "Button 1", "Button 2", etc.
- [ ] Operation buttons announced as "Plus", "Minus", "Multiply", "Divide"
- [ ] Special buttons announced appropriately (AC, Backspace, Equals)
- [ ] Expression input field is focusable and readable
- [ ] Solution display announces calculation results
- [ ] Menu and History buttons have descriptions

##### ✅ Home Screen
- [ ] Search input is properly labeled and focusable
- [ ] "Recent Calculations" section header is announced
- [ ] Recent history cards are accessible and interactive
- [ ] Category section headers are announced ("Algebra", "Geometry", etc.)
- [ ] Category buttons have descriptive names
- [ ] FAB (Calculator button) has proper content description
- [ ] Bottom navigation items are accessible

##### ✅ Navigation
- [ ] Bottom navigation tabs are properly labeled
- [ ] Tab selection is announced
- [ ] Screen transitions are smooth with TalkBack
- [ ] Back navigation works correctly
- [ ] Focus is properly managed during navigation

##### ✅ Search Functionality
- [ ] Search input activation is announced
- [ ] Search results are navigable
- [ ] Result categories are clearly separated
- [ ] Search result selection is announced
- [ ] Search clearing returns focus appropriately

##### ✅ Dialogs and Popups
- [ ] Calculator dialogs have proper focus management
- [ ] Dialog content is accessible
- [ ] Dialog dismissal is clear
- [ ] Context menus are accessible

## Performance Criteria

### 📊 Frame Rate Standards
- **Target**: 60 FPS (16.67ms per frame)
- **Minimum Acceptable**: 55 FPS
- **Test Areas**:
  - Home screen scrolling
  - Recent history horizontal scrolling
  - Search results scrolling
  - Navigation transitions

### ⚡ Responsiveness Standards
- **Button Response**: < 100ms average, < 200ms maximum
- **Navigation Transitions**: < 1.5 seconds
- **Search Results**: < 500ms
- **UI Thread Blocking**: < 3 seconds for complex operations

### 💾 Memory Standards
- **Memory Growth**: < 50MB during normal operations
- **Garbage Collection**: Minimal impact on performance
- **Memory Leaks**: None detected during extended usage

## Accessibility Standards

### 🎯 WCAG 2.1 AA Compliance
- **Perceivable**: All content has text alternatives
- **Operable**: All functionality keyboard accessible
- **Understandable**: Clear and consistent navigation
- **Robust**: Compatible with assistive technologies

### 📏 Touch Target Standards
- **Minimum Size**: 48dp x 48dp
- **Recommended**: 44pt x 44pt (iOS standard)
- **Spacing**: Adequate spacing between interactive elements

### 🔊 Content Description Standards
- **Interactive Elements**: All buttons, links, controls
- **Images**: Meaningful images have descriptions
- **State Changes**: Dynamic content changes announced
- **Error Messages**: Clear and descriptive

## Test Reports

### 📋 Report Locations
- **Automated Test Reports**: `app/build/reports/androidTests/connected/`
- **Coverage Reports**: `app/build/reports/coverage/`
- **Performance Reports**: `app/build/reports/performance/`

### 📊 Report Contents
- Test execution results
- Performance metrics
- Accessibility violations
- Screenshots and videos
- Device information

## Common Issues and Solutions

### 🐛 Common Test Failures

#### Search Not Working
- **Cause**: Search input not properly focused
- **Solution**: Add explicit click before typing text
- **Test**: Verify search input has focus indicator

#### Swipe Gestures Failing
- **Cause**: Insufficient swipe distance or speed
- **Solution**: Adjust swipe parameters in ViewActions
- **Test**: Verify on different screen sizes

#### Content Descriptions Missing
- **Cause**: Views not properly configured
- **Solution**: Add contentDescription attributes to XML or setContentDescription in code
- **Test**: Run accessibility scanner

#### Performance Issues
- **Cause**: Heavy operations on UI thread
- **Solution**: Move to background threads, optimize layouts
- **Test**: Profile with Android Studio

### 🔧 Debugging Tips

#### TalkBack Issues
1. Check if contentDescription is set
2. Verify view is focusable
3. Test with TalkBack developer settings
4. Use Accessibility Scanner app

#### Performance Issues
1. Use Android Studio Profiler
2. Enable GPU rendering profile
3. Check for memory leaks
4. Optimize layout hierarchy

## Continuous Integration

### 🔄 CI Pipeline Integration
```yaml
# Example GitHub Actions workflow
- name: Run QA Tests
  run: |
    ./gradlew assembleDebug assembleDebugAndroidTest
    ./gradlew connectedAndroidTest
    
- name: Generate Reports
  run: |
    ./gradlew jacocoTestReport
    ./gradlew sonarqube
```

### 📈 Quality Gates
- All accessibility tests must pass
- Performance tests must meet criteria
- Code coverage > 80%
- No critical accessibility violations

## Best Practices

### ✅ Test Development
1. **Maintainable**: Use page object pattern
2. **Reliable**: Add appropriate waits and retries
3. **Fast**: Minimize unnecessary delays
4. **Comprehensive**: Cover happy path and edge cases

### ✅ Accessibility
1. **Early Integration**: Test accessibility during development
2. **Real Users**: Test with actual users with disabilities
3. **Multiple Tools**: Use automated tools + manual testing
4. **Continuous**: Regular accessibility audits

### ✅ Performance
1. **Baseline**: Establish performance baselines
2. **Continuous**: Monitor performance over time
3. **Real Devices**: Test on actual devices, not just emulators
4. **Various Conditions**: Test under different network/battery conditions

## Resources

### 📚 Documentation
- [Android Accessibility Guide](https://developer.android.com/guide/topics/ui/accessibility)
- [TalkBack User Guide](https://support.google.com/accessibility/android/answer/6283677)
- [Espresso Testing Guide](https://developer.android.com/training/testing/espresso)
- [Performance Testing Guide](https://developer.android.com/training/testing/performance)

### 🛠️ Tools
- **Accessibility Scanner**: Google Play Store
- **TalkBack**: Built-in Android accessibility service
- **Android Studio Profiler**: Performance analysis
- **Espresso**: UI testing framework
- **UIAutomator**: Cross-app UI testing

---

## Conclusion

This comprehensive testing suite ensures the OneCalculator app meets high standards for:
- ✅ Functional regression testing
- ✅ Accessibility compliance (TalkBack, content descriptions)
- ✅ Performance standards (60 FPS, responsiveness)
- ✅ User experience quality

Regular execution of these tests will maintain app quality and accessibility for all users.
