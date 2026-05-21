package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emails")
data class Email(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val senderCombined: String, // e.g., "Akhilesh Singh <akhilesh@example.com>"
    val receiver: String, // e.g., "support@unifiedtracker.com"
    val body: String,
    val timestamp: Long,
    val category: String, // e.g., "Support", "Inquiry", "Billing", "Updates", "Internal", "Spam"
    val isRead: Boolean = false,
    val isOutgoing: Boolean = false
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderNumber: String, // e.g., "ORD-12345"
    val customerName: String,
    val customerEmail: String,
    val itemsJson: String, // Serialized list of order items, e.g., '[{"name":"Leather Wallet","quantity":1,"price":49.99}]'
    val totalAmount: Double,
    val status: String, // "Placed", "Processing", "Shipped", "Delivered", "Cancelled"
    val carrier: String, // "UPS", "FedEx", "DHL", "USPS", "None"
    val trackingNumber: String,
    val timestamp: Long,
    val deliveryEstimate: String // e.g., "2026-05-25" or "May 25, 2026"
)

@Entity(tableName = "tickets")
data class Ticket(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ticketNumber: String, // e.g., "TKT-54321"
    val subject: String,
    val customerEmail: String,
    val description: String,
    val status: String, // "New", "In Progress", "Pending Info", "Resolved"
    val priority: String, // "Low", "Medium", "High"
    val category: String, // "Billing", "Technical Support", "Delivery Issue", "Returns", "General"
    val timestamp: Long,
    val resolutionNotes: String? = null, // exact solution provided
    val resolutionTimeline: String? = null, // time taken to resolve of form e.g. "3 hours", "1 day"
    val resolutionStatus: String? = null // "Verified", "Closed", "Pending Partner Feedback" or null if unresolved
)
