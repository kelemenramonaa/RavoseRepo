package com.example.myapplication

object CompatibilityChecker {

    data class Conflict(
        val ingredients: List<String>,
        val message: String
    )

    private val rules = listOf(
        Conflict(listOf("Retin", "Acid"), "A Retinoidok és a hámlasztó savak (AHA/BHA) együttes használata súlyos irritációt okozhat. Javasoljuk, hogy külön este használd őket."),
        Conflict(listOf("Retin", "C-vitamin"), "A Retinoidok és a tiszta C-vitamin (Ascorbic Acid) kiolthatják egymás hatását és irritálhatnak. Használd a C-vitamint reggel, a Retinolt este."),
        Conflict(listOf("Retin", "Benzoyl"), "A Benzoyl Peroxide és a Retinol együtt kiolthatják egymást. Ne használd őket egy rutinban."),
        Conflict(listOf("Ascorbic", "Acid"), "A tiszta C-vitamin és a hámlasztó savak (AHA/BHA) együttesen túl erősek lehetnek a bőrnek. Használd őket különböző napszakokban."),
        Conflict(listOf("Glycolic", "Salicylic"), "Többféle erős sav (AHA és BHA) egyidejű használata károsíthatja a bőr barriert. Váltsd őket naponta."),
        Conflict(listOf("Niacinamide", "Ascorbic"), "A Niacinamide and a tiszta C-vitamin (Ascorbic Acid) egyeseknél bőrpírt okozhat. Ha ilyet tapasztalsz, használd őket külön."),
        Conflict(listOf("Benzoyl", "Acid"), "A Benzoyl Peroxide és a hámlasztó savak együtt extrém módon száríthatják a bőrt.")
    )

    fun findConflicts(ingredientsInRoutine: List<String>): Conflict? {
        val flatIngredients = ingredientsInRoutine.joinToString(" ")
        
        for (rule in rules) {
            val allMatch = rule.ingredients.all { term -> 
                flatIngredients.contains(term, ignoreCase = true) 
            }
            if (allMatch) return rule
        }
        return null
    }
}
