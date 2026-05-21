package com.example.ui.tracker

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Observe streams from viewmodel
    val emails by viewModel.filteredEmails.collectAsStateWithLifecycle()
    val orders by viewModel.filteredOrders.collectAsStateWithLifecycle()
    val tickets by viewModel.filteredTickets.collectAsStateWithLifecycle()

    val totalEmailsCount by viewModel.emails.collectAsStateWithLifecycle()
    val totalOrdersCount by viewModel.orders.collectAsStateWithLifecycle()
    val totalTicketsCount by viewModel.tickets.collectAsStateWithLifecycle()

    val isProcessingAI by viewModel.isProcessingAI.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    // Screen navigation state
    var activeTab by remember { mutableStateOf("Dashboard") }

    // Dialog sheets state
    var showAddDialog by remember { mutableStateOf<String?>(null) } // "Email", "Order", "Ticket"
    var selectedEmail by remember { mutableStateOf<Email?>(null) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var selectedTicket by remember { mutableStateOf<Ticket?>(null) }

    // Search and Input Terminal state directly under Dashboard tab
    var inputTerminalText by remember { mutableStateOf("") }

    // Show toast for async operations
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(PrimaryTeal, SecondaryAmber)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "UNIFIED TRACKING TERMINAL",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Centralized Communications, Orders & Tickets",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.simulateSyncInflux()
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Simulate Sync Influx",
                            tint = PrimaryTeal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                val tabs = listOf("Dashboard", "Emails", "Orders", "Tickets")
                val icons = listOf(
                    Icons.Default.Home,
                    Icons.Default.Email,
                    Icons.Default.ShoppingCart,
                    Icons.Default.Warning
                )

                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        icon = {
                            Icon(
                                imageVector = icons[index],
                                contentDescription = tab
                            )
                        },
                        label = {
                            Text(
                                text = tab,
                                fontSize = 11.sp,
                                fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryTeal,
                            selectedTextColor = PrimaryTeal,
                            indicatorColor = SurfaceBorder,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = "Ticket" }, // Default to logging a ticket
                containerColor = PrimaryTeal,
                contentColor = Color(0xFF030712)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Log New Case"
                )
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            // Unify search box across all tabs for fast lookups
            if (activeTab != "Dashboard") {
                SearchBarComponent(
                    query = searchQuery,
                    onQueryChange = { viewModel.search(it) }
                )
            }

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                label = "TabContent"
            ) { currentTab ->
                when (currentTab) {
                    "Dashboard" -> DashboardView(
                        viewModel = viewModel,
                        emailsCount = totalEmailsCount,
                        ordersCount = totalOrdersCount,
                        ticketsCount = totalTicketsCount,
                        inputTerminalText = inputTerminalText,
                        onInputTextChange = { inputTerminalText = it },
                        isProcessingAI = isProcessingAI,
                        onEmailClick = { selectedEmail = it },
                        onOrderClick = { selectedOrder = it },
                        onTicketClick = { selectedTicket = it }
                    )
                    "Emails" -> EmailsListView(
                        emails = emails,
                        viewModel = viewModel,
                        onEmailClick = { selectedEmail = it }
                    )
                    "Orders" -> OrdersListView(
                        orders = orders,
                        viewModel = viewModel,
                        onOrderClick = { selectedOrder = it }
                    )
                    "Tickets" -> TicketsListView(
                        tickets = tickets,
                        viewModel = viewModel,
                        onTicketClick = { selectedTicket = it }
                    )
                }
            }
        }
    }

    // Modal & Sheet Dialogs Section
    selectedEmail?.let { email ->
        EmailDetailsDialog(
            email = email,
            onDismiss = { selectedEmail = null },
            onMarkRead = {
                viewModel.updateEmailRead(email, !email.isRead)
                selectedEmail = null
            },
            onDelete = {
                viewModel.deleteEmail(email)
                selectedEmail = null
            }
        )
    }

    selectedOrder?.let { order ->
        OrderDetailsDialog(
            order = order,
            onDismiss = { selectedOrder = null },
            onUpdateStatus = { status ->
                viewModel.updateOrderStatus(order, status)
                selectedOrder = null
            },
            onDelete = {
                viewModel.deleteOrder(order)
                selectedOrder = null
            }
        )
    }

    selectedTicket?.let { ticket ->
        TicketDetailsDialog(
            ticket = ticket,
            viewModel = viewModel,
            isProcessingAI = isProcessingAI,
            onDismiss = { selectedTicket = null },
            onResolve = { notes, timeline, status ->
                viewModel.resolveTicket(ticket, notes, timeline, status)
                selectedTicket = null
            },
            onDelete = {
                viewModel.deleteTicket(ticket)
                selectedTicket = null
            }
        )
    }

    showAddDialog?.let { type ->
        AddEntryDialog(
            type = type,
            onDismiss = { showAddDialog = null },
            onSaveEmail = { sub, snd, rec, bdy, cat, out ->
                viewModel.addEmail(sub, snd, rec, bdy, cat, out)
                showAddDialog = null
            },
            onSaveOrder = { num, name, eml, items, tot, status, carrier, track, est ->
                viewModel.addOrder(num, name, eml, items, tot, status, carrier, track, est)
                showAddDialog = null
            },
            onSaveTicket = { sub, eml, desc, prior, cat ->
                viewModel.addTicket(sub, eml, desc, prior, cat)
                showAddDialog = null
            }
        )
    }
}

// ==================== DASHBOARD VIEW ====================
@Composable
fun DashboardView(
    viewModel: TrackerViewModel,
    emailsCount: List<Email>,
    ordersCount: List<Order>,
    ticketsCount: List<Ticket>,
    inputTerminalText: String,
    onInputTextChange: (String) -> Unit,
    isProcessingAI: Boolean,
    onEmailClick: (Email) -> Unit,
    onOrderClick: (Order) -> Unit,
    onTicketClick: (Ticket) -> Unit
) {
    val scrollState = rememberScrollState()

    // Derived statistics
    val unreadEmails = emailsCount.count { !it.isRead }
    val pendingOrders = ordersCount.count { it.status != "Delivered" && it.status != "Cancelled" }
    val activeTickets = ticketsCount.count { it.status != "Resolved" }
    val closedComplaints = ticketsCount.count { it.status == "Resolved" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Alert
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, SurfaceBorder)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(PrimaryTeal.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Alert",
                        tint = PrimaryTeal
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Real-Time Tracking Service",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Unified system syncing all incoming customer channels and orders. Paste queries to see AI categorizations.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Stat Grid Rows Custom Styled
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Unread Inbox",
                value = unreadEmails.toString(),
                label = "${emailsCount.size} total synced",
                color = PrimaryTeal,
                icon = Icons.Default.Email,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Pending Orders",
                value = pendingOrders.toString(),
                label = "${ordersCount.count { it.status == "Delivered" }} delivered",
                color = SecondaryAmber,
                icon = Icons.Default.ShoppingCart,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Active Tickets",
                value = activeTickets.toString(),
                label = "Needs Attention",
                color = AccentRose,
                icon = Icons.Default.Warning,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Resolved Cases",
                value = closedComplaints.toString(),
                label = "Logs Verified Closed",
                color = TintGreen,
                icon = Icons.Default.Check,
                modifier = Modifier.weight(1f)
            )
        }

        // ==================== INTERNAL SYNCHRONIZATION TERMINAL ====================
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "AI Terminal",
                        tint = SecondaryAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI INTELLIGENT HARVESTER",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Input raw emails, complaints, or purchase notes. The platform will automatically extract parameters, classify types, and sync directly to database.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = inputTerminalText,
                    onValueChange = onInputTextChange,
                    placeholder = {
                        Text(
                            "Paste customer email statement or type inquiry...",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 12.sp, color = Color.White, fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Quick template tags
                Text(
                    text = "Try Sample Feeds (Click to Fill):",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val samples = listOf(
                        "Refund Case" to "Order ORD-71952 has not arrived yet, and Akhilesh says double billing of $199.99 is still showing on credit card! Please contact me at akhileshsingh65804@gmail.com",
                        "New Purchase" to "Dear Sarah Jenkins, thank you for order ORD-31920 (Premium Leather Wallet, qty 1, $49.99) shipped via Courier FedEx estimate delivery May 25, 2026. sarah@gmail.com",
                        "Tech Complaint" to "Help! Extreme crash loop on main settings drawer. Error: NullPointer on SQLite load. High priority, customer email: dev@company.com"
                    )
                    samples.forEach { (label, content) ->
                        Surface(
                            onClick = { onInputTextChange(content) },
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceBorder,
                            border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 10.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (inputTerminalText.trim().isNotEmpty()) {
                            viewModel.analyzeAndImportText(inputTerminalText)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryTeal,
                        contentColor = Color(0xFF020617)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isProcessingAI && inputTerminalText.isNotBlank()
                ) {
                    if (isProcessingAI) {
                        CircularProgressIndicator(
                            color = Color(0xFF020617),
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Sync Now",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "AI EXTRACT & SYNC TO DATABASE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // Recent Influx Streams / logs
        Text(
            text = "Active Support Queues",
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 1.sp
        )

        val recentTickets = ticketsCount.take(4)
        if (recentTickets.isEmpty()) {
            EmptyListPlaceholder("No unresolved tickets found.")
        } else {
            recentTickets.forEach { ticket ->
                TicketItemCard(
                    ticket = ticket,
                    onTicketClick = { onTicketClick(ticket) }
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    label: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(105.dp),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                fontSize = 24.sp,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = label,
                fontSize = 9.sp,
                color = TextSecondary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
    }
}

// ==================== SEARCH BAR COMPONENT ====================
@Composable
fun SearchBarComponent(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search records by ID, subject or email...", fontSize = 13.sp, color = TextSecondary) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                }
            }
        },
        singleLine = true,
        textStyle = TextStyle(fontSize = 13.sp, color = Color.White),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryTeal,
            unfocusedBorderColor = SurfaceBorder,
            focusedContainerColor = DarkSurface,
            unfocusedContainerColor = DarkSurface
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

// ==================== EMAILS LIST VIEW ====================
@Composable
fun EmailsListView(
    emails: List<Email>,
    viewModel: TrackerViewModel,
    onEmailClick: (Email) -> Unit
) {
    var selectedCat by remember { mutableStateOf("All") }
    val categories = listOf("All", "Support", "Orders", "Inquiry", "Billing")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCat == cat
                Surface(
                    onClick = {
                        selectedCat = cat
                        viewModel.filterEmails(cat)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) PrimaryTeal else DarkSurface,
                    border = BorderStroke(1.dp, if (isSelected) PrimaryTeal else SurfaceBorder)
                ) {
                    Text(
                        text = cat,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        fontSize = 11.sp,
                        color = if (isSelected) Color(0xFF030712) else TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (emails.isEmpty()) {
            EmptyListPlaceholder("No localized communications synced.")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(emails) { email ->
                    EmailItemCard(email = email, onClick = { onEmailClick(email) })
                }
            }
        }
    }
}

@Composable
fun EmailItemCard(email: Email, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (!email.isRead) PrimaryTeal.copy(alpha = 0.4f) else SurfaceBorder
        )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (!email.isRead) PrimaryTeal else Color.Transparent)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = email.senderCombined,
                        fontSize = 12.sp,
                        fontWeight = if (!email.isRead) FontWeight.Bold else FontWeight.Medium,
                        color = if (!email.isRead) Color.White else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = getCategoryColor(email.category).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, getCategoryColor(email.category).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = email.category,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = getCategoryColor(email.category),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = email.subject,
                    fontSize = 13.sp,
                    fontWeight = if (!email.isRead) FontWeight.Bold else FontWeight.Normal,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = email.body,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTime(email.timestamp),
                    fontSize = 9.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ==================== ORDERS LIST VIEW ====================
@Composable
fun OrdersListView(
    orders: List<Order>,
    viewModel: TrackerViewModel,
    onOrderClick: (Order) -> Unit
) {
    var selectedStatus by remember { mutableStateOf("All") }
    val statuses = listOf("All", "Placed", "Processing", "Shipped", "Delivered")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statuses.forEach { st ->
                val isSelected = selectedStatus == st
                Surface(
                    onClick = {
                        selectedStatus = st
                        viewModel.filterOrders(st)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) SecondaryAmber else DarkSurface,
                    border = BorderStroke(1.dp, if (isSelected) SecondaryAmber else SurfaceBorder)
                ) {
                    Text(
                        text = st,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        fontSize = 11.sp,
                        color = if (isSelected) Color(0xFF030712) else TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (orders.isEmpty()) {
            EmptyListPlaceholder("No e-commerce orders logged.")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(orders) { order ->
                    OrderItemCard(order = order, onClick = { onOrderClick(order) })
                }
            }
        }
    }
}

@Composable
fun OrderItemCard(order: Order, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.orderNumber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = SecondaryAmber,
                    fontFamily = FontFamily.Monospace
                )
                OrderStatusChip(status = order.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${order.customerName} (${order.customerEmail})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Freight: ${order.carrier} / Lbl: ${order.trackingNumber.ifEmpty { "Pending allocation" }}",
                fontSize = 11.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Amt: $${String.format(Locale.US, "%.2f", order.totalAmount)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Est: ${order.deliveryEstimate}",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==================== TICKETS LIST VIEW ====================
@Composable
fun TicketsListView(
    tickets: List<Ticket>,
    viewModel: TrackerViewModel,
    onTicketClick: (Ticket) -> Unit
) {
    var selectedStatus by remember { mutableStateOf("All") }
    val statuses = listOf("All", "New", "In Progress", "Resolved")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statuses.forEach { st ->
                val isSelected = selectedStatus == st
                Surface(
                    onClick = {
                        selectedStatus = st
                        viewModel.filterTickets(st)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) AccentRose else DarkSurface,
                    border = BorderStroke(1.dp, if (isSelected) AccentRose else SurfaceBorder)
                ) {
                    Text(
                        text = st,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        fontSize = 11.sp,
                        color = if (isSelected) Color(0xFF030712) else TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (tickets.isEmpty()) {
            EmptyListPlaceholder("No localized tickets registered.")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tickets) { ticket ->
                    TicketItemCard(ticket = ticket, onTicketClick = { onTicketClick(ticket) })
                }
            }
        }
    }
}

@Composable
fun TicketItemCard(ticket: Ticket, onTicketClick: () -> Unit) {
    Surface(
        onClick = onTicketClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ticket.ticketNumber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentRose,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = getPriorityColor(ticket.priority).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = ticket.priority,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = getPriorityColor(ticket.priority),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                TicketStatusChip(status = ticket.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = ticket.subject,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ticket.description,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Show resolution notes summary badge if resolved
            if (ticket.status == "Resolved" && !ticket.resolutionNotes.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TintGreen.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Resolved",
                        tint = TintGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Resolved in ${ticket.resolutionTimeline ?: "3h"}. Exact closure synced.",
                        fontSize = 9.sp,
                        color = TintGreen,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Client: ${ticket.customerEmail} • Logged: ${formatTime(ticket.timestamp)}",
                fontSize = 9.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ==================== DIALOGS & SHEET IMPLEMENTATIONS ====================

// --- 1. EMAIL DETAILS ---
@Composable
fun EmailDetailsDialog(
    email: Email,
    onDismiss: () -> Unit,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EMAIL INGESTION RECORD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Divider(color = SurfaceBorder)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecordField(lbl = "Sender Address", valText = email.senderCombined)
                    RecordField(lbl = "Receiver Platform", valText = email.receiver)
                    RecordField(lbl = "Timestamp Processed", valText = formatTime(email.timestamp))
                    RecordField(lbl = "Auto-Category Classified", valText = email.category, valColor = getCategoryColor(email.category))
                }

                Divider(color = SurfaceBorder)

                Text(
                    text = email.subject,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkBackground,
                    border = BorderStroke(1.dp, SurfaceBorder),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = email.body,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onMarkRead,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (email.isRead) "Mark Unread" else "Mark Read", fontSize = 12.sp, color = TextPrimary)
                    }
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRose.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = AccentRose, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", fontSize = 12.sp, color = AccentRose)
                    }
                }
            }
        }
    }
}

// --- 2. ORDER DETAILS ---
@Composable
fun OrderDetailsDialog(
    order: Order,
    onDismiss: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ORDER TRANSACTION FLOW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryAmber,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Divider(color = SurfaceBorder)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecordField(lbl = "Order Reference ID", valText = order.orderNumber)
                    RecordField(lbl = "Customer Name", valText = order.customerName)
                    RecordField(lbl = "Customer Email", valText = order.customerEmail)
                    RecordField(lbl = "Shipping Carrier", valText = order.carrier)
                    RecordField(lbl = "Tracking Reference", valText = order.trackingNumber.ifEmpty { "Unassigned" })
                    RecordField(lbl = "Estimated Delivery", valText = order.deliveryEstimate)
                }

                Divider(color = SurfaceBorder)

                Text(
                    text = "Transaction Package Breakdown & Items:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )

                // Render lists items manually parsed
                val items = parseOrderItemsList(order.itemsJson)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkBackground,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (items.isEmpty()) {
                            Text("Standard Freight Pack x1", fontSize = 12.sp, color = Color.White)
                        } else {
                            items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${item.name} (x${item.qty})", fontSize = 11.sp, color = Color.White)
                                    Text("$${String.format(Locale.US, "%.2f", item.price)}", fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = SurfaceBorder)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grand Invoiced Total", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("$${String.format(Locale.US, "%.2f", order.totalAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SecondaryAmber, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Workflow status stepper advances
                Text(
                    text = "Advance Dispatch Logistics Bar:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val steps = listOf("Processing", "Shipped", "Delivered")
                    steps.forEach { step ->
                        val isCurrent = order.status == step
                        Surface(
                            onClick = { onUpdateStatus(step) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            color = if (isCurrent) SecondaryAmber else SurfaceBorder,
                            border = BorderStroke(1.dp, if (isCurrent) SecondaryAmber else Color.Transparent)
                        ) {
                            Text(
                                text = step,
                                modifier = Modifier.padding(vertical = 8.dp),
                                fontSize = 10.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) Color(0xFF030712) else TextSecondary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRose.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = AccentRose, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Order Log", fontSize = 12.sp, color = AccentRose)
                    }
                }
            }
        }
    }
}

// --- 3. TICKET DETAILS & RESOLUTION SYSTEM ---
@Composable
fun TicketDetailsDialog(
    ticket: Ticket,
    viewModel: TrackerViewModel,
    isProcessingAI: Boolean,
    onDismiss: () -> Unit,
    onResolve: (notes: String, timeline: String, status: String) -> Unit,
    onDelete: () -> Unit
) {
    var resolutionNotesInput by remember { mutableStateOf(ticket.resolutionNotes ?: "") }
    var resolutionTimelineInput by remember { mutableStateOf(ticket.resolutionTimeline ?: "2 hours") }
    var resolutionStatusChoice by remember { mutableStateOf(ticket.resolutionStatus ?: "Closed") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TICKET CASE RECORD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentRose,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Divider(color = SurfaceBorder)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecordField(lbl = "Case Index Number", valText = ticket.ticketNumber)
                    RecordField(lbl = "Client Account Address", valText = ticket.customerEmail)
                    RecordField(lbl = "Assigned Priority", valText = ticket.priority, valColor = getPriorityColor(ticket.priority))
                    RecordField(lbl = "Assigned Category", valText = ticket.category)
                    RecordField(lbl = "Date Documented", valText = formatTime(ticket.timestamp))
                }

                Divider(color = SurfaceBorder)

                Text(
                    text = ticket.subject,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkBackground,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Text(
                        text = ticket.description,
                        fontSize = 11.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Divider(color = SurfaceBorder)

                // RESOLUTION ACTION BAR SECTION
                Text(
                    text = "RESOLUTION OUTCOME MONITORING & VERIFICATION:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryAmber,
                    letterSpacing = 0.5.sp
                )

                if (ticket.status != "Resolved") {
                    // Show Suggest Option with loading spinner
                    Button(
                        onClick = {
                            viewModel.suggestTicketResolution(ticket) { notes, timeline ->
                                resolutionNotesInput = notes
                                resolutionTimelineInput = timeline
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TintPurple),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isProcessingAI
                    ) {
                        if (isProcessingAI) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Star, contentDescription = "AI Suggest", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SUGGEST AI RESOLUTION DRAFT", fontSize = 11.sp)
                        }
                    }

                    OutlinedTextField(
                        value = resolutionNotesInput,
                        onValueChange = { resolutionNotesInput = it },
                        label = { Text("Exact Solution Notes Provided", fontSize = 11.sp, color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 11.sp, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        ),
                        minLines = 3
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = resolutionTimelineInput,
                            onValueChange = { resolutionTimelineInput = it },
                            label = { Text("Time Invested", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(fontSize = 11.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryTeal, unfocusedBorderColor = SurfaceBorder)
                        )

                        // Choice Status
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Verification Status", fontSize = 9.sp, color = TextSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val states = listOf("Closed", "Verified")
                                states.forEach { s ->
                                    val isSel = resolutionStatusChoice == s
                                    Surface(
                                        onClick = { resolutionStatusChoice = s },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSel) TintGreen else SurfaceBorder,
                                        border = BorderStroke(1.dp, if (isSel) TintGreen else Color.Transparent)
                                    ) {
                                        Text(
                                            text = s,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            fontSize = 9.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color(0xFF030712) else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (resolutionNotesInput.isNotBlank()) {
                                onResolve(resolutionNotesInput, resolutionTimelineInput, resolutionStatusChoice)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TintGreen),
                        shape = RoundedCornerShape(8.dp),
                        enabled = resolutionNotesInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Resolve", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RESOLVE COMPLAINT & LOG VERIFICATION", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Show read only resolution summary
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = TintGreen.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, TintGreen.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("RESOLUTION LOG AUDIT COMPLETE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TintGreen, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(2.dp))
                            RecordField(lbl = "Status", valText = ticket.resolutionStatus ?: "Closed", valColor = TintGreen)
                            RecordField(lbl = "Investigation Solution", valText = ticket.resolutionNotes ?: "")
                            RecordField(lbl = "Duration Invested", valText = ticket.resolutionTimeline ?: "N/A")
                        }
                    }
                }

                Divider(color = SurfaceBorder)

                Button(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRose.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRose, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Ticket Log", fontSize = 12.sp, color = AccentRose)
                }
            }
        }
    }
}

// --- 4. MANUAL ADD DIALOG ---
@Composable
fun AddEntryDialog(
    type: String, // "Email", "Order", "Ticket"
    onDismiss: () -> Unit,
    onSaveEmail: (sub: String, snd: String, rec: String, bdy: String, cat: String, out: Boolean) -> Unit,
    onSaveOrder: (num: String, name: String, eml: String, items: String, tot: Double, status: String, carrier: String, track: String, est: String) -> Unit,
    onSaveTicket: (sub: String, eml: String, desc: String, prior: String, cat: String) -> Unit
) {
    // Ticket state
    var tSubject by remember { mutableStateOf("") }
    var tEmail by remember { mutableStateOf("akhileshsingh65804@gmail.com") }
    var tDescription by remember { mutableStateOf("") }
    var tPriority by remember { mutableStateOf("High") }
    var tCategory by remember { mutableStateOf("Technical Support") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LOG NEW SUPPORT COMPLAINT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Divider(color = SurfaceBorder)

                OutlinedTextField(
                    value = tSubject,
                    onValueChange = { tSubject = it },
                    label = { Text("Subject / Interaction Title", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 12.sp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryTeal, unfocusedBorderColor = SurfaceBorder)
                )

                OutlinedTextField(
                    value = tEmail,
                    onValueChange = { tEmail = it },
                    label = { Text("Customer Email Address", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 12.sp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryTeal, unfocusedBorderColor = SurfaceBorder)
                )

                OutlinedTextField(
                    value = tDescription,
                    onValueChange = { tDescription = it },
                    label = { Text("Detailed Description", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 12.sp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryTeal, unfocusedBorderColor = SurfaceBorder),
                    minLines = 3
                )

                // Priority Selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Severity Priority:", fontSize = 11.sp, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val priorities = listOf("Low", "Medium", "High")
                        priorities.forEach { p ->
                            val isSel = tPriority == p
                            val pColor = getPriorityColor(p)
                            Surface(
                                onClick = { tPriority = p },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) pColor else SurfaceBorder,
                                border = BorderStroke(1.dp, if (isSel) pColor else Color.Transparent)
                            ) {
                                Text(
                                    text = p,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    fontSize = 11.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color(0xFF030712) else TextPrimary
                                )
                            }
                        }
                    }
                }

                // Category selection dropdown tags
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Category Tag:", fontSize = 11.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val cats = listOf("Technical Support", "Billing", "Delivery Issue", "Returns", "General")
                        cats.forEach { c ->
                            val isSel = tCategory == c
                            Surface(
                                onClick = { tCategory = c },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSel) PrimaryTeal else SurfaceBorder,
                                border = BorderStroke(1.dp, if (isSel) PrimaryTeal else Color.Transparent)
                            ) {
                                Text(
                                    text = c,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 10.sp,
                                    color = if (isSel) Color(0xFF030712) else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (tSubject.isNotBlank() && tEmail.isNotBlank()) {
                            onSaveTicket(tSubject, tEmail, tDescription, tPriority, tCategory)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = Color(0xFF020617)),
                    shape = RoundedCornerShape(8.dp),
                    enabled = tSubject.isNotBlank() && tEmail.isNotBlank()
                ) {
                    Text("SAVE TO SECURE QUEUE", fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}


// ==================== STAT CHIP DRAWABLES & FORMATTERS ====================

@Composable
fun RecordField(lbl: String, valText: String, valColor: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$lbl:",
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.width(135.dp),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = valText,
            fontSize = 11.sp,
            color = valColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun OrderStatusChip(status: String) {
    val (backColor, txColor) = when (status) {
        "Placed" -> TintCyan.copy(alpha = 0.15f) to TintCyan
        "Processing" -> SecondaryAmber.copy(alpha = 0.15f) to SecondaryAmber
        "Shipped" -> TintPurple.copy(alpha = 0.15f) to TintPurple
        "Delivered" -> TintGreen.copy(alpha = 0.15f) to TintGreen
        else -> TextSecondary.copy(alpha = 0.15f) to TextSecondary
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backColor,
        border = BorderStroke(1.dp, txColor.copy(alpha = 0.3f))
    ) {
        Text(
            text = status.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = txColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun TicketStatusChip(status: String) {
    val (backColor, txColor) = when (status) {
        "New" -> AccentRose.copy(alpha = 0.15f) to AccentRose
        "In Progress" -> SecondaryAmber.copy(alpha = 0.15f) to SecondaryAmber
        "Pending Info" -> TintPurple.copy(alpha = 0.15f) to TintPurple
        "Resolved" -> TintGreen.copy(alpha = 0.15f) to TintGreen
        else -> TextSecondary.copy(alpha = 0.15f) to TextSecondary
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backColor,
        border = BorderStroke(1.dp, txColor.copy(alpha = 0.3f))
    ) {
        Text(
            text = status.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = txColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun EmptyListPlaceholder(msg: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = SurfaceBorder,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = msg,
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

fun getPriorityColor(priority: String): Color {
    return when (priority) {
        "High" -> AccentRose
        "Medium" -> SecondaryAmber
        else -> TextSecondary
    }
}

fun getCategoryColor(category: String): Color {
    return when (category) {
        "Support" -> AccentRose
        "Orders" -> SecondaryAmber
        "Inquiry" -> TintCyan
        "Billing" -> TintPurple
        else -> TintGreen
    }
}

fun formatTime(ms: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    return sdf.format(Date(ms))
}

data class ClientItem(val name: String, val qty: Int, val price: Double)

fun parseOrderItemsList(jsonString: String): List<ClientItem> {
    try {
        val list = mutableListOf<ClientItem>()
        val arr = JSONArray(jsonString)
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            list.add(
                ClientItem(
                    name = item.optString("name", "Product Care"),
                    qty = item.optInt("qty", 1),
                    price = item.optDouble("price", 29.99)
                )
            )
        }
        return list
    } catch (e: Exception) {
        return emptyList()
    }
}
