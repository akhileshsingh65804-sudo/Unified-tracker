package com.example.data

import kotlinx.coroutines.flow.Flow

class UnifiedRepository(
    private val emailDao: EmailDao,
    private val orderDao: OrderDao,
    private val ticketDao: TicketDao
) {
    val allEmails: Flow<List<Email>> = emailDao.getAllEmails()
    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()
    val allTickets: Flow<List<Ticket>> = ticketDao.getAllTickets()

    fun searchEmails(query: String): Flow<List<Email>> = emailDao.searchEmails(query)
    fun searchOrders(query: String): Flow<List<Order>> = orderDao.searchOrders(query)
    fun searchTickets(query: String): Flow<List<Ticket>> = ticketDao.searchTickets(query)

    suspend fun insertEmail(email: Email) = emailDao.insertEmail(email)
    suspend fun updateEmail(email: Email) = emailDao.updateEmail(email)
    suspend fun markEmailAsRead(id: Int, isRead: Boolean) = emailDao.markAsRead(id, isRead)
    suspend fun deleteEmail(email: Email) = emailDao.deleteEmail(email)

    suspend fun insertOrder(order: Order) = orderDao.insertOrder(order)
    suspend fun updateOrder(order: Order) = orderDao.updateOrder(order)
    suspend fun deleteOrder(order: Order) = orderDao.deleteOrder(order)

    suspend fun insertTicket(ticket: Ticket) = ticketDao.insertTicket(ticket)
    suspend fun updateTicket(ticket: Ticket) = ticketDao.updateTicket(ticket)
    suspend fun deleteTicket(ticket: Ticket) = ticketDao.deleteTicket(ticket)

    suspend fun clearAllData() {
        emailDao.deleteAllEmails()
        orderDao.deleteAllOrders()
        ticketDao.deleteAllTickets()
    }
}
