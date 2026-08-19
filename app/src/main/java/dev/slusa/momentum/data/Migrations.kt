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
