package com.driftlessdays.app.data.remote

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

data class CalendarEvent(
    val id: String,
    val title: String,
    val date: LocalDate,
    val startTime: String?,
    val endTime: String?,
    val isAllDay: Boolean
)

sealed class CalendarResult {
    data class Success(val events: List<CalendarEvent>) : CalendarResult()
    data class Error(val message: String) : CalendarResult()
    object NotSignedIn : CalendarResult()
}

class CalendarRepository(private val context: Context) {

    private fun getCalendarService(accountEmail: String): Calendar {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: throw Exception("Not signed in")

        val credential = GoogleAccountCredential
            .usingOAuth2(context, listOf(CalendarScopes.CALENDAR_READONLY))
            .setBackOff(ExponentialBackOff())
        credential.selectedAccount = account.account

        return Calendar.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Driftless Days")
            .build()
    }

    suspend fun getEventsForMonth(
        accountEmail: String?,
        month: java.time.YearMonth
    ): CalendarResult {
        if (accountEmail == null) {
            android.util.Log.d("CalendarRepo", "No account email — not signed in")
            return CalendarResult.NotSignedIn
        }

        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("CalendarRepo", "Fetching events for $accountEmail, month: $month")
                val service = getCalendarService(accountEmail)

                val firstDay = month.atDay(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .let { com.google.api.client.util.DateTime(Date.from(it)) }

                val lastDay = month.atEndOfMonth()
                    .atTime(23, 59, 59)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .let { com.google.api.client.util.DateTime(Date.from(it)) }

                android.util.Log.d("CalendarRepo", "Querying from $firstDay to $lastDay")
// Get list of all calendars first
                val calendarList = service.calendarList().list().execute()

                val allEvents = mutableListOf<CalendarEvent>()

                calendarList.items?.forEach { cal ->
                    try {
                        val events = service.events().list(cal.id)
                            .setTimeMin(firstDay)
                            .setTimeMax(lastDay)
                            .setOrderBy("startTime")
                            .setSingleEvents(true)
                            .execute()

                        android.util.Log.d("CalendarRepo", "Calendar '${cal.summary}': ${events.items?.size ?: 0} events")

                        events.items?.forEach { event ->
                            try {
                                val isAllDay = event.start.date != null
                                val startDate = if (isAllDay) {
                                    val d = event.start.date.toStringRfc3339().substring(0, 10)
                                    LocalDate.parse(d)
                                } else {
                                    Date(event.start.dateTime.value)
                                        .toInstant()
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                }

                                allEvents.add(CalendarEvent(
                                    id = event.id ?: "",
                                    title = event.summary ?: "Untitled",
                                    date = startDate,
                                    startTime = if (!isAllDay) {
                                        val d = Date(event.start.dateTime.value)
                                            .toInstant()
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalTime()
                                        "${d.hour}:${d.minute.toString().padStart(2, '0')}"
                                    } else null,
                                    endTime = if (!isAllDay && event.end?.dateTime != null) {
                                        val d = Date(event.end.dateTime.value)
                                            .toInstant()
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalTime()
                                        "${d.hour}:${d.minute.toString().padStart(2, '0')}"
                                    } else null,
                                    isAllDay = isAllDay
                                ))
                            } catch (e: Exception) {
                                android.util.Log.e("CalendarRepo", "Error parsing event: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CalendarRepo", "Error fetching calendar ${cal.summary}: ${e.message}")
                    }
                }

                android.util.Log.d("CalendarRepo", "Total events across all calendars: ${allEvents.size}")
                CalendarResult.Success(allEvents)

            } catch (e: Exception) {
                android.util.Log.e("CalendarRepo", "Error fetching events: ${e.message}", e)
                CalendarResult.Error(e.message ?: "Failed to load calendar events")
            }
        }
    }
}