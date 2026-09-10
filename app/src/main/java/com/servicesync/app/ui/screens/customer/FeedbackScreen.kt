package com.servicesync.app.ui.screens.customer

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servicesync.app.data.model.AppFeedback
import com.servicesync.app.data.repository.ServiceSyncRepository
import com.servicesync.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class FeedbackIssueOption(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val placeholderPrompt: String
)

val BASIC_ISSUE_LIST = listOf(
    FeedbackIssueOption(
        id = "bug",
        title = "App Bugs & Glitches",
        description = "App crashing, screen freezing, buttons not responding, or OTP errors.",
        icon = Icons.Default.BugReport,
        placeholderPrompt = "Describe the bug you encountered. Which screen were you on, and what happened?"
    ),
    FeedbackIssueOption(
        id = "booking",
        title = "Booking & Arrival Timing",
        description = "Slot availability, specialist arriving late, or rescheduling difficulties.",
        icon = Icons.Default.Schedule,
        placeholderPrompt = "What issue did you experience with your booking or the specialist's arrival?"
    ),
    FeedbackIssueOption(
        id = "quality",
        title = "Specialist & Work Quality",
        description = "Service workmanship, specialist behavior, professionalism, or tools.",
        icon = Icons.Default.Engineering,
        placeholderPrompt = "How was the service quality? What could our specialist have done better?"
    ),
    FeedbackIssueOption(
        id = "pricing",
        title = "Billing, Pricing & Wallet",
        description = "Unexpected charges, pricing transparency, refund status, or UPI issues.",
        icon = Icons.Default.Payments,
        placeholderPrompt = "Share your pricing or payment concerns so our billing team can assist you..."
    ),
    FeedbackIssueOption(
        id = "ui_theme",
        title = "UI, Dark Mode & Design",
        description = "Text readability, contrast in dark mode, iconography, or navigation.",
        icon = Icons.Default.Palette,
        placeholderPrompt = "What design, color contrast, or navigation improvements would you suggest?"
    ),
    FeedbackIssueOption(
        id = "new_service",
        title = "Request New Service / Appliance",
        description = "Need a service, appliance repair, or spare part not currently in SaServe.",
        icon = Icons.Default.HomeRepairService,
        placeholderPrompt = "Which service or appliance repair would you like SaServe to introduce next?"
    ),
    FeedbackIssueOption(
        id = "improvement",
        title = "General Improvements & Ideas",
        description = "New feature suggestions, experience enhancements, or compliments.",
        icon = Icons.Default.Lightbulb,
        placeholderPrompt = "What ideas or suggestions do you have to make SaServe even better for you?"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    repository: ServiceSyncRepository,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by repository.currentUser.collectAsState()
    val pastFeedbacks by repository.feedbackList.collectAsState()

    var selectedIssueOption by remember { mutableStateOf<FeedbackIssueOption?>(BASIC_ISSUE_LIST.first()) }
    var suggestionsText by remember { mutableStateOf("") }
    var ratingStars by remember { mutableStateOf(5) }
    var userNameInput by remember { mutableStateOf(currentUser?.name ?: "") }
    var userPhoneInput by remember { mutableStateOf(currentUser?.phone ?: "") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submittedFeedbackId by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) }

    // Synchronize user info when loaded
    LaunchedEffect(currentUser) {
        if (userNameInput.isBlank() && !currentUser?.name.isNullOrBlank()) {
            userNameInput = currentUser?.name.orEmpty()
        }
        if (userPhoneInput.isBlank() && !currentUser?.phone.isNullOrBlank()) {
            userPhoneInput = currentUser?.phone.orEmpty()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Feedback & Suggestions",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Intro Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Feedback,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Help Us Improve SaServe",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Your feedback directly shapes our features & service quality",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Text(
                            text = "Choose an issue or topic from the list below, then type your suggestions or report the problem in detail.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Step 1: Basic Issue List Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. Select Issue / Topic Category",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap to choose",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BASIC_ISSUE_LIST.forEach { option ->
                            val isSelected = selectedIssueOption?.id == option.id
                            Surface(
                                onClick = {
                                    selectedIssueOption = option
                                    showError = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PrimaryBlue.copy(alpha = 0.12f) else SurfaceLight,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) PrimaryBlue else CardBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) PrimaryBlue else PrimaryBlue.copy(alpha = 0.10f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = option.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else PrimaryBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) PrimaryBlue else TextPrimary
                                        )
                                        Text(
                                            text = option.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            maxLines = 2
                                        )
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedIssueOption = option
                                            showError = false
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = PrimaryBlue,
                                            unselectedColor = TextMuted
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Step 2: Rating Satisfaction
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "2. Overall App Experience Rating",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                (1..5).forEach { star ->
                                    IconButton(
                                        onClick = { ratingStars = star },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (star <= ratingStars) Icons.Default.Star else Icons.Default.StarOutline,
                                            contentDescription = "$star stars",
                                            tint = if (star <= ratingStars) StarGold else TextMuted,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }

                            val ratingLabel = when (ratingStars) {
                                1 -> "😞 Poor"
                                2 -> "😐 Fair"
                                3 -> "🙂 Good"
                                4 -> "😊 Very Good"
                                else -> "🤩 Excellent"
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PrimaryBlue.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = ratingLabel,
                                    color = PrimaryBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Step 3: Detailed Suggestion & Description Input Box
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "3. Type Your Suggestions / Issue Details",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (selectedIssueOption != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PrimaryBlue.copy(alpha = 0.10f)
                                ) {
                                    Text(
                                        text = selectedIssueOption!!.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrimaryBlue,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // Informative context banner
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceVariantLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = selectedIssueOption?.placeholderPrompt
                                        ?: "Type your suggestions or describe the issue in detail...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        OutlinedTextField(
                            value = suggestionsText,
                            onValueChange = {
                                if (it.length <= 1000) {
                                    suggestionsText = it
                                    if (it.isNotBlank()) showError = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 140.dp),
                            placeholder = {
                                Text(
                                    text = "Type your suggestions, improvements, or issue details here...",
                                    color = TextMuted,
                                    fontSize = 14.sp
                                )
                            },
                            isError = showError && suggestionsText.isBlank(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (showError && suggestionsText.isBlank()) {
                                Text(
                                    text = "Please enter your suggestions or issue description.",
                                    color = StatusCancelled,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = "Be as specific as possible to help us resolve it quickly.",
                                    color = TextMuted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Text(
                                text = "${suggestionsText.length}/1000",
                                color = TextMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Step 4: Contact Information Preview
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "4. Your Contact Details (Optional)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "We will use this to update you when your feedback is reviewed or resolved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = userNameInput,
                                onValueChange = { userNameInput = it },
                                label = { Text("Name") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            OutlinedTextField(
                                value = userPhoneInput,
                                onValueChange = { userPhoneInput = it },
                                label = { Text("Phone") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Submit Button
            item {
                Button(
                    onClick = {
                        if (suggestionsText.isBlank()) {
                            showError = true
                            Toast.makeText(context, "Please type your suggestion before submitting", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmitting = true
                        val categoryName = selectedIssueOption?.title ?: "General Feedback"
                        val feedback = repository.submitFeedback(
                            issueCategory = categoryName,
                            rating = ratingStars,
                            suggestions = suggestionsText,
                            contactName = userNameInput,
                            contactPhone = userPhoneInput
                        )
                        isSubmitting = false
                        submittedFeedbackId = feedback.id
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Submit Feedback & Suggestions",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Previous Submissions History (if any)
            if (pastFeedbacks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Your Recent Submissions (${pastFeedbacks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                items(pastFeedbacks) { fb ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PrimaryBlue.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = fb.issueCategory,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrimaryBlue,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = StarGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${fb.rating}/5",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = fb.suggestions,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )

                            val dateStr = try {
                                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(fb.timestamp))
                            } catch (e: Exception) {
                                "Recently"
                            }
                            Text(
                                text = "Submitted on $dateStr • ID: ${fb.id}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Success Confirmation Dialog
    if (submittedFeedbackId != null) {
        AlertDialog(
            onDismissRequest = {
                submittedFeedbackId = null
                onBackClick()
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(StatusCompleted.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusCompleted,
                        modifier = Modifier.size(36.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Thank You for Your Feedback! 🎉",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "We have received your suggestions for '${selectedIssueOption?.title}'. Our product and operations teams actively review every submission to make SaServe superior.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "Feedback Reference ID: ${submittedFeedbackId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        submittedFeedbackId = null
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Return to Home", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        submittedFeedbackId = null
                        suggestionsText = ""
                    }
                ) {
                    Text("Submit Another", color = TextSecondary)
                }
            },
            containerColor = SurfaceLight,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
