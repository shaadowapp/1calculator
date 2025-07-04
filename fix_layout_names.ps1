# PowerShell script to fix layout names in fragment files

$baseDir = "C:\Users\surya\AndroidStudioProjects\1calculator\app\src\main\java\com\shaadow\onecalculator\calculators"

# Define the mapping of class names to correct layout names
$layoutMappings = @{
    # Algebra
    "ComplexNumbersFragment" = "fragment_complex_numbers"
    "FactoringFragment" = "fragment_factoring"
    "LinearEquationsFragment" = "fragment_linear_equations"
    "PolynomialsFragment" = "fragment_polynomials"
    "QuadraticEquationsFragment" = "fragment_quadratic_equations"
    "SystemOfEquationsFragment" = "fragment_system_of_equations"
    
    # Geometry
    "AreaCalculatorFragment" = "fragment_area_calculator"
    "PerimeterCalculatorFragment" = "fragment_perimeter_calculator"
    "PythagoreanTheoremFragment" = "fragment_pythagorean_theorem"
    "SurfaceAreaFragment" = "fragment_surface_area"
    "TrigonometryFragment" = "fragment_trigonometry"
    "VolumeCalculatorFragment" = "fragment_volume_calculator"
    
    # Finance
    "CompoundInterestFragment" = "fragment_compound_interest"
    "InvestmentCalculatorFragment" = "fragment_investment_calculator"
    "LoanCalculatorFragment" = "fragment_loan_calculator"
    "MortgageCalculatorFragment" = "fragment_mortgage_calculator"
    "SimpleInterestFragment" = "fragment_simple_interest"
    "TaxCalculatorFragment" = "fragment_tax_calculator"
    
    # Insurance
    "AutoInsuranceFragment" = "fragment_auto_insurance"
    "CoverageCalculatorFragment" = "fragment_coverage_calculator"
    "HealthInsuranceFragment" = "fragment_health_insurance"
    "HomeInsuranceFragment" = "fragment_home_insurance"
    "LifeInsuranceFragment" = "fragment_life_insurance"
    "PremiumCalculatorFragment" = "fragment_premium_calculator"
    
    # Health
    "BMICalculatorFragment" = "fragment_bmi_calculator"
    "BMRCalculatorFragment" = "fragment_bmr_calculator"
    "BodyFatCalculatorFragment" = "fragment_body_fat_calculator"
    "CalorieCalculatorFragment" = "fragment_calorie_calculator"
    "HealthScoreFragment" = "fragment_health_score"
    "IdealWeightFragment" = "fragment_ideal_weight"
    
    # DateTime
    "AgeCalculatorFragment" = "fragment_age_calculator"
    "CalendarCalculatorFragment" = "fragment_calendar_calculator"
    "CountdownTimerFragment" = "fragment_countdown_timer"
    "DateCalculatorFragment" = "fragment_date_calculator"
    "TimeCalculatorFragment" = "fragment_time_calculator"
    "TimeZoneConverterFragment" = "fragment_time_zone_converter"
    
    # Converters
    "AreaConverterFragment" = "fragment_area_converter"
    "LengthConverterFragment" = "fragment_length_converter"
    "SpeedConverterFragment" = "fragment_speed_converter"
    "TemperatureConverterFragment" = "fragment_temperature_converter"
    "VolumeConverterFragment" = "fragment_volume_converter"
    "WeightConverterFragment" = "fragment_weight_converter"
    
    # Others
    "MatrixCalculatorFragment" = "fragment_matrix_calculator"
    "PercentageCalculatorFragment" = "fragment_percentage_calculator"
    "ProbabilityCalculatorFragment" = "fragment_probability_calculator"
    "RatioCalculatorFragment" = "fragment_ratio_calculator"
    "ScientificCalculatorFragment" = "fragment_scientific_calculator"
    "StatisticsCalculatorFragment" = "fragment_statistics_calculator"
}

# Get all Kotlin files recursively
$ktFiles = Get-ChildItem -Path $baseDir -Filter "*.kt" -Recurse

foreach ($file in $ktFiles) {
    $fileName = $file.Name
    $className = $fileName -replace "\.kt$", ""
    
    if ($layoutMappings.ContainsKey($className)) {
        $correctLayoutName = $layoutMappings[$className]
        
        Write-Host "Fixing layout reference in $fileName..."
        
        $content = Get-Content $file.FullName -Raw
        
        # Fix the layout reference
        $content = $content -replace 'R\.layout\.fragment_[a-z_]+', "R.layout.$correctLayoutName"
        
        # Write back to file
        Set-Content -Path $file.FullName -Value $content -Encoding UTF8
    }
}

Write-Host "Layout name fixes completed!"
