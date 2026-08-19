package dev.slusa.momentum.data

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

/**
 * firstTodayDate okazalo sie duplikatem plannedDate: niedokonczone zadanie zachowuje
 * swoja date przy rolloverze, wiec ta sama kolumna wyznacza juz punkt odniesienia
 * dla wieku. Dwa pola trzymane w zgodzie recznie to gotowa przyszla awaria.
 *
 * Migracja automatyczna, wiec dane z wersji 1 przezywaja aktualizacje.
 */
@DeleteColumn(tableName = "todos", columnName = "firstTodayDate")
class DropFirstTodayDate : AutoMigrationSpec

/**
 * Pauza per-nawyk ustapila globalnemu trybowi urlopowemu. Urlop jest stanem calej
 * osoby, a nie pojedynczego nawyku, a dwa mechanizmy robiace to samo predzej czy
 * pozniej sie rozjezdzaja. Nowy stan mieszka w SettingsStore, nie w bazie.
 */
@DeleteColumn(tableName = "habits", columnName = "pausedFrom")
@DeleteColumn(tableName = "habits", columnName = "pausedTo")
class DropPerHabitPause : AutoMigrationSpec
