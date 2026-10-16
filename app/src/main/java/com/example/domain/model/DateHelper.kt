package com.example.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateHelper {

  fun formatEpochMillisToDateString(epochMillis: Long): String {
    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date(epochMillis))
  }

  fun getTodayDateString(): String {
    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    return sdf.format(Date())
  }

  fun getTodayEpochDay(): Long {
    return System.currentTimeMillis() / 86400000L
  }

  fun epochMillisToEpochDay(epochMillis: Long): Long {
    return epochMillis / 86400000L
  }

  fun parseDateStringToEpochMillis(dateString: String): Long? {
    return try {
      val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
      sdf.timeZone = TimeZone.getTimeZone("UTC")
      sdf.parse(dateString)?.time
    } catch (e: Exception) {
      null
    }
  }

  fun parseDateStringToEpochDay(dateString: String): Long {
    val millis = parseDateStringToEpochMillis(dateString)
    return if (millis != null) {
      millis / 86400000L
    } else {
      getTodayEpochDay()
    }
  }
}
