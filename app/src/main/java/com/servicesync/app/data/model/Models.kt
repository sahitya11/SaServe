package com.servicesync.app.data.model

enum class UserRole {
    CUSTOMER,
    PROVIDER
}

enum class ServiceCategory(
    val displayName: String,
    val description: String,
    val iconName: String
) {
    ELECTRICIAN(
        displayName = "Electrician",
        description = "Wiring, lighting, switchboards, circuit breakers & EV chargers",
        iconName = "Bolt"
    ),
    PLUMBER(
        displayName = "Plumber",
        description = "Pipe repairs, leakages, faucets, heaters & drainage unclogging",
        iconName = "WaterDrop"
    ),
    CARPENTER(
        displayName = "Carpenter",
        description = "Furniture repairs, custom cabinetry, doors & wood installation",
        iconName = "Handyman"
    ),
    MECHANIC(
        displayName = "Mechanic",
        description = "Car & bike repair, oil changes, engine diagnosis & brake services",
        iconName = "Build"
    ),
    APPLIANCE_REPAIR(
        displayName = "Appliance Repair",
        description = "AC, refrigerators, washing machines, microwaves & TV repair",
        iconName = "HomeRepairService"
    ),
    PAINTER(
        displayName = "Painter",
        description = "Interior/exterior painting, waterproofing & wall touch-ups",
        iconName = "FormatPaint"
    )
}

data class SavedAddress(
    val id: String = "",
    val label: String = "Home", // "Home", "Office", "Other"
    val addressLine: String = "",
    val isDefault: Boolean = false
)

data class User(
    val id: String,
    val name: String,
    val email: String = "",
    val phone: String,
    val password: String = "",
    val role: UserRole = UserRole.CUSTOMER,
    val address: String = "",
    val savedAddresses: List<SavedAddress> = emptyList(),
    val providerId: String? = null // Linked if role == PROVIDER
)


data class TimeSlot(
    val id: String,
    val timeRange: String, // e.g., "09:00 AM - 11:00 AM"
    val isAvailable: Boolean = true
)

data class Review(
    val id: String,
    val author: String,
    val rating: Float,
    val comment: String,
    val date: String
)

data class ServiceProvider(
    val id: String,
    val userId: String,
    val name: String,
    val phone: String,
    val email: String,
    val category: ServiceCategory,
    val rating: Float, // e.g., 4.9f
    val reviewCount: Int,
    val experienceYears: Int,
    val hourlyRate: Double,
    val bio: String,
    val location: String,
    val distanceMiles: Double,
    val isAvailable: Boolean = true,
    val isVerified: Boolean = true,
    val skills: List<String> = emptyList(),
    val availableSlots: List<TimeSlot> = emptyList(),
    val reviews: List<Review> = emptyList()
)

enum class BookingStatus(val label: String) {
    PENDING("Pending Approval"),
    ACCEPTED("Accepted & Scheduled"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

data class Booking(
    val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val providerPhone: String = "",
    val category: ServiceCategory = ServiceCategory.ELECTRICIAN,
    val scheduledDate: String = "", // e.g., "Tomorrow, Sep 6"
    val scheduledSlot: String = "", // e.g., "11:00 AM - 01:00 PM"
    val issueDescription: String = "",
    val status: BookingStatus = BookingStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val hourlyRate: Double = 0.0,
    val estimatedHours: Int = 1,
    val startOtp: String = ((1000..9999).random()).toString(),
    val completionOtp: String = ((1000..9999).random()).toString(),
    val customerRating: Float? = null,
    val customerReview: String? = null
) {
    val totalAmount: Double get() = hourlyRate * estimatedHours
}

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val bookingId: String? = null,
    val isRead: Boolean = false
)

data class WalletTransaction(
    val id: String = "",
    val amount: Double = 0.0,
    val type: String = "DEPOSIT", // "DEPOSIT" or "PAYMENT"
    val description: String = "",
    val timestamp: String = "",
    val upiRefId: String = ""
)

