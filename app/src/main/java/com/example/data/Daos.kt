package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailDao {
    @Query("SELECT * FROM emails ORDER BY timestamp DESC")
    fun getAllEmails(): Flow<List<Email>>

    @Query("SELECT * FROM emails WHERE category = :category ORDER BY timestamp DESC")
    fun getEmailsByCategory(category: String): Flow<List<Email>>

    @Query("SELECT * FROM emails WHERE subject LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%' OR senderCombined LIKE '%' || :query || '%'")
    fun searchEmails(query: String): Flow<List<Email>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmail(email: Email)

    @Update
    suspend fun updateEmail(email: Email)

    @Query("UPDATE emails SET isRead = :isRead WHERE id = :id")
    suspend fun markAsRead(id: Int, isRead: Boolean)

    @Delete
    suspend fun deleteEmail(email: Email)

    @Query("DELETE FROM emails")
    suspend fun deleteAllEmails()
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE status = :status ORDER BY timestamp DESC")
    fun getOrdersByStatus(status: String): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE orderNumber LIKE '%' || :query || '%' OR customerName LIKE '%' || :query || '%' OR customerEmail LIKE '%' || :query || '%'")
    fun searchOrders(query: String): Flow<List<Order>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order)

    @Update
    suspend fun updateOrder(order: Order)

    @Delete
    suspend fun deleteOrder(order: Order)

    @Query("DELETE FROM orders")
    suspend fun deleteAllOrders()
}

@Dao
interface TicketDao {
    @Query("SELECT * FROM tickets ORDER BY timestamp DESC")
    fun getAllTickets(): Flow<List<Ticket>>

    @Query("SELECT * FROM tickets WHERE status = :status ORDER BY timestamp DESC")
    fun getTicketsByStatus(status: String): Flow<List<Ticket>>

    @Query("SELECT * FROM tickets WHERE ticketNumber LIKE '%' || :query || '%' OR subject LIKE '%' || :query || '%' OR customerEmail LIKE '%' || :query || '%'")
    fun searchTickets(query: String): Flow<List<Ticket>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: Ticket)

    @Update
    suspend fun updateTicket(ticket: Ticket)

    @Delete
    suspend fun deleteTicket(ticket: Ticket)

    @Query("DELETE FROM tickets")
    suspend fun deleteAllTickets()
}
