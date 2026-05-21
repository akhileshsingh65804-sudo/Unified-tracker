package com.example.ui.tracker

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.GeminiContent
import com.example.api.GeminiGenerationConfig
import com.example.api.GeminiPart
import com.example.api.GeminiRequest
import com.example.api.RetrofitClient
import com.example.data.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class TrackerViewModel(private val repository: UnifiedRepository) : ViewModel() {

    // Filter states
    val searchQuery = MutableStateFlow("")
    val emailFilterCategory = MutableStateFlow("All")
    val orderFilterStatus = MutableStateFlow("All")
    val ticketFilterStatus = MutableStateFlow("All")

    // Async operation status message
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isProcessingAI = MutableStateFlow(false)
    val isProcessingAI: StateFlow<Boolean> = _isProcessingAI.asStateFlow()

    // Database streams
    val emails: StateFlow<List<Email>> = repository.allEmails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<Order>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tickets: StateFlow<List<Ticket>> = repository.allTickets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered lists
    val filteredEmails: StateFlow<List<Email>> = combine(emails, searchQuery, emailFilterCategory) { list, query, cat ->
        list.filter { email ->
            val matchesQuery = query.isEmpty() || 
                    email.subject.contains(query, ignoreCase = true) || 
                    email.body.contains(query, ignoreCase = true) || 
                    email.senderCombined.contains(query, ignoreCase = true)
            val matchesCat = cat == "All" || email.category.equals(cat, ignoreCase = true)
            matchesQuery && matchesCat
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredOrders: StateFlow<List<Order>> = combine(orders, searchQuery, orderFilterStatus) { list, query, status ->
        list.filter { order ->
            val matchesQuery = query.isEmpty() || 
                    order.orderNumber.contains(query, ignoreCase = true) || 
                    order.customerName.contains(query, ignoreCase = true) || 
                    order.customerEmail.contains(query, ignoreCase = true)
            val matchesStatus = status == "All" || order.status.equals(status, ignoreCase = true)
            matchesQuery && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTickets: StateFlow<List<Ticket>> = combine(tickets, searchQuery, ticketFilterStatus) { list, query, status ->
        list.filter { ticket ->
            val matchesQuery = query.isEmpty() || 
                    ticket.ticketNumber.contains(query, ignoreCase = true) || 
                    ticket.subject.contains(query, ignoreCase = true) || 
                    ticket.customerEmail.contains(query, ignoreCase = true)
            val matchesStatus = status == "All" || ticket.status.equals(status, ignoreCase = true)
            matchesQuery && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Pre-populate data if database is empty
        viewModelScope.launch {
            emails.first { true } // Trigger collection
            if (emails.value.isEmpty() && orders.value.isEmpty() && tickets.value.isEmpty()) {
                prepopulateWithMockData()
            }
        }
    }

    private suspend fun prepopulateWithMockData() {
        val email1 = Email(
            subject = "Order Placed & Processing: #ORD-88291",
            senderCombined = "orders@ecommerce.com",
            receiver = "akhileshsingh65804@gmail.com",
            body = "Hi Sarah,\n\nThank you for your purchase! Your order #ORD-88291 for Leather Wallet + Classic Belt ($69.98) has been received and is currently under processing.\n\nBest,\nLogistics Team",
            timestamp = System.currentTimeMillis() - 7200000, // 2 hours ago
            category = "Orders",
            isRead = true
        )
        val email2 = Email(
            subject = "URGENT Technical Support: App FREEZES on double tap",
            senderCombined = "Akhilesh Singh <akhileshsingh65804@gmail.com>",
            receiver = "support@unifiedtracker.com",
            body = "Hello staff, the app keeps freezing when I double tap the signup action. I'm using Android 14. Let me know what steps are needed to debug this critical error. Thanks!",
            timestamp = System.currentTimeMillis() - 14400000, // 4 hours ago
            category = "Support",
            isRead = false
        )
        val email3 = Email(
            subject = "Invoice Payment Succeeded - June 2026",
            senderCombined = "billing@unifiedtracker.com",
            receiver = "akhileshsingh65804@gmail.com",
            body = "Your professional tier subscription of $49.00 has been paid successfully. Thank you for choosing us!",
            timestamp = System.currentTimeMillis() - 28800000, // 8 hours ago
            category = "Billing",
            isRead = true
        )
        val email4 = Email(
            subject = "Inquiry: Shipping carrier partnerships?",
            senderCombined = "Partner Agent <partner@carrier-corp.com>",
            receiver = "logistics@unifiedtracker.com",
            body = "Greetings, we want to pitch our low-cost API courier integration with your dashboard. Can we jump on a call?",
            timestamp = System.currentTimeMillis() - 86400000, // 1 day ago
            category = "Inquiry",
            isRead = true
        )

        val order1 = Order(
            orderNumber = "ORD-88291",
            customerName = "Sarah Jenkins",
            customerEmail = "sarah@gmail.com",
            itemsJson = "[{\"name\":\"Leather Wallet\",\"qty\":1,\"price\":49.99},{\"name\":\"Classic Belt\",\"qty\":1,\"price\":19.99}]",
            totalAmount = 69.98,
            status = "Shipped",
            carrier = "FedEx",
            trackingNumber = "FX-8837192",
            timestamp = System.currentTimeMillis() - 7200000,
            deliveryEstimate = "May 24, 2026"
        )
        val order2 = Order(
            orderNumber = "ORD-71952",
            customerName = "Akhilesh Singh",
            customerEmail = "akhileshsingh65804@gmail.com",
            itemsJson = "[{\"name\":\"Premium Wireless Headphones\",\"qty\":1,\"price\":199.99}]",
            totalAmount = 199.99,
            status = "Processing",
            carrier = "DHL",
            trackingNumber = "DHL-9817293",
            timestamp = System.currentTimeMillis() - 18000000, // 5 hours ago
            deliveryEstimate = "May 26, 2026"
        )
        val order3 = Order(
            orderNumber = "ORD-48201",
            customerName = "Sam R.",
            customerEmail = "sam@example.com",
            itemsJson = "[{\"name\":\"Microphone Desk Mount\",\"qty\":1,\"price\":29.50}]",
            totalAmount = 29.50,
            status = "Delivered",
            carrier = "UPS",
            trackingNumber = "US-3829192",
            timestamp = System.currentTimeMillis() - 86400000,
            deliveryEstimate = "May 20, 2026"
        )

        val ticket1 = Ticket(
            ticketNumber = "TKT-54321",
            subject = "App freezing on signup double tap",
            customerEmail = "akhileshsingh65804@gmail.com",
            description = "App keeps freezing on signup forms when I click multiple times quickly. High-priority bug on Android 14.",
            status = "In Progress",
            priority = "High",
            category = "Technical Support",
            timestamp = System.currentTimeMillis() - 14400000
        )
        val ticket2 = Ticket(
            ticketNumber = "TKT-10928",
            subject = "Received double billing for Professional Tier",
            customerEmail = "akhileshsingh65804@gmail.com",
            description = "Stripe invoiced our credit card twice for $49.00 on May 20. Need a refund of one charge.",
            status = "New",
            priority = "Medium",
            category = "Billing",
            timestamp = System.currentTimeMillis() - 28800000
        )
        val ticket3 = Ticket(
            ticketNumber = "TKT-29182",
            subject = "Delivered package not found in mailbox",
            customerEmail = "jess@example.com",
            description = "The tracking status on my order ORD-48201 says delivered, but it is not at our mailbox or porch! Please refund or open courier lookup.",
            status = "Resolved",
            priority = "High",
            category = "Delivery Issue",
            timestamp = System.currentTimeMillis() - 86400000,
            resolutionNotes = "Contacted USPS dispatch supervisor. Mail carrier accidentally delivered to next door unit 14B instead of 14A. Retrieved package and delivered to correct customer unit. Customer verified safe receipt.",
            resolutionTimeline = "6 hours",
            resolutionStatus = "Closed"
        )

        repository.insertEmail(email1)
        repository.insertEmail(email2)
        repository.insertEmail(email3)
        repository.insertEmail(email4)

        repository.insertOrder(order1)
        repository.insertOrder(order2)
        repository.insertOrder(order3)

        repository.insertTicket(ticket1)
        repository.insertTicket(ticket2)
        repository.insertTicket(ticket3)
    }

    // Helper functions for updating state
    fun search(query: String) {
        searchQuery.value = query
    }

    fun filterEmails(category: String) {
        emailFilterCategory.value = category
    }

    fun filterOrders(status: String) {
        orderFilterStatus.value = status
    }

    fun filterTickets(status: String) {
        ticketFilterStatus.value = status
    }

    fun clearStatus() {
        _statusMessage.value = null
    }

    // Insert actions
    fun addEmail(subject: String, sender: String, receiver: String, body: String, category: String, isOutgoing: Boolean) {
        viewModelScope.launch {
            val email = Email(
                subject = subject,
                senderCombined = sender,
                receiver = receiver,
                body = body,
                category = category,
                timestamp = System.currentTimeMillis(),
                isOutgoing = isOutgoing,
                isRead = isOutgoing
            )
            repository.insertEmail(email)
            showStatus("Email logged and categorized under '$category'")
        }
    }

    fun addOrder(orderNumber: String, name: String, email: String, items: String, total: Double, status: String, carrier: String, tracking: String, estimate: String) {
        viewModelScope.launch {
            val order = Order(
                orderNumber = orderNumber,
                customerName = name,
                customerEmail = email,
                itemsJson = items,
                totalAmount = total,
                status = status,
                carrier = carrier,
                trackingNumber = tracking,
                timestamp = System.currentTimeMillis(),
                deliveryEstimate = estimate
            )
            repository.insertOrder(order)
            showStatus("E-commerce Order #$orderNumber created: status '$status'")
        }
    }

    fun addTicket(subject: String, email: String, description: String, priority: String, category: String, status: String = "New") {
        viewModelScope.launch {
            val randomNum = (10000..99999).random()
            val ticket = Ticket(
                ticketNumber = "TKT-$randomNum",
                subject = subject,
                customerEmail = email,
                description = description,
                status = status,
                priority = priority,
                category = category,
                timestamp = System.currentTimeMillis()
            )
            repository.insertTicket(ticket)
            showStatus("Created Ticket ${ticket.ticketNumber} categorized of '$category'")
        }
    }

    // Sync mock activity influx (creates a random notification/sync state)
    fun simulateSyncInflux() {
        viewModelScope.launch {
            val prompts = listOf(
                "Inquiry: Can I get volume discounts on shipments? from corp@solutions.com",
                "Order Update: Place ORD-28192 for Akhilesh. 1x Wireless Mouse ($29.99), status Placed. akhileshsingh65804@gmail.com",
                "Ticket Complaint: Server error 500 when exporting tracking list. billing issue. high priority from tech@devs.com"
            )
            val selected = prompts.random()
            analyzeAndImportText(selected)
        }
    }

    // Update statuses of Tickets or Orders
    fun updateOrderStatus(order: Order, newStatus: String) {
        viewModelScope.launch {
            val updated = order.copy(status = newStatus)
            repository.updateOrder(updated)
            showStatus("Order #${order.orderNumber} updated to '$newStatus'")
        }
    }

    fun resolveTicket(ticket: Ticket, notes: String, timeline: String, status: String = "Closed") {
        viewModelScope.launch {
            val updated = ticket.copy(
                status = "Resolved",
                resolutionNotes = notes,
                resolutionTimeline = timeline,
                resolutionStatus = status
            )
            repository.updateTicket(updated)
            showStatus("Ticket ${ticket.ticketNumber} resolved & logs finalized")
        }
    }

    fun updateEmailRead(email: Email, isRead: Boolean) {
        viewModelScope.launch {
            repository.markEmailAsRead(email.id, isRead)
        }
    }

    fun deleteEmail(email: Email) {
        viewModelScope.launch {
            repository.deleteEmail(email)
            showStatus("Communication record deleted")
        }
    }

    fun deleteOrder(order: Order) {
        viewModelScope.launch {
            repository.deleteOrder(order)
            showStatus("Order tracker cleared")
        }
    }

    fun deleteTicket(ticket: Ticket) {
        viewModelScope.launch {
            repository.deleteTicket(ticket)
            showStatus("Ticket record cleared")
        }
    }

    private fun showStatus(msg: String) {
        _statusMessage.value = msg
    }

    // AI TEXT PARSING AND CENTRALIZED INSERTION
    fun analyzeAndImportText(rawText: String) {
        viewModelScope.launch {
            _isProcessingAI.value = true
            val parsedResult = parseTextWithAI(rawText)
            _isProcessingAI.value = false

            if (parsedResult != null) {
                saveParsedResultToDb(parsedResult)
            } else {
                showStatus("Failed to analyze content - try again with clearer phrases.")
            }
        }
    }

    // AI SUGGEST RESOLUTION FOR TICKETS
    fun suggestTicketResolution(ticket: Ticket, onResult: (notes: String, timeline: String) -> Unit) {
        viewModelScope.launch {
            _isProcessingAI.value = true
            val (notes, timeline) = getSuggestedResolutionFromAI(ticket)
            _isProcessingAI.value = false
            onResult(notes, timeline)
        }
    }

    // INTERNAL API OPERATIONS
    private suspend fun parseTextWithAI(text: String): ParsedResponse? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasKey = apiKey.isNotEmpty() && !apiKey.startsWith("MY_")

        if (hasKey) {
            val systemInstructions = """
                You are a data extraction bot for a Centralized Unified Tracking CRM.
                Analyze the input text and extract whether it is a support Ticket, an e-commerce Order, or a general/inquiry Email.
                You must respond in strict JSON matching this structure:
                {
                  "type": "Email" or "Order" or "Ticket",
                  "subject": "Extracted summary subject",
                  "emailAddress": "Extracted customer email address",
                  "customerName": "Extracted customer name if any, otherwise default to Email prefix",
                  "body": "Re-styled body of the message",
                  "orderNumber": "Extracted order ID like ORD-XXXXX if any",
                  "items": [{"name": "Item name", "qty": 1, "price": 49.99}],
                  "totalAmount": 49.99,
                  "carrier": "Courier carrier e.g. FedEx, DHL, UPS, USPS or None",
                  "trackingNumber": "Courier tracking number if any",
                  "priority": "Low" or "Medium" or "High",
                  "category": "Extracted category (Billing, Technical, Inquiry, Refunds, Support, General)"
                }
                Make sure you return exactly a valid JSON block containing only this schema. Avoid any extra wrapper text.
            """.trimIndent()

            val prompt = "Extract details from this text and return the JSON: \"$text\""
            
            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstructions))),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.1f
                )
            )

            try {
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                Log.d("TrackerVM", "AI Raw response: $responseText")
                return@withContext parseJsonResponse(responseText)
            } catch (e: Exception) {
                Log.e("TrackerVM", "Failed to call Gemini API, fallback to regex", e)
            }
        }

        // Fallback local regex parsing
        return@withContext runLocalParsingFallback(text)
    }

    private suspend fun getSuggestedResolutionFromAI(ticket: Ticket): Pair<String, String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasKey = apiKey.isNotEmpty() && !apiKey.startsWith("MY_")

        if (hasKey) {
            val systemInstructions = """
                You are a senior customer support engineering advisor.
                Analyze the ticket details (Subject, Description, Category, Priority) and write a professional, detailed resolution note that logs the solution, actions taken, and the estimated duration/timeline (e.g., '2 hours', '1 day').
                You must return a valid JSON structure:
                {
                   "notes": "Steps taken to resolve",
                   "timeline": "e.g., 2 hours, 1 day, etc"
                }
            """.trimIndent()

            val prompt = "Suggest a resolution for ticket ${ticket.ticketNumber} subject: '${ticket.subject}' description: '${ticket.description}' category: '${ticket.category}'"
            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstructions))),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.4f
                )
            )

            try {
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val json = JSONObject(responseText)
                val notes = json.optString("notes", "No notes generated.")
                val timeline = json.optString("timeline", "1 hour")
                return@withContext Pair(notes, timeline)
            } catch (e: Exception) {
                Log.e("TrackerVM", "Gemini Resolution error, run fallback", e)
            }
        }

        // Fallback local resolutions based on categories
        val ticketText = (ticket.subject + " " + ticket.description).lowercase()
        return@withContext when {
            ticketText.contains("billing") || ticketText.contains("charge") || ticketText.contains("double") -> {
                Pair(
                    "Investigated transaction logs in payment provider console. Located duplicate charge triggered by concurrent network requests on submission. Initiated an instant refund of $49.00 via Stripe gateway. Notified system administrator to adjust the API gateway throttle.",
                    "3 hours"
                )
            }
            ticketText.contains("crash") || ticketText.contains("freeze") || ticketText.contains("hang") -> {
                Pair(
                    "Identified main-thread synchronization issue on click throttle mechanism. The double tap event triggered nested database updates synchronously. Configured button action states to throttle multiple quick clicks. Hotfix app update compiled and deployed successfully.",
                    "1 day"
                )
            }
            ticketText.contains("deliver") || ticketText.contains("package") || ticketText.contains("track") || ticketText.contains("not found") -> {
                Pair(
                    "Contacted priority carrier support supervisor to trace geo-location tags. Carrier confirmed delivery coordinates matched adjacent block mailbox instead of correct customer porch. Retrieved the cargo parcel and successfully delivered it manually to the correct customer unit.",
                    "5 hours"
                )
            }
            else -> {
                Pair(
                    "Reviewed account credentials and configuration files. Corrected internal database schema mismatch causing incorrect data displays. Reset UI cache, logged in with test profile, and confirmed all parameters load correctly. Case resolved successfully.",
                    "2 hours"
                )
            }
        }
    }

    private fun parseJsonResponse(jsonString: String): ParsedResponse? {
        try {
            val textToParse = if (jsonString.contains("```json")) {
                jsonString.substringAfter("```json").substringBefore("```").trim()
            } else if (jsonString.contains("```")) {
                jsonString.substringAfter("```").substringBefore("```").trim()
            } else {
                jsonString.trim()
            }

            val json = JSONObject(textToParse)
            val type = json.optString("type", "Email")
            val subject = json.optString("subject", "Sync Update")
            val emailAddress = json.optString("emailAddress", "client@example.com")
            val customerName = json.optString("customerName", "Client Profile")
            val body = json.optString("body", "")
            val orderNumber = json.optString("orderNumber", "ORD-${(10000..99999).random()}")
            
            val itemsArr = json.optJSONArray("items")
            val moshiItems = mutableListOf<Map<String, Any>>()
            if (itemsArr != null) {
                for (i in 0 until itemsArr.length()) {
                    val item = itemsArr.getJSONObject(i)
                    moshiItems.add(mapOf(
                        "name" to item.optString("name", "Product Care"),
                        "qty" to item.optInt("qty", 1),
                        "price" to item.optDouble("price", 29.99)
                    ))
                }
            } else {
                moshiItems.add(mapOf("name" to "Sync Item Package", "qty" to 1, "price" to 49.99))
            }
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(List::class.java)
            val itemsJson = adapter.toJson(moshiItems)

            val totalAmount = json.optDouble("totalAmount", 49.99)
            val carrier = json.optString("carrier", "UPS")
            val trackingNumber = json.optString("trackingNumber", "US-${(100000..999999).random()}")
            val priority = json.optString("priority", "Medium")
            val category = json.optString("category", "General")

            return ParsedResponse(
                type = type,
                subject = subject,
                customerEmail = emailAddress,
                customerName = customerName,
                body = body,
                orderNumber = orderNumber,
                itemsJson = itemsJson,
                totalAmount = totalAmount,
                carrier = carrier,
                trackingNumber = trackingNumber,
                priority = priority,
                category = category
            )
        } catch (e: Exception) {
            Log.e("TrackerVM", "Moshi json mapping error", e)
            return null
        }
    }

    private fun runLocalParsingFallback(text: String): ParsedResponse {
        val cleanText = text.lowercase()
        val emailRegex = "([a-zA-Z0-9._-]+@[a-zA-Z0-9._-]+\\.[a-zA-Z0-9._-]+)".toRegex()
        val foundEmail = emailRegex.find(text)?.value ?: "akhileshsingh65804@gmail.com"
        val namePrefix = foundEmail.substringBefore("@")

        val ordRegex = "(ORD-\\d+)".toRegex()
        val foundOrderNum = ordRegex.find(text)?.value ?: "ORD-${(10000..99999).random()}"

        return when {
            cleanText.contains("order") || cleanText.contains("bought") || cleanText.contains("purchased") || cleanText.contains("items") -> {
                // Return an Order structure
                val total = if (cleanText.contains("$")) {
                    val valStr = cleanText.substringAfter("$").takeWhile { it.isDigit() || it == '.' }
                    valStr.toDoubleOrNull() ?: 129.99
                } else 129.99

                val items = "[{\"name\":\"Premium Purchase Box\",\"qty\":1,\"price\":$total}]"
                val status = if (cleanText.contains("processing")) "Processing" else "Placed"

                ParsedResponse(
                    type = "Order",
                    subject = "Sync Order update #$foundOrderNum",
                    customerEmail = foundEmail,
                    customerName = namePrefix.replaceFirstChar { it.uppercase() },
                    body = text,
                    orderNumber = foundOrderNum,
                    itemsJson = items,
                    totalAmount = total,
                    carrier = "DHL",
                    trackingNumber = "DHL-${(10000..99999).random()}",
                    priority = "Medium",
                    category = "Inquiry"
                )
            }
            cleanText.contains("ticket") || cleanText.contains("complaint") || cleanText.contains("crash") || cleanText.contains("refund") || cleanText.contains("issue") -> {
                // Return a Support Ticket structure
                val priority = if (cleanText.contains("urgent") || cleanText.contains("high")) "High" else "Medium"
                val category = when {
                    cleanText.contains("billing") || cleanText.contains("refund") -> "Billing"
                    cleanText.contains("crash") || cleanText.contains("freeze") -> "Technical Support"
                    else -> "Delivery Issue"
                }

                ParsedResponse(
                    type = "Ticket",
                    subject = "Sync Ticket support for: " + text.take(45) + "...",
                    customerEmail = foundEmail,
                    customerName = namePrefix.replaceFirstChar { it.uppercase() },
                    body = text,
                    orderNumber = foundOrderNum,
                    itemsJson = "[]",
                    totalAmount = 0.0,
                    carrier = "None",
                    trackingNumber = "",
                    priority = priority,
                    category = category
                )
            }
            else -> {
                // Return an Email structure
                val category = when {
                    cleanText.contains("billing") -> "Billing"
                    cleanText.contains("partnership") || cleanText.contains("deals") -> "Inquiry"
                    cleanText.contains("help") || cleanText.contains("support") -> "Support"
                    else -> "General"
                }

                ParsedResponse(
                    type = "Email",
                    subject = "Centralized Email digest: " + text.take(35) + "...",
                    customerEmail = foundEmail,
                    customerName = namePrefix.replaceFirstChar { it.uppercase() },
                    body = text,
                    orderNumber = "",
                    itemsJson = "[]",
                    totalAmount = 0.0,
                    carrier = "None",
                    trackingNumber = "",
                    priority = "Low",
                    category = category
                )
            }
        }
    }

    private suspend fun saveParsedResultToDb(parsed: ParsedResponse) {
        when (parsed.type) {
            "Order" -> {
                val order = Order(
                    orderNumber = parsed.orderNumber,
                    customerName = parsed.customerName,
                    customerEmail = parsed.customerEmail,
                    itemsJson = parsed.itemsJson,
                    totalAmount = parsed.totalAmount,
                    status = "Placed",
                    carrier = parsed.carrier,
                    trackingNumber = parsed.trackingNumber,
                    timestamp = System.currentTimeMillis(),
                    deliveryEstimate = "May 28, 2026"
                )
                repository.insertOrder(order)

                // Log a matching incoming Email
                val email = Email(
                    subject = "Incoming Order notification: #${parsed.orderNumber}",
                    senderCombined = "${parsed.customerName} <${parsed.customerEmail}>",
                    receiver = "logistics@unifiedtracker.com",
                    body = parsed.body,
                    timestamp = System.currentTimeMillis(),
                    category = "Orders",
                    isRead = false
                )
                repository.insertEmail(email)
                showStatus("Synchronized & Logged Order #${parsed.orderNumber} successfully!")
            }
            "Ticket" -> {
                val randomNum = (10000..99999).random()
                val ticket = Ticket(
                    ticketNumber = "TKT-$randomNum",
                    subject = parsed.subject,
                    customerEmail = parsed.customerEmail,
                    description = parsed.body,
                    status = "New",
                    priority = parsed.priority,
                    category = parsed.category,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertTicket(ticket)

                // Log a matching incoming Email
                val email = Email(
                    subject = "New Support Ticket requested: ${parsed.subject}",
                    senderCombined = "${parsed.customerName} <${parsed.customerEmail}>",
                    receiver = "support@unifiedtracker.com",
                    body = parsed.body,
                    timestamp = System.currentTimeMillis(),
                    category = "Support",
                    isRead = false
                )
                repository.insertEmail(email)
                showStatus("Categorized Support Ticket logged: TKT-$randomNum")
            }
            else -> {
                val email = Email(
                    subject = parsed.subject,
                    senderCombined = "${parsed.customerName} <${parsed.customerEmail}>",
                    receiver = "info@unifiedtracker.com",
                    body = parsed.body,
                    timestamp = System.currentTimeMillis(),
                    category = parsed.category,
                    isRead = false
                )
                repository.insertEmail(email)
                showStatus("Email parsed & synced into general inbox.")
            }
        }
    }
}

data class ParsedResponse(
    val type: String,
    val subject: String,
    val customerEmail: String,
    val customerName: String,
    val body: String,
    val orderNumber: String,
    val itemsJson: String,
    val totalAmount: Double,
    val carrier: String,
    val trackingNumber: String,
    val priority: String,
    val category: String
)

class TrackerViewModelFactory(private val repository: UnifiedRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrackerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TrackerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
