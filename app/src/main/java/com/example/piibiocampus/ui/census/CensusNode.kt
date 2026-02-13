package com.example.piibiocampus.ui.census

enum class CensusType { ORDER, FAMILY, GENUS, SPECIES }

data class CensusNode(
    val id: String,
    val name: String,
    val imageUrl: String = "",
    val description: List<String> = emptyList(),
    val type: CensusType,
    val children: List<CensusNode> = emptyList()
)
