package com.example.swimminganalysisapplication.data.remote.model

data class Race(
    val id: Int,
    val event_name: String,
    val date: String?, // API仕様では DATE型ですが、文字列として受け取るのが簡単です
    val location: String?
)
