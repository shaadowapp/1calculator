package com.shaadow.onecalculator

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.shaadow.onecalculator.calculators.algebra.*
import com.shaadow.onecalculator.calculators.geometry.*
import com.shaadow.onecalculator.calculators.finance.*
import com.shaadow.onecalculator.calculators.insurance.*
import com.shaadow.onecalculator.calculators.health.*
import com.shaadow.onecalculator.calculators.datetime.*
import com.shaadow.onecalculator.calculators.converters.*
import com.shaadow.onecalculator.calculators.others.*

class CalculatorHostDialog : DialogFragment() {
    companion object {
        private const val ARG_TYPE = "type"
        fun newInstance(type: String): CalculatorHostDialog {
            val args = Bundle()
            args.putString(ARG_TYPE, type)
            val dialog = CalculatorHostDialog()
            dialog.arguments = args
            return dialog
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_calculator_host, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val type = arguments?.getString(ARG_TYPE) ?: return
        val fragment: Fragment = getFragmentForCalculatorType(type)
        
        childFragmentManager.beginTransaction()
            .replace(R.id.calculator_fragment_container, fragment)
            .commit()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    
    private fun getFragmentForCalculatorType(calculatorType: String): Fragment {
        return when (calculatorType) {
            // Algebra
            "Linear Equations" -> LinearEquationsFragment()
            "Quadratic Equations" -> QuadraticEquationsFragment()
            "System of Equations" -> SystemOfEquationsFragment()
            "Polynomials" -> PolynomialsFragment()
            "Factoring" -> FactoringFragment()
            "Complex Numbers" -> ComplexNumbersFragment()
            
            // Geometry
            "Area Calculator" -> AreaCalculatorFragment()
            "Perimeter Calculator" -> PerimeterCalculatorFragment()
            "Volume Calculator" -> VolumeCalculatorFragment()
            "Surface Area" -> SurfaceAreaFragment()
            "Pythagorean Theorem" -> PythagoreanTheoremFragment()
            "Trigonometry" -> TrigonometryFragment()
            
            // Finance
            "Simple Interest" -> SimpleInterestFragment()
            "Compound Interest" -> CompoundInterestFragment()
            "Loan Calculator" -> LoanCalculatorFragment()
            "Mortgage Calculator" -> MortgageCalculatorFragment()
            "Investment Calculator" -> InvestmentCalculatorFragment()
            "Tax Calculator" -> TaxCalculatorFragment()
            
            // Insurance
            "Life Insurance" -> LifeInsuranceFragment()
            "Health Insurance" -> HealthInsuranceFragment()
            "Auto Insurance" -> AutoInsuranceFragment()
            "Home Insurance" -> HomeInsuranceFragment()
            "Premium Calculator" -> PremiumCalculatorFragment()
            "Coverage Calculator" -> CoverageCalculatorFragment()
            
            // Health
            "BMI Calculator" -> BMICalculatorFragment()
            "Calorie Calculator" -> CalorieCalculatorFragment()
            "BMR Calculator" -> BMRCalculatorFragment()
            "Body Fat Calculator" -> BodyFatCalculatorFragment()
            "Ideal Weight" -> IdealWeightFragment()
            "Health Score" -> HealthScoreFragment()
            
            // Date & Time
            "Date Calculator" -> DateCalculatorFragment()
            "Time Calculator" -> TimeCalculatorFragment()
            "Age Calculator" -> AgeCalculatorFragment()
            "Countdown Timer" -> CountdownTimerFragment()
            "Time Zone Converter" -> TimeZoneConverterFragment()
            "Calendar Calculator" -> CalendarCalculatorFragment()
            
            // Unit Converters
            "Length Converter" -> LengthConverterFragment()
            "Weight Converter" -> WeightConverterFragment()
            "Temperature Converter" -> TemperatureConverterFragment()
            "Area Converter" -> AreaConverterFragment()
            "Volume Converter" -> VolumeConverterFragment()
            "Speed Converter" -> SpeedConverterFragment()
            
            // Others
            "Percentage Calculator" -> PercentageCalculatorFragment()
            "Ratio Calculator" -> RatioCalculatorFragment()
            "Statistics Calculator" -> StatisticsCalculatorFragment()
            "Probability Calculator" -> ProbabilityCalculatorFragment()
            "Scientific Calculator" -> ScientificCalculatorFragment()
            "Matrix Calculator" -> MatrixCalculatorFragment()
            
            else -> LinearEquationsFragment() // fallback
        }
    }
}
