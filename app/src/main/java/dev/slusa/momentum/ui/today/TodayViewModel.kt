package dev.slusa.momentum.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.slusa.momentum.data.Bucket
import dev.slusa.momentum.data.Todo
import dev.slusa.momentum.data.TodoRepository
import dev.slusa.momentum.domain.Aging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodoUi(
    val todo: Todo,
    val ageDays: Int,
    val onTodayList: Boolean,
)

data class TodayUiState(
    val day: LocalDate = LocalDate.now(),
    val withDate: List<TodoUi> = emptyList(),
    val markedToday: List<TodoUi> = emptyList(),
    val general: List<TodoUi> = emptyList(),
    val done: List<TodoUi> = emptyList(),
) {
    val todayCount: Int get() = withDate.size + markedToday.size
}

class TodayViewModel(private val repo: TodoRepository) : ViewModel() {

    /**
     * Data dnia jest stanem, a nie odczytem w locie: przepływy nie obudza sie same
     * o polnocy, wiec odswiezamy ja przy powrocie do aplikacji.
     */
    private val today = MutableStateFlow(LocalDate.now())

    /**
     * Okno cofniecia liczy sie od "teraz", wiec zapytanie o odhaczone musi powstac
     * na nowo przy kazdej zmianie dnia - stad flatMapLatest zamiast zwyklego combine.
     * Inaczej granica 24 godzin zostalaby zamrozona w chwili utworzenia przeplywu.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<TodayUiState> = today.flatMapLatest { day ->
        combine(
            repo.open(Bucket.GLOWNE),
            repo.completedLastDay(Bucket.GLOWNE),
        ) { open, done ->
            val onToday = { t: Todo -> t.plannedDate != null && !t.plannedDate.isAfter(day) }

            fun map(list: List<Todo>) = list.map {
                TodoUi(
                    todo = it,
                    ageDays = Aging.ageDays(it.firstTodayDate, day),
                    onTodayList = onToday(it),
                )
            }

            TodayUiState(
                day = day,
                withDate = map(open.filter { onToday(it) && it.fromSchedule }),
                markedToday = map(open.filter { onToday(it) && !it.fromSchedule }),
                general = map(open.filter { it.plannedDate == null }),
                done = map(done),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    init {
        viewModelScope.launch { repo.purgeOldCompleted() }
    }

    /** Wolane przy powrocie do aplikacji - lapie zmiane doby bez restartu. */
    fun refreshDay() {
        today.value = LocalDate.now()
    }

    fun add(title: String, forToday: Boolean) = viewModelScope.launch {
        repo.add(title = title, forToday = forToday, today = today.value)
    }

    fun toggleToday(id: Long) = viewModelScope.launch {
        repo.toggleToday(id, today.value)
    }

    fun setDone(id: Long, done: Boolean) = viewModelScope.launch {
        repo.setDone(id, done)
    }

    fun delete(todo: Todo) = viewModelScope.launch {
        repo.delete(todo)
    }

    companion object {
        fun factory(repo: TodoRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { TodayViewModel(repo) }
        }
    }
}
