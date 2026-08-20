package dev.slusa.momentum.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.net.Uri
import dev.slusa.momentum.backup.BackupData
import dev.slusa.momentum.backup.BackupRepository
import dev.slusa.momentum.data.Bucket
import dev.slusa.momentum.data.Habit
import dev.slusa.momentum.data.HabitRepository
import dev.slusa.momentum.data.Recurrence
import dev.slusa.momentum.data.Reminder
import dev.slusa.momentum.data.Settings
import dev.slusa.momentum.data.SettingsStore
import dev.slusa.momentum.data.Todo
import dev.slusa.momentum.data.TodoRepository
import dev.slusa.momentum.domain.Aging
import dev.slusa.momentum.domain.DayState
import dev.slusa.momentum.domain.Momentum
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

data class TodoUi(
    val todo: Todo,
    val ageDays: Int = 0,
    val onTodayList: Boolean = false,
    /** Regula powtarzania, jesli to instancja cykliczna - do etykiety na kafelku. */
    val rule: Recurrence? = null,
    /** Termin jeszcze przed nami - rozstrzyga, ktore akcje maja sens w arkuszu. */
    val hasFutureDate: Boolean = false,
)

data class HabitUi(
    val habit: Habit,
    val momentum: Int = 0,
    val doneToday: Boolean = false,
    val scheduledToday: Boolean = false,
    val pausedToday: Boolean = false,
    val grid: List<Pair<LocalDate, DayState>> = emptyList(),
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
 * Jeden model widoku na wszystkie listy. Aplikacja dla jednej osoby z jedna baza -
 * osobne modele na zakladke daloby kilka kopii tego samego kodu i kilka miejsc
 * do zsynchronizowania przy kazdej akcji.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MomentumViewModel(
    private val todos: TodoRepository,
    private val habits: HabitRepository,
    private val settingsStore: SettingsStore,
    private val backups: BackupRepository,
) : ViewModel() {

    /**
     * Ostatni komunikat o kopii zapasowej. Kopia jest operacja, ktorej efektu nie
     * widac w aplikacji - bez potwierdzenia z nazwa pliku nie wiadomo, czy cokolwiek
     * sie stalo, a to jest dokladnie ta funkcja, ktorej trzeba ufac.
     */
    val backupMessage: MutableStateFlow<String?> = MutableStateFlow(null)

    /**
     * Kopia znaleziona w katalogu aplikacji przy pustej bazie - czyli praktycznie
     * zawsze po przeniesieniu na nowy telefon, gdy Auto Backup odtworzyl pliki, ale
     * nie odtworzyl bazy. Data sluzy do pytania, zanim cokolwiek nadpiszemy.
     */
    val foundSnapshot: MutableStateFlow<BackupData?> = MutableStateFlow(null)

    /** Plik kopii przygotowany do wyslania na zewnatrz. */
    val shareRequest: MutableStateFlow<Uri?> = MutableStateFlow(null)

    val settings: StateFlow<Settings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

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
            todos.open(Bucket.GLOWNE),
            todos.completedLastDay(Bucket.GLOWNE),
            todos.rules(),
        ) { open, done, rules ->
            val byId = rules.associateBy { it.id }
            fun onToday(t: Todo) = t.plannedDate != null && !t.plannedDate.isAfter(day)
            // Zadania cykliczne starzeja sie tak samo jak reszta - zalegly czynsz ma
            // czerniec, bo inaczej nic nie sygnalizuje spoznienia. Nie starzeja sie
            // wylacznie nawyki, ktore faktycznie resetuja sie co dobe.
            fun map(list: List<Todo>) = list.map {
                TodoUi(
                    todo = it,
                    ageDays = Aging.ageDays(it.plannedDate, day),
                    onTodayList = onToday(it),
                    rule = it.recurrenceId?.let(byId::get),
                    hasFutureDate = it.plannedDate?.isAfter(day) == true,
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

    /**
     * Momentum i siatka licza sie z historii przy kazdym odczycie - patrz komentarz
     * w [Momentum]. Historia idzie jednym zapytaniem dla wszystkich nawykow naraz.
     */
    private val habitsWithHistory: Flow<List<HabitUi>> = today.flatMapLatest { day ->
        combine(
            habits.active(),
            habits.completionsSince(day.minusDays(400)),
            settingsStore.settings,
        ) { list, completions, appSettings ->
            val vacation = appSettings.vacation
            val byHabit = completions.groupBy({ it.habitId }, { it.date })
                .mapValues { (_, dates) -> dates.toSet() }

            list.map { habit ->
                val done = byHabit[habit.id].orEmpty()
                HabitUi(
                    habit = habit,
                    momentum = Momentum.compute(habit, done, day, vacation),
                    doneToday = day in done,
                    scheduledToday = habit.isScheduledOn(day),
                    pausedToday = vacation?.covers(day) == true,
                    grid = Momentum.grid(habit, done, day, vacation),
                )
            }
        }
    }

    /** Nawyki, ktore wypadaja dzisiaj - ta lista dokleja sie do ekranu ToDo. */
    val todayHabits: StateFlow<List<HabitUi>> = habitsWithHistory
        .map { list -> list.filter { it.scheduledToday && !it.pausedToday } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val habitsState: StateFlow<List<HabitUi>> = habitsWithHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val somedayState: StateFlow<List<TodoUi>> = todos.open(Bucket.KIEDYS)
        .map { list -> list.map { TodoUi(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Wszystko z terminem w przyszlosci plus zadania cykliczne, najblizsze na gorze.
     * Cykliczne pokazuja sie tu niezaleznie od daty - takze te wypadajace dzisiaj albo
     * zalegle, bo one z definicji sa czescia planu i lista, ktora je gubi az do
     * odhaczenia, klamie o tym, co jest zaplanowane.
     */
    val scheduledState: StateFlow<List<TodoUi>> = today.flatMapLatest { day ->
        combine(todos.open(Bucket.GLOWNE), todos.rules()) { list, rules ->
            val byId = rules.associateBy { it.id }
            list.filter {
                it.plannedDate != null && (it.plannedDate.isAfter(day) || it.recurrenceId != null)
            }
                .sortedBy { it.plannedDate }
                .map {
                    TodoUi(
                        todo = it,
                        rule = it.recurrenceId?.let(byId::get),
                        hasFutureDate = it.plannedDate?.isAfter(day) == true,
                    )
                }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val shoppingState: StateFlow<ShoppingUiState> = today.flatMapLatest {
        combine(
            todos.open(Bucket.ZAKUPY),
            todos.completedLastDay(Bucket.ZAKUPY),
        ) { open, done ->
            ShoppingUiState(
                open = open.map { TodoUi(it) },
                done = done.map { TodoUi(it) },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingUiState())

    init {
        viewModelScope.launch { todos.purgeOldCompleted() }
        viewModelScope.launch {
            if (backups.isEmpty()) foundSnapshot.value = backups.localSnapshot()
        }
    }

    /** Wolane przy powrocie do aplikacji - lapie zmiane doby bez restartu. */
    fun refreshDay() {
        today.value = LocalDate.now()
    }

    // --- todosy ---

    fun add(title: String, bucket: Bucket = Bucket.GLOWNE, forToday: Boolean = false) =
        viewModelScope.launch {
            todos.add(
                title = title,
                bucket = bucket,
                plannedDate = if (forToday) today.value else null,
            )
        }

    fun addTask(title: String, date: LocalDate?, rule: Recurrence?) = viewModelScope.launch {
        todos.addTask(title, date, rule)
    }

    fun editTask(id: Long, title: String, date: LocalDate?, rule: Recurrence?) =
        viewModelScope.launch { todos.edit(id, title, date, rule) }

    fun toggleToday(id: Long) = viewModelScope.launch { todos.toggleToday(id, today.value) }

    fun setDone(id: Long, done: Boolean) = viewModelScope.launch {
        todos.setDone(id, done, today = today.value)
    }

    fun schedule(id: Long, date: LocalDate) = viewModelScope.launch { todos.schedule(id, date) }

    fun moveTo(id: Long, bucket: Bucket) = viewModelScope.launch { todos.moveTo(id, bucket) }

    fun delete(id: Long) = viewModelScope.launch { todos.delete(id) }


    fun clearShoppingDone() = viewModelScope.launch { todos.clearCompleted(Bucket.ZAKUPY) }

    // --- nawyki ---

    fun addHabit(name: String, daily: Boolean, days: Set<DayOfWeek>) = viewModelScope.launch {
        habits.add(name, daily, days)
    }

    fun saveHabit(habit: Habit) = viewModelScope.launch { habits.save(habit) }

    fun setHabitDone(habitId: Long, done: Boolean) = viewModelScope.launch {
        habits.setDone(habitId, today.value, done)
    }

    fun startVacation(until: LocalDate?) = viewModelScope.launch {
        settingsStore.startVacation(today.value, until)
    }

    fun endVacation() = viewModelScope.launch { settingsStore.endVacation() }

    fun setMorningReminder(reminder: Reminder) = viewModelScope.launch {
        settingsStore.setMorning(reminder)
    }

    fun setAfternoonReminder(reminder: Reminder) = viewModelScope.launch {
        settingsStore.setAfternoon(reminder)
    }

    fun setShoppingKeywords(text: String) = viewModelScope.launch {
        settingsStore.setShoppingKeywords(text)
    }

    // --- kopia zapasowa ---

    fun setBackupFolder(uri: String?) = viewModelScope.launch {
        settingsStore.setBackupFolder(uri)
        backupMessage.value = if (uri == null) null else "Folder ustawiony."
    }

    fun backupNow() = viewModelScope.launch {
        val folder = settingsStore.settings.first().backupFolder
        if (folder == null) {
            backupMessage.value = "Najpierw wskaż folder na kopie."
            return@launch
        }

        val name = backups.writeTo(Uri.parse(folder), today.value)
        if (name == null) {
            backupMessage.value = "Nie udało się zapisać. Wskaż folder jeszcze raz."
        } else {
            settingsStore.setLastBackup(today.value)
            backupMessage.value = "Zapisane: $name"
        }
    }

    fun shareBackup() = viewModelScope.launch {
        val uri = backups.fileToShare(today.value)
        if (uri == null) {
            backupMessage.value = "Nie udało się przygotować pliku."
        } else {
            shareRequest.value = uri
        }
    }

    fun shareHandled() {
        shareRequest.value = null
    }

    fun restoreFoundSnapshot() = viewModelScope.launch {
        val data = foundSnapshot.value ?: return@launch
        backups.restore(data)
        foundSnapshot.value = null
        backupMessage.value = "Przywrócone z kopii."
    }

    fun dismissFoundSnapshot() {
        foundSnapshot.value = null
    }

    fun restoreFrom(uri: Uri) = viewModelScope.launch {
        val data = backups.readFrom(uri)
        if (data == null) {
            backupMessage.value = "To nie wygląda na kopię Momentum."
            return@launch
        }

        backups.restore(data)
        foundSnapshot.value = null
        backupMessage.value = "Wczytane: ${data.todos.size} zadań, ${data.habits.size} nawyków."
    }

    fun archiveHabit(habitId: Long, archived: Boolean) = viewModelScope.launch {
        habits.setArchived(habitId, archived)
    }

    fun deleteHabit(habitId: Long) = viewModelScope.launch { habits.delete(habitId) }

    companion object {
        fun factory(
            todos: TodoRepository,
            habits: HabitRepository,
            settings: SettingsStore,
            backups: BackupRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { MomentumViewModel(todos, habits, settings, backups) }
        }
    }
}
