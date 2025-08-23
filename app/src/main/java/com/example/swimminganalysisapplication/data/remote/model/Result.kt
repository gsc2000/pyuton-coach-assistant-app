package com.example.swimminganalysisapplication.data.remote.model

data class Result(
    val id: Int,
    val swimmer_id: Int,
    val race_id: Int,
    val time: Double?, // API仕様では DECIMAL(8,2) ですが、Doubleで受け取れます
    val place: Int?
)
