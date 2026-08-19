package dev.slusa.momentum.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY sortIndex ASC, id ASC")
    fun observeActive(): Flow<List<Habit>>

    @Query("SELECT * FROM habits ORDER BY archived ASC, sortIndex ASC, id ASC")
    fun observeAll(): Flow<List<Habit>>

    /**
     * Historia od podanej daty. Momentum siega maksymalnie kilkuset dni wstecz,
     * wiec nie ma sensu wciagac do pamieci wszystkiego od poczatku swiata.
     */
    @Query("SELECT * FROM habit_completions WHERE date >= :since")
    fun observeCompletionsSince(since: LocalDate): Flow<List<HabitCompletion>>

    @Insert
    suspend fun insert(habit: Habit): Long

    @Update
    suspend fun update(habit: Habit)

    @Delete
    suspend fun delete(habit: Habit)

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun byId(id: Long): Habit?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markDone(completion: HabitCompletion)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun unmarkDone(habitId: Long, date: LocalDate)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId")
    suspend fun deleteHistory(habitId: Long)

    @Query("SELECT COALESCE(MAX(sortIndex), 0) + 1 FROM habits")
    suspend fun nextSortIndex(): Int
}
