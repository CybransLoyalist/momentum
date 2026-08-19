package dev.slusa.momentum.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurrenceDao {

    /**
     * Regul jest tyle, ile zadan cyklicznych, czyli garstka - lista w calosci jest
     * tansza niz zapytanie na kazdy kafelek listy.
     */
    @Query("SELECT * FROM recurrences")
    fun observeAll(): Flow<List<Recurrence>>

    @Query("SELECT * FROM recurrences WHERE id = :id")
    suspend fun byId(id: Long): Recurrence?

    @Insert
    suspend fun insert(rule: Recurrence): Long

    @Update
    suspend fun update(rule: Recurrence)

    @Query("DELETE FROM recurrences WHERE id = :id")
    suspend fun delete(id: Long)
}
