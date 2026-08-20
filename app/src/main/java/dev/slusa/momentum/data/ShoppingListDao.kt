package dev.slusa.momentum.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {

    @Query("SELECT * FROM shopping_lists ORDER BY sortIndex ASC, id ASC")
    fun observeAll(): Flow<List<ShoppingList>>

    @Query("SELECT * FROM shopping_lists")
    suspend fun all(): List<ShoppingList>

    @Query("SELECT COALESCE(MAX(sortIndex), 0) + 1 FROM shopping_lists")
    suspend fun nextSortIndex(): Int

    @Insert
    suspend fun insert(list: ShoppingList): Long

    @Insert
    suspend fun insertAll(lists: List<ShoppingList>)

    @Update
    suspend fun update(list: ShoppingList)

    @Query("DELETE FROM shopping_lists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM shopping_lists")
    suspend fun deleteAll()
}
