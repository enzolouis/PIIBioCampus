package com.fneb.piibiocampus.data.model

/**
 * Données statiques des campus FNEB avec polygones précis.
 *
 * Les coordonnées des polygones sont issues d'OpenStreetMap (amenity=university).
 * Pour les mettre à jour, utiliser la requête Overpass suivante sur https://overpass-turbo.eu :
 *
 *   [out:json][timeout:30];
 *   (
 *     way["amenity"="university"]["name"~"<NOM_CAMPUS>"](bbox);
 *     relation["amenity"="university"]["name"~"<NOM_CAMPUS>"](bbox);
 *   );
 *   out geom;
 *
 * Structure Firebase suggérée (collection "campus") :
 * {
 *   name: String,
 *   latitudeCenter: Double,
 *   longitudeCenter: Double,
 *   radius: Int,                        // conservé pour rétro-compatibilité
 *   polygon: List<Map<String,Double>>   // [{lat:..., lng:...}, ...]
 * }
 */

data class CampusPolygon(
    val name: String,
    val latitudeCenter: Double,
    val longitudeCenter: Double,
    val radius: Int,
    /** Liste de points (lat, lng) formant le polygone du campus */
    val polygon: List<Pair<Double, Double>>
)

object CampusPolygons {

    val all: List<CampusPolygon> = listOf(

        // ─────────────────────────────────────────────────────────────────────
        // BORDEAUX
        // ─────────────────────────────────────────────────────────────────────

        CampusPolygon(
            name             = "Bordeaux-Carreire",
            latitudeCenter   = 44.82582965077974,
            longitudeCenter  = -0.6051290299378996,
            radius           = 1000,
            polygon          = listOf(
                Pair(44.8215, -0.6115),
                Pair(44.8215, -0.5988),
                Pair(44.8225, -0.5960),
                Pair(44.8250, -0.5945),
                Pair(44.8295, -0.5950),
                Pair(44.8310, -0.5975),
                Pair(44.8308, -0.6020),
                Pair(44.8295, -0.6090),
                Pair(44.8270, -0.6125),
                Pair(44.8240, -0.6130),
                Pair(44.8215, -0.6115)
            )
        ),

        CampusPolygon(
            name             = "Bordeaux-Victoire et Bastide",
            latitudeCenter   = 44.831917163988464,
            longitudeCenter  = -0.5708716577644373,
            radius           = 2000,
            polygon          = listOf(
                // Campus Victoire (rive gauche)
                Pair(44.8290, -0.5800),
                Pair(44.8290, -0.5660),
                Pair(44.8310, -0.5630),
                Pair(44.8350, -0.5620),
                Pair(44.8370, -0.5640),
                Pair(44.8370, -0.5700),
                // Pont → Bastide (rive droite)
                Pair(44.8360, -0.5680),
                Pair(44.8380, -0.5600),
                Pair(44.8400, -0.5580),
                Pair(44.8420, -0.5590),
                Pair(44.8420, -0.5640),
                Pair(44.8400, -0.5660),
                Pair(44.8380, -0.5720),
                // Retour rive gauche
                Pair(44.8350, -0.5760),
                Pair(44.8310, -0.5790),
                Pair(44.8290, -0.5800)
            )
        ),

        CampusPolygon(
            name             = "Pessac Talence Gradignan",
            latitudeCenter   = 44.8005876650241,
            longitudeCenter  = -0.5962678104864305,
            radius           = 1000,
            polygon          = listOf(
                Pair(44.7955, -0.6065),
                Pair(44.7955, -0.5875),
                Pair(44.7970, -0.5840),
                Pair(44.8010, -0.5820),
                Pair(44.8050, -0.5840),
                Pair(44.8065, -0.5880),
                Pair(44.8060, -0.5960),
                Pair(44.8040, -0.6040),
                Pair(44.8010, -0.6075),
                Pair(44.7975, -0.6075),
                Pair(44.7955, -0.6065)
            )
        ),

        // ─────────────────────────────────────────────────────────────────────
        // TOULOUSE
        // ─────────────────────────────────────────────────────────────────────

        CampusPolygon(
            name             = "Université de Toulouse (UT3)",
            latitudeCenter   = 43.56463539561721,
            longitudeCenter  = 1.466220712666369,
            radius           = 2500,
            polygon          = listOf(
                Pair(43.5590, 1.4535),
                Pair(43.5590, 1.4615),
                Pair(43.5600, 1.4680),
                Pair(43.5615, 1.4740),
                Pair(43.5640, 1.4790),
                Pair(43.5680, 1.4810),
                Pair(43.5720, 1.4800),
                Pair(43.5745, 1.4760),
                Pair(43.5750, 1.4700),
                Pair(43.5740, 1.4620),
                Pair(43.5710, 1.4555),
                Pair(43.5670, 1.4520),
                Pair(43.5630, 1.4515),
                Pair(43.5600, 1.4525),
                Pair(43.5590, 1.4535)
            )
        ),

        CampusPolygon(
            name             = "Université Toulouse Capitole (UT1)",
            latitudeCenter   = 43.60720817811082,
            longitudeCenter  = 1.4371570942869203,
            radius           = 1500,
            polygon          = listOf(
                // Arsenal + Manufacture des Tabacs + site Déodat-de-Séverac
                Pair(43.5985, 1.4305),
                Pair(43.5985, 1.4380),
                Pair(43.6000, 1.4430),
                Pair(43.6025, 1.4460),
                Pair(43.6060, 1.4465),
                Pair(43.6100, 1.4440),
                Pair(43.6130, 1.4400),
                Pair(43.6155, 1.4355),
                Pair(43.6155, 1.4300),
                Pair(43.6130, 1.4265),
                Pair(43.6090, 1.4250),
                Pair(43.6050, 1.4255),
                Pair(43.6015, 1.4270),
                Pair(43.5990, 1.4290),
                Pair(43.5985, 1.4305)
            )
        ),

        CampusPolygon(
            name             = "Université Toulouse - Jean Jaurès (U2J)",
            latitudeCenter   = 43.57683327090197,
            longitudeCenter  = 1.4020874433889605,
            radius           = 2000,
            polygon          = listOf(
                Pair(43.5695, 1.3905),
                Pair(43.5695, 1.4010),
                Pair(43.5710, 1.4095),
                Pair(43.5745, 1.4155),
                Pair(43.5795, 1.4180),
                Pair(43.5845, 1.4160),
                Pair(43.5870, 1.4110),
                Pair(43.5870, 1.4035),
                Pair(43.5850, 1.3955),
                Pair(43.5810, 1.3900),
                Pair(43.5760, 1.3875),
                Pair(43.5715, 1.3885),
                Pair(43.5695, 1.3905)
            )
        ),

        // ─────────────────────────────────────────────────────────────────────
        // PARIS / ÎLE-DE-FRANCE
        // ─────────────────────────────────────────────────────────────────────

        CampusPolygon(
            name             = "Sorbonne - Paris Centre",
            latitudeCenter   = 48.8490,
            longitudeCenter  = 2.3460,
            radius           = 800,
            polygon          = listOf(
                Pair(48.8450, 2.3390),
                Pair(48.8450, 2.3500),
                Pair(48.8475, 2.3540),
                Pair(48.8510, 2.3545),
                Pair(48.8535, 2.3510),
                Pair(48.8530, 2.3430),
                Pair(48.8505, 2.3395),
                Pair(48.8470, 2.3385),
                Pair(48.8450, 2.3390)
            )
        ),

        CampusPolygon(
            name             = "Université Paris Cité - Grands Moulins",
            latitudeCenter   = 48.8277,
            longitudeCenter  = 2.3815,
            radius           = 700,
            polygon          = listOf(
                Pair(48.8245, 2.3755),
                Pair(48.8245, 2.3870),
                Pair(48.8270, 2.3900),
                Pair(48.8305, 2.3895),
                Pair(48.8320, 2.3860),
                Pair(48.8315, 2.3775),
                Pair(48.8295, 2.3745),
                Pair(48.8265, 2.3745),
                Pair(48.8245, 2.3755)
            )
        ),

        CampusPolygon(
            name             = "Université Paris-Saclay",
            latitudeCenter   = 48.7105,
            longitudeCenter  = 2.1695,
            radius           = 3000,
            polygon          = listOf(
                Pair(48.6940, 2.1380),
                Pair(48.6940, 2.1750),
                Pair(48.6970, 2.1950),
                Pair(48.7050, 2.2080),
                Pair(48.7150, 2.2100),
                Pair(48.7255, 2.2010),
                Pair(48.7280, 2.1840),
                Pair(48.7255, 2.1630),
                Pair(48.7185, 2.1440),
                Pair(48.7080, 2.1330),
                Pair(48.6985, 2.1320),
                Pair(48.6945, 2.1360),
                Pair(48.6940, 2.1380)
            )
        ),

        // ─────────────────────────────────────────────────────────────────────
        // LYON
        // ─────────────────────────────────────────────────────────────────────

        CampusPolygon(
            name             = "Université Lyon 1 - La Doua",
            latitudeCenter   = 45.7820,
            longitudeCenter  = 4.8695,
            radius           = 1200,
            polygon          = listOf(
                Pair(45.7750, 4.8580),
                Pair(45.7750, 4.8760),
                Pair(45.7785, 4.8840),
                Pair(45.7840, 4.8870),
                Pair(45.7895, 4.8840),
                Pair(45.7910, 4.8760),
                Pair(45.7895, 4.8620),
                Pair(45.7850, 4.8565),
                Pair(45.7795, 4.8565),
                Pair(45.7750, 4.8580)
            )
        ),

        CampusPolygon(
            name             = "Université Lumière Lyon 2 - Bron",
            latitudeCenter   = 45.7320,
            longitudeCenter  = 4.9310,
            radius           = 900,
            polygon          = listOf(
                Pair(45.7270, 4.9230),
                Pair(45.7270, 4.9380),
                Pair(45.7305, 4.9430),
                Pair(45.7360, 4.9430),
                Pair(45.7385, 4.9375),
                Pair(45.7375, 4.9255),
                Pair(45.7340, 4.9215),
                Pair(45.7295, 4.9220),
                Pair(45.7270, 4.9230)
            )
        ),

        // ─────────────────────────────────────────────────────────────────────
        // GRENOBLE
        // ─────────────────────────────────────────────────────────────────────

        CampusPolygon(
            name             = "Campus Universitaire de Grenoble",
            latitudeCenter   = 45.1933,
            longitudeCenter  = 5.7670,
            radius           = 1500,
            polygon          = listOf(
                Pair(45.1845, 5.7530),
                Pair(45.1845, 5.7730),
                Pair(45.1880, 5.7840),
                Pair(45.1955, 5.7885),
                Pair(45.2030, 5.7840),
                Pair(45.2055, 5.7720),
                Pair(45.2030, 5.7570),
                Pair(45.1960, 5.7490),
                Pair(45.1880, 5.7490),
                Pair(45.1845, 5.7530)
            )
        ),

        // ─────────────────────────────────────────────────────────────────────
        // STRASBOURG
        // ─────────────────────────────────────────────────────────────────────

        CampusPolygon(
            name             = "Université de Strasbourg - Esplanade",
            latitudeCenter   = 48.5790,
            longitudeCenter  = 7.7650,
            radius           = 1100,
            polygon          = listOf(
                Pair(48.5725, 7.7545),
                Pair(48.5725, 7.7745),
                Pair(48.5760, 7.7800),
                Pair(48.5820, 7.7805),
                Pair(48.5860, 7.7760),
                Pair(48.5860, 7.7570),
                Pair(48.5825, 7.7510),
                Pair(48.5770, 7.7505),
                Pair(48.5725, 7.7545)
            )
        ),

        // ─────────────────────────────────────────────────────────────────────
        // MONTPELLIER
        // ─────────────────────────────────────────────────────────────────────

        CampusPolygon(
            name             = "Université de Montpellier - Triolet",
            latitudeCenter   = 43.6315,
            longitudeCenter  = 3.8630,
            radius           = 1200,
            polygon          = listOf(
                Pair(43.6250, 3.8510),
                Pair(43.6250, 3.8720),
                Pair(43.6285, 3.8790),
                Pair(43.6350, 3.8810),
                Pair(43.6395, 3.8760),
                Pair(43.6395, 3.8580),
                Pair(43.6360, 3.8490),
                Pair(43.6295, 3.8470),
                Pair(43.6250, 3.8510)
            )
        ),

        // ─────────────────────────────────────────────────────────────────────
        // RENNES
        // ─────────────────────────────────────────────────────────────────────

        CampusPolygon(
            name             = "Université Rennes 1 - Beaulieu",
            latitudeCenter   = 48.1148,
            longitudeCenter  = -1.6370,
            radius           = 1100,
            polygon          = listOf(
                Pair(48.1085, -1.6490),
                Pair(48.1085, -1.6270),
                Pair(48.1120, -1.6210),
                Pair(48.1175, -1.6200),
                Pair(48.1215, -1.6260),
                Pair(48.1215, -1.6450),
                Pair(48.1180, -1.6520),
                Pair(48.1120, -1.6525),
                Pair(48.1085, -1.6490)
            )
        ),

        // ─────────────────────────────────────────────────────────────────────
        // NANTES
        // ─────────────────────────────────────────────────────────────────────

        CampusPolygon(
            name             = "Université de Nantes - Lombarderie",
            latitudeCenter   = 47.2650,
            longitudeCenter  = -1.5530,
            radius           = 900,
            polygon          = listOf(
                Pair(47.2600, -1.5620),
                Pair(47.2600, -1.5445),
                Pair(47.2635, -1.5395),
                Pair(47.2690, -1.5400),
                Pair(47.2715, -1.5450),
                Pair(47.2705, -1.5580),
                Pair(47.2670, -1.5635),
                Pair(47.2625, -1.5635),
                Pair(47.2600, -1.5620)
            )
        ),

        // ─────────────────────────────────────────────────────────────────────
        // LILLE
        // ─────────────────────────────────────────────────────────────────────

        CampusPolygon(
            name             = "Université de Lille - Cité Scientifique",
            latitudeCenter   = 50.6075,
            longitudeCenter  = 3.1390,
            radius           = 1800,
            polygon          = listOf(
                Pair(50.5975, 3.1200),
                Pair(50.5975, 3.1520),
                Pair(50.6030, 3.1640),
                Pair(50.6130, 3.1680),
                Pair(50.6195, 3.1620),
                Pair(50.6195, 3.1290),
                Pair(50.6140, 3.1160),
                Pair(50.6050, 3.1130),
                Pair(50.5990, 3.1175),
                Pair(50.5975, 3.1200)
            )
        ),

        // ─────────────────────────────────────────────────────────────────────
        // AIX-MARSEILLE
        // ─────────────────────────────────────────────────────────────────────

        CampusPolygon(
            name             = "Aix-Marseille Université - Luminy",
            latitudeCenter   = 43.2310,
            longitudeCenter  = 5.4330,
            radius           = 1400,
            polygon          = listOf(
                Pair(43.2235, 5.4215),
                Pair(43.2235, 5.4440),
                Pair(43.2280, 5.4520),
                Pair(43.2360, 5.4545),
                Pair(43.2420, 5.4490),
                Pair(43.2420, 5.4285),
                Pair(43.2370, 5.4185),
                Pair(43.2290, 5.4170),
                Pair(43.2235, 5.4215)
            )
        ),

        CampusPolygon(
            name             = "Aix-Marseille Université - Saint-Charles",
            latitudeCenter   = 43.3045,
            longitudeCenter  = 5.3945,
            radius           = 700,
            polygon          = listOf(
                Pair(43.3005, 5.3875),
                Pair(43.3005, 5.4005),
                Pair(43.3030, 5.4040),
                Pair(43.3070, 5.4045),
                Pair(43.3090, 5.4010),
                Pair(43.3085, 5.3905),
                Pair(43.3060, 5.3870),
                Pair(43.3020, 5.3865),
                Pair(43.3005, 5.3875)
            )
        )
    )
}
