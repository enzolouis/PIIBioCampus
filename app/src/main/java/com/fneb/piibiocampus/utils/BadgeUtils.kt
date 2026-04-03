package com.fneb.piibiocampus.utils

import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.model.Badge

object BadgeUtils {

    val ALL_BADGES = listOf(
        Badge("ic_badge_cerf_erudit",          R.drawable.ic_badge_cerf_erudit,          "Cerf érudit",           100),
        Badge("ic_badge_chouette_savante",      R.drawable.ic_badge_chouette_savante,      "Chouette savante",       90),
        Badge("ic_badge_renard_ruse",           R.drawable.ic_badge_renard_ruse,           "Renard rusé",            80),
        Badge("ic_badge_sanglier_chercheur",    R.drawable.ic_badge_sanglier_chercheur,    "Sanglier chercheur",     70),
        Badge("ic_badge_pie_futee",             R.drawable.ic_badge_pie_futee,             "Pie futée",              60),
        Badge("ic_badge_ecureuil_eclaire",      R.drawable.ic_badge_ecureuil_eclaire,      "Écureuil éclairé",       50),
        Badge("ic_badge_blaireau_fouineur",     R.drawable.ic_badge_blaireau_fouineur,     "Blaireau fouineur",      40),
        Badge("ic_badge_herisson_debrouillard", R.drawable.ic_badge_herisson_debrouillard, "Hérisson débrouillard",  30),
        Badge("ic_badge_lapin_malin",           R.drawable.ic_badge_lapin_malin,           "Lapin malin",            20),
        Badge("ic_badge_scarabe_astucieux",     R.drawable.ic_badge_scarabe_astucieux,     "Scarabée astucieux",     10),
        Badge("ic_badge_abeille_curieuse",      R.drawable.ic_badge_abeille_curieuse,      "Abeille curieuse",        1),
    )

    fun getUnlockedBadges(photoCount: Int): List<Badge> {
        return ALL_BADGES.filter { photoCount >= it.threshold }
    }

    fun getCurrentBadgeDrawable(photoCount: Int): Int {
        return ALL_BADGES.firstOrNull { photoCount >= it.threshold }?.drawableRes
            ?: R.drawable.norank
    }

    fun getDrawableById(id: String): Int {
        return ALL_BADGES.firstOrNull { it.id == id }?.drawableRes ?: R.drawable.norank
    }
}