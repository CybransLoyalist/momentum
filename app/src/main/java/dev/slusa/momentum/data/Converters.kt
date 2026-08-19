package dev.slusa.momentum.data

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/**
 * Daty trzymamy jako ISO ("2026-08-19"), chwile jako milisekundy epoki.
 * Data w postaci tekstu jest czytelna w podgladzie bazy i poprawnie sortuje sie
 * leksykograficznie, wiec zapytania po zakresie dat dzialaja bez konwersji.
 */
class Converters {
    @TypeConverter
    fun dateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}
