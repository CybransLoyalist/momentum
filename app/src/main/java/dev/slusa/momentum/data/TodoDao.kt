package dev.slusa.momentum.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface TodoDao {

    /** Wszystko otwarte w danym koszyku - podzial na sekcje robi warstwa wyzej. */
    @Query(
        """
        SELECT * FROM todos
        WHERE bucket = :bucket AND completedAt IS NULL
        ORDER BY sortIndex ASC, createdAt ASC
        """
    )
    fun observeOpen(bucket: Bucket): Flow<List<Todo>>

    /** Odhaczone od podanej chwili - to jest zwijana sekcja "Zrobione" na dole. */
    @Query(
        """
        SELECT * FROM todos
        WHERE bucket = :bucket AND completedAt IS NOT NULL AND completedAt >= :since
        ORDER BY completedAt DESC
        """
    )
    fun observeCompletedSince(bucket: Bucket, since: Instant): Flow<List<Todo>>

    /** Wszystko, lacznie z odhaczonym - do kopii zapasowej. */
    @Query("SELECT * FROM todos")
    suspend fun all(): List<Todo>

    @Query("DELETE FROM todos")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(todos: List<Todo>)

    /** Przy kasowaniu podlisty rzeczy z niej wracaja na glowna, zamiast ginac razem z nia. */
    @Query("UPDATE todos SET shoppingListId = NULL WHERE shoppingListId = :listId")
    suspend fun detachFromShoppingList(listId: Long)

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun byId(id: Long): Todo?

    /**
     * Otwarte instancje danej reguly. Z zalozenia jest ich zero albo jedna - lista
     * sluzy do sprzatania po cofnietym odhaczeniu, a nie do wyswietlania.
     */
    @Query("SELECT * FROM todos WHERE recurrenceId = :recurrenceId AND completedAt IS NULL")
    suspend fun openByRecurrence(recurrenceId: Long): List<Todo>

    @Insert
    suspend fun insert(todo: Todo): Long

    @Update
    suspend fun update(todo: Todo)

    @Delete
    suspend fun delete(todo: Todo)

    @Query("SELECT COALESCE(MIN(sortIndex), 0) - 1 FROM todos WHERE bucket = :bucket")
    suspend fun nextTopSortIndex(bucket: Bucket): Int

    /**
     * Odhaczone todosy kasujemy po dobie - nie ma potrzeby ich trzymac, a okno
     * cofniecia i tak konczy sie po 24 godzinach. Historia nawykow to osobna
     * tabela i jej to nie dotyczy.
     */
    @Query("DELETE FROM todos WHERE completedAt IS NOT NULL AND completedAt < :cutoff")
    suspend fun purgeCompletedBefore(cutoff: Instant): Int

    /** Przycisk "wyczysc odhaczone" na liscie zakupow. */
    @Query("DELETE FROM todos WHERE bucket = :bucket AND completedAt IS NOT NULL")
    suspend fun clearCompleted(bucket: Bucket): Int
}
