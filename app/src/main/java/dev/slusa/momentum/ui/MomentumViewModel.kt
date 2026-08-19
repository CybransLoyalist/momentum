package dev.slusa.momentum.ui

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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodoUi(
    val todo: Todo,
    val ageDays: Int = 0,
    val onTodayList: Boolean = false,
)

data class TodayUiState(
    val day: LocalDate = LocalDate.now(),
    val withDate: List<TodoUi> = emptyList(),
    val markedToday: List<TodoUi> = emptyList(),
    val general: List<TodoUi> = emptyList(),
    val done: List<TodoUi> = emptyList(),
) {
    val todayCount: Int get() = withDate.size + markedToday.size
    val isEmpty: Boolean get() = todayCount == 0 && general.isEmpty()
}

data class ShoppingUiState(
    val open: List<TodoUi> = emptyList(),
    val done: List<TodoUi> = emptyList(),
)

/**
 * Jeden model widoku na wszystkie cztery listy. Aplikacja dla jednej osoby
 * z jednym repozytorium - osobne modele na zakladke daloby cztery kopie tego
 * samego kodu i cztery miejsca do zsynchronizowania przy kazdej akcji.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MomentumViewModel(private val repo: TodoRepository) : ViewModel() {

    /**
     * Data dnia jest stanem, a nie odczytem w locie: przeplywy nie obudza sie same
     * o polnocy, wiec odswiezamy ja przy powrocie do aplikacji.
     */
    private val today = MutableStateFlow(LocalDate.now())

    /**
     * Okno cofniecia liczy sie od "teraz", wiec zapytanie o odhaczone musi powstac
     * na nowo przy kazdej zmianie dnia - stad flatMapLatest zamiast zwyklego combine.
     */
    val todayState: StateFlow<TodayUiState> = today.flatMapLatest { day ->
        combine(
            repo.open(Bucket.GLOWNE),
            repo.completedLastDay(Bucket.GLOWNE),
        ) { open, done ->
            fun onToday(t: Todo) = t.plannedDate != null && !t.plannedDate.isAfter(day)
            fun map(list: List<Todo>) = list.map {
                TodoUi(
                    todo = it,
                    ageDays = Aging.ageDays(it.plannedDate, day),
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

    val somedayState: StateFlow<List<TodoUi>> = repo.open(Bucket.KIEDYS)
        .map { list -> list.map { TodoUi(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Wszystko z terminem w przyszlosci, najblizsze na gorze. */
    val scheduledState: StateFlow<List<TodoUi>> = today.flatMapLatest { day ->
        repo.open(Bucket.GLOWNE).map { list ->
            list.filter { it.plannedDate != null && it.plannedDate.isAfter(day) }
                .sortedBy { it.plannedDate }
                .map { TodoUi(it) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val shoppingState: StateFlow<ShoppingUiState> = today.flatMapLatest {
        combine(
            repo.open(Bucket.ZAKUPY),
            repo.completedLastDay(Bucket.ZAKUPY),
        ) { open, done ->
            ShoppingUiState(
                open = open.map { TodoUi(it) },
                done = done.map { TodoUi(it) },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingUiState())

    init {
        viewModelScope.launch { repo.purgeOldCompleted() }
    }

    /** Wolane przy powrocie do aplikacji - lapie zmiane doby bez restartu. */
    fun refreshDay() {
        today.value = LocalDate.now()
    }

    fun add(title: String, bucket: Bucket = Bucket.GLOWNE, forToday: Boolean = false) =
        viewModelScope.launch {
            repo.add(
                title = title,
                bucket = bucket,
                plannedDate = if (forToday) today.value else null,
            )
        }

    fun addScheduled(title: String, date: LocalDate) = viewModelScope.launch {
        repo.add(title = title, plannedDate = date, fromSchedule = true)
    }

    fun toggleToday(id: Long) = viewModelScope.launch { repo.toggleToday(id, today.value) }

    fun setDone(id: Long, done: Boolean) = viewModelScope.launch { repo.setDone(id, done) }

    fun schedule(id: Long, date: LocalDate) = viewModelScope.launch { repo.schedule(id, date) }

    fun moveTo(id: Long, bucket: Bucket) = viewModelScope.launch { repo.moveTo(id, bucket) }

    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }

    fun clearShoppingDone() = viewModelScope.launch { repo.clearCompleted(Bucket.ZAKUPY) }

    companion object {
        fun factory(repo: TodoRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { MomentumViewModel(repo) }
        }
    }
}
