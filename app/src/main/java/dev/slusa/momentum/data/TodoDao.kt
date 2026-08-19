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

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun byId(id: Long): Todo?

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
}
