package com.servicesync.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.servicesync.app.data.model.*
import com.servicesync.app.notification.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ServiceSyncRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("servicesync_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _providers = MutableStateFlow<List<ServiceProvider>>(emptyList())
    val providers: StateFlow<List<ServiceProvider>> = _providers.asStateFlow()

    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings: StateFlow<List<Booking>> = _bookings.asStateFlow()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    init {
        loadOrInitializeData()
    }

    private fun loadOrInitializeData() {
        // Clear out old pre-seeded provider caches
        prefs.edit().remove("key_providers_list").remove("key_providers_list_inr").apply()

        // 1. Providers: Only user-added providers are loaded
        val savedProvidersJson = prefs.getString(KEY_CUSTOM_PROVIDERS, null)
        if (!savedProvidersJson.isNullOrEmpty()) {
            val type = object : TypeToken<List<ServiceProvider>>() {}.type
            _providers.value = gson.fromJson(savedProvidersJson, type)
        } else {
            // Start completely empty as requested: only providers the user adds are visible
            _providers.value = emptyList()
            saveProviders(emptyList())
        }

        // 2. Bookings
        val savedBookingsJson = prefs.getString(KEY_CUSTOM_BOOKINGS, null)
        if (!savedBookingsJson.isNullOrEmpty()) {
            val type = object : TypeToken<List<Booking>>() {}.type
            _bookings.value = gson.fromJson(savedBookingsJson, type)
        } else {
            _bookings.value = emptyList()
            saveBookings(emptyList())
        }

        // 3. Notifications
        val savedNotifsJson = prefs.getString(KEY_NOTIFICATIONS, null)
        if (!savedNotifsJson.isNullOrEmpty()) {
            val type = object : TypeToken<List<AppNotification>>() {}.type
            _notifications.value = gson.fromJson(savedNotifsJson, type)
        } else {
            val initialNotifs = listOf(
                AppNotification(
                    id = UUID.randomUUID().toString(),
                    title = "Welcome to SaServe! 👋",
                    message = "Tap '+ Add Specialist' to add electricians, plumbers, carpenters, or mechanics to your service catalog.",
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
            )
            _notifications.value = initialNotifs
            saveNotifications(initialNotifs)
        }

        // 4. Current User (Customer Only)
        val savedUserJson = prefs.getString(KEY_CURRENT_USER, null)
        if (!savedUserJson.isNullOrEmpty()) {
            _currentUser.value = gson.fromJson(savedUserJson, User::class.java)
        } else {
            val defaultCustomer = User(
                id = "cust_user",
                name = "Customer User",
                email = "customer@saserve.com",
                phone = "+91 98765 43210",
                role = UserRole.CUSTOMER,
                address = "Flat 402, Green Valley Apartments"
            )
            _currentUser.value = defaultCustomer
            saveUser(defaultCustomer)
        }
    }

    private fun saveProviders(list: List<ServiceProvider>) {
        prefs.edit().putString(KEY_CUSTOM_PROVIDERS, gson.toJson(list)).apply()
    }

    private fun saveBookings(list: List<Booking>) {
        prefs.edit().putString(KEY_CUSTOM_BOOKINGS, gson.toJson(list)).apply()
    }

    private fun saveNotifications(list: List<AppNotification>) {
        prefs.edit().putString(KEY_NOTIFICATIONS, gson.toJson(list)).apply()
    }

    private fun saveUser(user: User?) {
        if (user == null) {
            prefs.edit().remove(KEY_CURRENT_USER).apply()
        } else {
            prefs.edit().putString(KEY_CURRENT_USER, gson.toJson(user)).apply()
        }
    }

    fun updateCustomerProfile(name: String, phone: String, email: String, address: String) {
        val updatedUser = User(
            id = _currentUser.value?.id ?: "cust_user",
            name = name,
            email = email,
            phone = phone,
            role = UserRole.CUSTOMER,
            address = address
        )
        _currentUser.value = updatedUser
        saveUser(updatedUser)
    }

    /**
     * Adds a new service provider.
     * ONLY service providers added through this will be visible in the application.
     */
    fun addServiceProvider(
        name: String,
        phone: String,
        email: String,
        category: ServiceCategory,
        hourlyRate: Double,
        experienceYears: Int,
        bio: String,
        location: String,
        rating: Float = 5.0f,
        skills: List<String> = listOf("Installation", "Maintenance", "Emergency Support")
    ): ServiceProvider {
        val standardSlots = listOf(
            TimeSlot("slot_1", "09:00 AM - 11:00 AM", true),
            TimeSlot("slot_2", "11:00 AM - 01:00 PM", true),
            TimeSlot("slot_3", "02:00 PM - 04:00 PM", true),
            TimeSlot("slot_4", "04:00 PM - 06:00 PM", true)
        )

        val newProvider = ServiceProvider(
            id = "prov_" + UUID.randomUUID().toString().take(8),
            userId = "user_" + UUID.randomUUID().toString().take(8),
            name = name,
            phone = phone,
            email = email,
            category = category,
            rating = rating,
            reviewCount = 1,
            experienceYears = experienceYears,
            hourlyRate = hourlyRate,
            bio = bio.ifBlank { "Certified ${category.displayName} specialist ready for service bookings." },
            location = location.ifBlank { "Local Area" },
            distanceMiles = 1.2,
            isAvailable = true,
            isVerified = true,
            skills = skills,
            availableSlots = standardSlots,
            reviews = listOf(
                Review(
                    id = "rev_new",
                    author = "System",
                    rating = rating,
                    comment = "Verified ${category.displayName} on SaServe.",
                    date = "Today"
                )
            )
        )

        val updated = listOf(newProvider) + _providers.value
        _providers.value = updated
        saveProviders(updated)

        // Add a notification that a new specialist was added
        val notif = AppNotification(
            id = UUID.randomUUID().toString(),
            title = "New Specialist Added ✨",
            message = "${newProvider.name} (${newProvider.category.displayName}) is now available for bookings at ₹${newProvider.hourlyRate.toInt()}/hr."
        )
        val updatedNotifs = listOf(notif) + _notifications.value
        _notifications.value = updatedNotifs
        saveNotifications(updatedNotifs)

        return newProvider
    }

    fun deleteServiceProvider(providerId: String) {
        val updated = _providers.value.filter { it.id != providerId }
        _providers.value = updated
        saveProviders(updated)
    }

    fun createBooking(
        provider: ServiceProvider,
        date: String,
        timeSlot: String,
        address: String,
        issueDescription: String
    ): Booking {
        val user = _currentUser.value
        val startOtp = ((1000..9999).random()).toString()
        val completionOtp = ((1000..9999).random()).toString()

        val booking = Booking(
            id = "bk_" + UUID.randomUUID().toString().take(8),
            customerId = user?.id ?: "cust_user",
            customerName = user?.name ?: "Customer User",
            customerPhone = user?.phone ?: "+91 98765 43210",
            customerAddress = if (address.isNotBlank()) address else (user?.address ?: "Customer Address"),
            providerId = provider.id,
            providerName = provider.name,
            providerPhone = provider.phone,
            category = provider.category,
            scheduledDate = date,
            scheduledSlot = timeSlot,
            issueDescription = if (issueDescription.isNotBlank()) issueDescription else "General service and maintenance",
            status = BookingStatus.PENDING,
            hourlyRate = provider.hourlyRate,
            startOtp = startOtp,
            completionOtp = completionOtp
        )

        val updatedBookings = listOf(booking) + _bookings.value
        _bookings.value = updatedBookings
        saveBookings(updatedBookings)

        // Customer confirmation notification
        val newNotif = AppNotification(
            id = UUID.randomUUID().toString(),
            title = "Booking Request Sent 🚀",
            message = "Your request for ${provider.category.displayName} (${provider.name}) on $date at $timeSlot has been submitted.",
            bookingId = booking.id
        )
        val updatedNotifs = listOf(newNotif) + _notifications.value
        _notifications.value = updatedNotifs
        saveNotifications(updatedNotifs)

        return booking
    }

    /**
     * Simulates or executes provider acceptance for a booking.
     * Triggers the Android system push notification and updates customer's booking status to ACCEPTED.
     */
    fun acceptBooking(bookingId: String): Boolean {
        val currentList = _bookings.value
        val targetIndex = currentList.indexOfFirst { it.id == bookingId }
        if (targetIndex == -1) return false

        val currentBooking = currentList[targetIndex]
        val updatedBooking = currentBooking.copy(status = BookingStatus.ACCEPTED)
        val updatedList = currentList.toMutableList().apply {
            set(targetIndex, updatedBooking)
        }
        _bookings.value = updatedList
        saveBookings(updatedList)

        // 1. Send Android Push / System Notification to customer
        NotificationHelper.sendBookingAcceptedNotification(context, updatedBooking)

        // 2. Add In-App Notification entry
        val acceptanceNotif = AppNotification(
            id = UUID.randomUUID().toString(),
            title = "🎉 Booking Accepted by ${updatedBooking.providerName}!",
            message = "${updatedBooking.providerName} (${updatedBooking.category.displayName}) has confirmed your appointment for ${updatedBooking.scheduledDate} at ${updatedBooking.scheduledSlot}.",
            bookingId = updatedBooking.id
        )
        val updatedNotifs = listOf(acceptanceNotif) + _notifications.value
        _notifications.value = updatedNotifs
        saveNotifications(updatedNotifs)

        return true
    }

    fun cancelBooking(bookingId: String): Boolean {
        val currentList = _bookings.value
        val targetIndex = currentList.indexOfFirst { it.id == bookingId }
        if (targetIndex == -1) return false

        val currentBooking = currentList[targetIndex]
        val updatedBooking = currentBooking.copy(status = BookingStatus.CANCELLED)
        val updatedList = currentList.toMutableList().apply {
            set(targetIndex, updatedBooking)
        }
        _bookings.value = updatedList
        saveBookings(updatedList)
        return true
    }

    fun markNotificationRead(id: String) {
        val updated = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        _notifications.value = updated
        saveNotifications(updated)
    }

    fun clearAllNotifications() {
        _notifications.value = emptyList()
        saveNotifications(emptyList())
    }

    companion object {
        private const val KEY_CUSTOM_PROVIDERS = "key_custom_providers_only_v2"
        private const val KEY_CUSTOM_BOOKINGS = "key_custom_bookings_v2"
        private const val KEY_NOTIFICATIONS = "key_notifications_list"
        private const val KEY_CURRENT_USER = "key_current_user"

        @Volatile
        private var INSTANCE: ServiceSyncRepository? = null

        fun getInstance(context: Context): ServiceSyncRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ServiceSyncRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
