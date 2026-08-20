package dev.slusa.momentum.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Nazwana podlista zakupow: Ikea, Rossmann, przez internet.
 *
 * Lista glowna nie ma tu swojego wiersza i nie bedzie miala. Reprezentuje ja
 * [Todo.shoppingListId] rowne null - dzieki temu nie da sie jej skasowac, przemianowac
 * ani zgubic, a wszystkie dotychczasowe zakupy trafily na nia same, bez migracji danych.
 */
@Entity(tableName = "shopping_lists")
data class ShoppingList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    /** Kolejnosc dodania. Przestawiania na razie nie ma - listy sa dlugowieczne. */
    val sortIndex: Int = 0,
)
