package dev.slusa.momentum.backup

import dev.slusa.momentum.data.Bucket
import dev.slusa.momentum.data.Habit
import dev.slusa.momentum.data.HabitCompletion
import dev.slusa.momentum.data.Recurrence
import dev.slusa.momentum.data.RecurrenceMode
import dev.slusa.momentum.data.RecurrenceUnit
import dev.slusa.momentum.data.Todo
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate

/** Wszystko, co trafia do pliku kopii. */
data class BackupData(
    val todos: List<Todo> = emptyList(),
    val recurrences: List<Recurrence> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val completions: List<HabitCompletion> = emptyList(),
)

/**
 * Zamiana danych na JSON i z powrotem, pisana recznie.
 *
 * Biblioteka do serializacji zrobilaby to krocej, ale zwiazalaby format pliku z
 * nazwami pol w kodzie - zmiana nazwy kolumny cicho psulaby stare kopie. Tutaj format
 * jest jawny i zmiana nazwy w kodzie nie rusza pliku, dopoki nie ruszy sie tej mapy.
 *
 * Nieznane pola przy wczytywaniu ida na wartosc domyslna, a nie wywalaja calosci:
 * kopia sprzed dwoch wersji ma sie dac wczytac, nawet jesli czegos w niej brakuje.
 */
object BackupFormat {

    /** Podbijamy przy zmianie ksztaltu pliku, nie przy zmianie wersji bazy. */
    const val VERSION = 1

    fun toJson(data: BackupData, createdAt: Instant = Instant.now()): String {
        val root = JSONObject()
        root.put("wersja", VERSION)
        root.put("utworzono", createdAt.toString())

        root.put("todosy", JSONArray().apply {
            data.todos.forEach { todo ->
                put(
                    JSONObject()
                        .put("id", todo.id)
                        .put("tytul", todo.title)
                        .put("lista", todo.bucket.name)
                        .putOrNull("termin", todo.plannedDate?.toString())
                        .put("zTerminu", todo.fromSchedule)
                        .put("kolejnosc", todo.sortIndex)
                        .put("utworzono", todo.createdAt.toEpochMilli())
                        .putOrNull("odhaczono", todo.completedAt?.toEpochMilli())
                        .putOrNull("regulaId", todo.recurrenceId)
                )
            }
        })

        root.put("reguly", JSONArray().apply {
            data.recurrences.forEach { rule ->
                put(
                    JSONObject()
                        .put("id", rule.id)
                        .put("tryb", rule.mode.name)
                        .put("coIle", rule.everyN)
                        .put("jednostka", rule.unit.name)
                        .putOrNull("dzienMiesiaca", rule.anchorDay)
                )
            }
        })

        root.put("nawyki", JSONArray().apply {
            data.habits.forEach { habit ->
                put(
                    JSONObject()
                        .put("id", habit.id)
                        .put("nazwa", habit.name)
                        .put("codziennie", habit.daily)
                        .put("dniTygodnia", habit.weekdaysMask)
                        .put("zarchiwizowany", habit.archived)
                        .put("kolejnosc", habit.sortIndex)
                        .put("start", habit.startDate.toString())
                )
            }
        })

        root.put("odhaczenia", JSONArray().apply {
            data.completions.forEach { done ->
                put(
                    JSONObject()
                        .put("nawykId", done.habitId)
                        .put("data", done.date.toString())
                )
            }
        })

        return root.toString(2)
    }

    fun fromJson(text: String): BackupData {
        val root = JSONObject(text)

        return BackupData(
            todos = root.array("todosy").map { obj ->
                Todo(
                    id = obj.optLong("id"),
                    title = obj.optString("tytul"),
                    bucket = enumOr(obj.optString("lista"), Bucket.GLOWNE),
                    plannedDate = obj.dateOrNull("termin"),
                    fromSchedule = obj.optBoolean("zTerminu"),
                    sortIndex = obj.optInt("kolejnosc"),
                    createdAt = Instant.ofEpochMilli(
                        if (obj.has("utworzono")) obj.optLong("utworzono") else 0L
                    ),
                    completedAt = if (obj.isNull("odhaczono")) {
                        null
                    } else {
                        Instant.ofEpochMilli(obj.optLong("odhaczono"))
                    },
                    recurrenceId = if (obj.isNull("regulaId")) null else obj.optLong("regulaId"),
                )
            },
            recurrences = root.array("reguly").map { obj ->
                Recurrence(
                    id = obj.optLong("id"),
                    mode = enumOr(obj.optString("tryb"), RecurrenceMode.KALENDARZOWA),
                    everyN = obj.optInt("coIle", 1),
                    unit = enumOr(obj.optString("jednostka"), RecurrenceUnit.MIESIAC),
                    anchorDay = if (obj.isNull("dzienMiesiaca")) null else obj.optInt("dzienMiesiaca"),
                )
            },
            habits = root.array("nawyki").map { obj ->
                Habit(
                    id = obj.optLong("id"),
                    name = obj.optString("nazwa"),
                    daily = obj.optBoolean("codziennie", true),
                    weekdaysMask = obj.optInt("dniTygodnia"),
                    archived = obj.optBoolean("zarchiwizowany"),
                    sortIndex = obj.optInt("kolejnosc"),
                    startDate = obj.dateOrNull("start") ?: LocalDate.now(),
                )
            },
            completions = root.array("odhaczenia").mapNotNull { obj ->
                val date = obj.dateOrNull("data") ?: return@mapNotNull null
                HabitCompletion(habitId = obj.optLong("nawykId"), date = date)
            },
        )
    }

    private fun JSONObject.putOrNull(key: String, value: Any?): JSONObject =
        if (value == null) put(key, JSONObject.NULL) else put(key, value)

    private fun JSONObject.dateOrNull(key: String): LocalDate? {
        if (isNull(key)) return null
        return runCatching { LocalDate.parse(optString(key)) }.getOrNull()
    }

    private fun JSONObject.array(key: String): List<JSONObject> {
        val array = optJSONArray(key) ?: return emptyList()
        return (0 until array.length()).mapNotNull { array.optJSONObject(it) }
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String, fallback: T): T =
        runCatching { enumValueOf<T>(name) }.getOrDefault(fallback)
}
