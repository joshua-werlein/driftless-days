package com.driftlessdays.app.data.remote

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class DailyPhoto(
    val date: LocalDate,
    val url: String,
    val photographer: String = "Driftless Days",
    val category: String = "nature"
)

class PhotoRepository {

    private val workerBaseUrl = "https://driftless-worker.jjwerlein.workers.dev"

    fun getPhotoForDate(
        date: LocalDate,
        category: String = "nature"
    ): DailyPhoto {
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return DailyPhoto(
            date = date,
            url = "$workerBaseUrl/photo/$category/$dateString",
            category = category
        )
    }

    fun getPhotoForMonth(
        month: YearMonth,
        category: String = "nature"
    ): DailyPhoto {
        val dateString = "${month.year}-${
            month.monthValue.toString().padStart(2, '0')
        }-01"
        return DailyPhoto(
            date = month.atDay(1),
            url = "$workerBaseUrl/photo/$category/$dateString",
            category = category
        )
    }

    fun getRandomPhoto(category: String = "nature"): DailyPhoto {
        return DailyPhoto(
            date = LocalDate.now(),
            url = "$workerBaseUrl/photo/$category/random",
            category = category
        )
    }

    fun getPhotoForToday(category: String = "nature"): DailyPhoto =
        getPhotoForDate(LocalDate.now(), category)
}