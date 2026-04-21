package com.driftlessdays.app.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.driftlessdays.app.data.remote.CalendarEvent
import com.driftlessdays.app.data.remote.CalendarRepository
import com.driftlessdays.app.data.remote.CalendarResult
import com.driftlessdays.app.data.remote.DailyPhoto
import com.driftlessdays.app.data.remote.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class CalendarView { MONTH, DAY }

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    val currentView: CalendarView = CalendarView.MONTH,
    val monthPhoto: DailyPhoto? = null,
    val dayPhoto: DailyPhoto? = null,
    val events: List<CalendarEvent> = emptyList(),
    val isEventsLoading: Boolean = false,
    val eventsError: String? = null,
    val accountEmail: String? = null,
    val photoMode: PhotoMode = PhotoMode.SAME_AS_MONTH,
    val showPhotoSettings: Boolean = false
)

class CalendarViewModel(
    application: Application,
    private val accountEmail: String? = null
) : AndroidViewModel(application) {

    private val calendarRepository = CalendarRepository(application.applicationContext)
    private val photoRepository = PhotoRepository()

    private val _uiState = MutableStateFlow(
        CalendarUiState(accountEmail = accountEmail)
    )
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadMonthPhoto()
        if (accountEmail != null) {
            loadEventsForCurrentMonth()
        }
    }

    private fun loadMonthPhoto() {
        viewModelScope.launch {
            val photo = photoRepository.getPhotoForMonth(_uiState.value.currentMonth)
            _uiState.value = _uiState.value.copy(
                monthPhoto = photo
            )
        }
    }

    private fun loadDayPhoto(date: LocalDate) {
        viewModelScope.launch {
            val photo = when (_uiState.value.photoMode) {
                PhotoMode.SAME_AS_MONTH -> _uiState.value.monthPhoto
                PhotoMode.DATE_SPECIFIC -> photoRepository.getPhotoForDate(date)
                PhotoMode.RANDOM -> photoRepository.getRandomPhoto()
            }
            _uiState.value = _uiState.value.copy(dayPhoto = photo)
        }
    }

    fun loadEvents(email: String) {
        _uiState.value = _uiState.value.copy(accountEmail = email)
        loadEventsForCurrentMonth()
    }

    private fun loadEventsForCurrentMonth() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isEventsLoading = true,
                eventsError = null
            )
            when (val result = calendarRepository.getEventsForMonth(
                accountEmail = _uiState.value.accountEmail,
                month = _uiState.value.currentMonth
            )) {
                is CalendarResult.Success -> _uiState.value = _uiState.value.copy(
                    events = result.events,
                    isEventsLoading = false
                )
                is CalendarResult.Error -> _uiState.value = _uiState.value.copy(
                    isEventsLoading = false,
                    eventsError = result.message
                )
                is CalendarResult.NotSignedIn -> _uiState.value = _uiState.value.copy(
                    isEventsLoading = false
                )
            }
        }
    }

    fun onPreviousMonth() {
        _uiState.value = _uiState.value.copy(
            currentMonth = _uiState.value.currentMonth.minusMonths(1)
        )
        loadMonthPhoto()
        loadEventsForCurrentMonth()
    }

    fun onNextMonth() {
        _uiState.value = _uiState.value.copy(
            currentMonth = _uiState.value.currentMonth.plusMonths(1)
        )
        loadMonthPhoto()
        loadEventsForCurrentMonth()
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            currentView = CalendarView.DAY
        )
        loadDayPhoto(date)
    }

    fun onBackToMonth() {
        _uiState.value = _uiState.value.copy(
            currentView = CalendarView.MONTH
        )
    }

    fun setPhotoMode(mode: PhotoMode) {
        _uiState.value = _uiState.value.copy(
            photoMode = mode,
            showPhotoSettings = false
        )
        loadDayPhoto(_uiState.value.selectedDate)
    }

    fun togglePhotoSettings() {
        _uiState.value = _uiState.value.copy(
            showPhotoSettings = !_uiState.value.showPhotoSettings
        )
    }

    fun getEventsForDate(date: LocalDate): List<CalendarEvent> =
        _uiState.value.events.filter { it.date == date }

    fun navigateToDate(dateString: String) {
        try {
            val date = LocalDate.parse(dateString)
            _uiState.value = _uiState.value.copy(
                selectedDate = date,
                currentMonth = YearMonth.from(date),
                currentView = CalendarView.DAY
            )
            loadDayPhoto(date)
        } catch (_: Exception) {
            // ignore bad date strings
        }
    }
}

class CalendarViewModelFactory(
    private val application: Application,
    private val accountEmail: String? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(application, accountEmail) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}