package com.example.myapplication

import java.util.*

object SkinCyclingManager {
    
    enum class NightType(val title: String, val description: String) {
        EXFOLIATION("Hámlasztó este", "Ma távolítsuk el az elhalt hámsejteket AHA vagy BHA savval."),
        RETINOL("Retinol este", "Ma használjunk retinoidot a bőr megújulásáért."),
        RECOVERY("Regeneráló este", "Ma csak hidratáljunk és nyugtassuk a bőrt."),
        RECOVERY_2("Regeneráló este 2", "Folytassuk a hidratálást, hagyjuk pihenni a bőrt.")
    }

    fun getTodaysNightType(): NightType {
        val calendar = Calendar.getInstance()
        val startCalendar = Calendar.getInstance().apply {
            set(2024, 0, 1)
        }
        val diff = calendar.timeInMillis - startCalendar.timeInMillis
        val days = (diff / (1000 * 60 * 60 * 24)).toInt()
        
        return when (days % 4) {
            0 -> NightType.EXFOLIATION
            1 -> NightType.RETINOL
            2 -> NightType.RECOVERY
            else -> NightType.RECOVERY_2
        }
    }
}
