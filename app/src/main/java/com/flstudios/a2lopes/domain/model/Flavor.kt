package com.flstudios.a2lopes.domain.model

data class Flavor(
    val id: Long,
    val name: String,
    val defaultPriceCents: Long?,
    val active: Boolean,
)
