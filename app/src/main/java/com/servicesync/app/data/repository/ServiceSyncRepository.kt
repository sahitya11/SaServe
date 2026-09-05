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

    private val _isLoggedIn = MutableStateFlow<Boolean>(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _providers = MutableStateFlow<List<ServiceProvider>>(emptyList())
    val providers: StateFlow<List<ServiceProvider>> = _providers.asStateFlow()

    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings: StateFlow<List<Booking>> = _bookings.asStateFlow()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _dismissedHomeBookingIds = MutableStateFlow<Set<String>>(emptySet())
    val dismissedHomeBookingIds: StateFlow<Set<String>> = _dismissedHomeBookingIds.asStateFlow()

    private val _walletBalance = MutableStateFlow<Double>(0.0)
    val walletBalance: StateFlow<Double> = _walletBalance.asStateFlow()

    private val _walletTransactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
    val walletTransactions: StateFlow<List<WalletTransaction>> = _walletTransactions.asStateFlow()

    private val _savedAddresses = MutableStateFlow<List<SavedAddress>>(emptyList())
    val savedAddresses: StateFlow<List<SavedAddress>> = _savedAddresses.asStateFlow()

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
            try {
                val rawBookings: List<Booking>? = gson.fromJson(savedBookingsJson, type)
                val sanitized = rawBookings?.map { b ->
                    b.copy(
                        id = if (b.id.isNullOrBlank()) ("bk_" + UUID.randomUUID().toString().take(8)) else b.id,
                        customerId = b.customerId ?: "cust_user",
                        customerName = b.customerName ?: "Customer",
                        customerPhone = b.customerPhone ?: "+91 98765 43210",
                        customerAddress = b.customerAddress ?: "Customer Address",
                        providerId = b.providerId ?: "prov_default",
                        providerName = b.providerName ?: "Service Specialist",
                        providerPhone = b.providerPhone ?: "+91 98765 43210",
                        category = b.category ?: ServiceCategory.ELECTRICIAN,
                        scheduledDate = b.scheduledDate ?: "Scheduled Date",
                        scheduledSlot = b.scheduledSlot ?: "11:00 AM - 01:00 PM",
                        issueDescription = b.issueDescription ?: "General service",
                        status = b.status ?: BookingStatus.PENDING,
                        startOtp = if (b.startOtp.isNullOrBlank()) ((1000..9999).random()).toString() else b.startOtp,
                        completionOtp = if (b.completionOtp.isNullOrBlank()) ((1000..9999).random()).toString() else b.completionOtp
                    )
                } ?: emptyList()
                _bookings.value = sanitized
                saveBookings(sanitized)
            } catch (e: Exception) {
                _bookings.value = emptyList()
            }
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

        // 4. Current User (Customer Authentication)
        val savedUserJson = prefs.getString(KEY_CURRENT_USER, null)
        val loggedInPhone = prefs.getString(KEY_LOGGED_IN_PHONE, null)
        if (!savedUserJson.isNullOrEmpty() && !loggedInPhone.isNullOrEmpty()) {
            try {
                val user = gson.fromJson(savedUserJson, User::class.java)
                val sanitizedUser = if (user.savedAddresses == null) {
                    val initialAddresses = if (user.address.isNotBlank()) {
                        listOf(SavedAddress(id = "addr_default", label = "Home", addressLine = user.address, isDefault = true))
                    } else {
                        emptyList()
                    }
                    user.copy(savedAddresses = initialAddresses)
                } else {
                    user
                }

                _currentUser.value = sanitizedUser
                _isLoggedIn.value = true

                // Load saved addresses safely
                val initialAddresses = if (sanitizedUser.safeSavedAddresses.isNotEmpty()) {
                    sanitizedUser.safeSavedAddresses
                } else if (sanitizedUser.address.isNotBlank()) {
                    listOf(SavedAddress(id = "addr_default", label = "Home", addressLine = sanitizedUser.address, isDefault = true))
                } else {
                    emptyList()
                }
                _savedAddresses.value = initialAddresses
                saveUser(sanitizedUser)
            } catch (e: Exception) {
                _currentUser.value = null
                _isLoggedIn.value = false
                _savedAddresses.value = emptyList()
            }
        } else {
            // First time or logged out: user must register/sign in with phone and password
            _currentUser.value = null
            _isLoggedIn.value = false
            _savedAddresses.value = emptyList()
        }

        // 5. Dismissed Home Bookings
        val savedDismissed = prefs.getStringSet(KEY_DISMISSED_HOME_BOOKINGS, null)
        if (savedDismissed != null) {
            _dismissedHomeBookingIds.value = savedDismissed
        }

        // 6. Wallet Balance & Transactions
        val savedBalance = prefs.getFloat(KEY_WALLET_BALANCE, 500.0f).toDouble()
        _walletBalance.value = savedBalance

        val savedTxJson = prefs.getString(KEY_WALLET_TRANSACTIONS, null)
        if (!savedTxJson.isNullOrEmpty()) {
            val type = object : TypeToken<List<WalletTransaction>>() {}.type
            _walletTransactions.value = gson.fromJson(savedTxJson, type)
        } else {
            val initialTx = listOf(
                WalletTransaction(
                    id = "tx_welcome",
                    amount = 500.0,
                    type = "DEPOSIT",
                    description = "Welcome Bonus Added to SaServe Wallet",
                    timestamp = "Today",
                    upiRefId = "UPI/WEL/98234"
                )
            )
            _walletTransactions.value = initialTx
            saveWallet(500.0, initialTx)
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
        val updatedUser = (_currentUser.value ?: User(id = "cust_user", name = name, phone = phone)).copy(
            name = name,
            email = email,
            phone = phone,
            address = address
        )
        _currentUser.value = updatedUser
        saveUser(updatedUser)
    }

    fun isUserLoggedIn(): Boolean {
        return _isLoggedIn.value
    }

    fun registerCustomer(phone: String, password: String, name: String, address: String): Boolean {
        val cleanPhone = phone.trim()
        val cleanPassword = password.trim()
        if (cleanPhone.isBlank() || cleanPassword.isBlank()) return false

        val initialAddressList = if (address.isNotBlank()) {
            listOf(SavedAddress(id = "addr_" + UUID.randomUUID().toString().take(6), label = "Home", addressLine = address.trim(), isDefault = true))
        } else {
            emptyList()
        }

        val newUser = User(
            id = "cust_" + UUID.randomUUID().toString().take(8),
            name = name.ifBlank { "Customer (${cleanPhone.takeLast(4)})" },
            email = "",
            phone = cleanPhone,
            password = cleanPassword,
            role = UserRole.CUSTOMER,
            address = address.ifBlank { "Home Address" },
            savedAddresses = initialAddressList
        )

        // Save into registered users list
        val usersList = getRegisteredUsers().toMutableList()
        val existingIndex = usersList.indexOfFirst {
            it.phone.replace(Regex("[^0-9]"), "") == cleanPhone.replace(Regex("[^0-9]"), "")
        }
        if (existingIndex != -1) {
            usersList[existingIndex] = newUser
        } else {
            usersList.add(newUser)
        }
        saveRegisteredUsers(usersList)

        // Set as active logged-in user
        _currentUser.value = newUser
        _savedAddresses.value = initialAddressList
        _isLoggedIn.value = true
        saveUser(newUser)
        prefs.edit().putString(KEY_LOGGED_IN_PHONE, cleanPhone).apply()

        // Welcome notification
        val welcomeNotif = AppNotification(
            id = UUID.randomUUID().toString(),
            title = "Welcome to SaServe! 🎉",
            message = "Hello ${newUser.name}! Your account is registered. Explore verified specialists and book with two-OTP security."
        )
        val updatedNotifs = listOf(welcomeNotif) + _notifications.value
        _notifications.value = updatedNotifs
        saveNotifications(updatedNotifs)

        return true
    }

    fun loginCustomer(phone: String, password: String): Boolean {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        val cleanPassword = password.trim()
        if (cleanPhone.isBlank() || cleanPassword.isBlank()) return false

        val users = getRegisteredUsers()
        val matchingUser = users.firstOrNull {
            it.phone.replace(Regex("[^0-9]"), "") == cleanPhone && it.password == cleanPassword
        }

        if (matchingUser != null) {
            val sanitized = if (matchingUser.savedAddresses == null) {
                matchingUser.copy(savedAddresses = if (matchingUser.address.isNotBlank()) {
                    listOf(SavedAddress(id = "addr_default", label = "Home", addressLine = matchingUser.address, isDefault = true))
                } else emptyList())
            } else matchingUser

            _currentUser.value = sanitized
            _savedAddresses.value = sanitized.safeSavedAddresses
            _isLoggedIn.value = true
            saveUser(sanitized)
            prefs.edit().putString(KEY_LOGGED_IN_PHONE, sanitized.phone).apply()
            return true
        }

        // Check if matches previous saved user
        val savedUserJson = prefs.getString(KEY_CURRENT_USER, null)
        if (!savedUserJson.isNullOrEmpty()) {
            val savedUser = gson.fromJson(savedUserJson, User::class.java)
            if (savedUser.phone.replace(Regex("[^0-9]"), "") == cleanPhone &&
                (savedUser.password.isBlank() || savedUser.password == cleanPassword)
            ) {
                val updated = savedUser.copy(
                    password = cleanPassword,
                    savedAddresses = savedUser.savedAddresses ?: if (savedUser.address.isNotBlank()) {
                        listOf(SavedAddress(id = "addr_default", label = "Home", addressLine = savedUser.address, isDefault = true))
                    } else emptyList()
                )
                _currentUser.value = updated
                _savedAddresses.value = updated.safeSavedAddresses
                _isLoggedIn.value = true
                saveUser(updated)
                prefs.edit().putString(KEY_LOGGED_IN_PHONE, updated.phone).apply()
                return true
            }
        }

        return false
    }

    fun logoutCustomer() {
        _currentUser.value = null
        _isLoggedIn.value = false
        prefs.edit().remove(KEY_LOGGED_IN_PHONE).remove(KEY_CURRENT_USER).apply()
    }

    private fun getRegisteredUsers(): List<User> {
        val json = prefs.getString(KEY_REGISTERED_USERS, null) ?: return emptyList()
        val type = object : TypeToken<List<User>>() {}.type
        return try {
            val list: List<User> = gson.fromJson(json, type) ?: emptyList()
            list.map { u ->
                if (u.savedAddresses == null) {
                    val fallback = if (u.address.isNotBlank()) {
                        listOf(SavedAddress(id = "addr_default", label = "Home", addressLine = u.address, isDefault = true))
                    } else emptyList()
                    u.copy(savedAddresses = fallback)
                } else u
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveRegisteredUsers(users: List<User>) {
        prefs.edit().putString(KEY_REGISTERED_USERS, gson.toJson(users)).apply()
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

    fun startBookingWithOtp(bookingId: String, otp: String): Boolean {
        val currentList = _bookings.value
        val targetIndex = currentList.indexOfFirst { it.id == bookingId }
        if (targetIndex == -1) return false

        val currentBooking = currentList[targetIndex]
        if (currentBooking.startOtp != otp.trim()) return false

        val updatedBooking = currentBooking.copy(status = BookingStatus.IN_PROGRESS)
        val updatedList = currentList.toMutableList().apply {
            set(targetIndex, updatedBooking)
        }
        _bookings.value = updatedList
        saveBookings(updatedList)

        // Notification: Service in progress
        val notif = AppNotification(
            id = UUID.randomUUID().toString(),
            title = "Service Started 🛠️",
            message = "${updatedBooking.providerName} has verified the Start OTP and begun the service.",
            bookingId = updatedBooking.id
        )
        val updatedNotifs = listOf(notif) + _notifications.value
        _notifications.value = updatedNotifs
        saveNotifications(updatedNotifs)

        return true
    }

    fun completeBookingWithOtp(bookingId: String, otp: String): Boolean {
        val currentList = _bookings.value
        val targetIndex = currentList.indexOfFirst { it.id == bookingId }
        if (targetIndex == -1) return false

        val currentBooking = currentList[targetIndex]
        if (currentBooking.completionOtp != otp.trim()) return false

        val updatedBooking = currentBooking.copy(status = BookingStatus.COMPLETED)
        val updatedList = currentList.toMutableList().apply {
            set(targetIndex, updatedBooking)
        }
        _bookings.value = updatedList
        saveBookings(updatedList)

        // Happy congratulatory notification
        val notif = AppNotification(
            id = UUID.randomUUID().toString(),
            title = "🎉 Service Completed Successfully!",
            message = "Your service with ${updatedBooking.providerName} is completed. Thank you for choosing SaServe! We'd love your review.",
            bookingId = updatedBooking.id
        )
        val updatedNotifs = listOf(notif) + _notifications.value
        _notifications.value = updatedNotifs
        saveNotifications(updatedNotifs)

        return true
    }

    fun addReviewForBooking(bookingId: String, rating: Float, comment: String): Boolean {
        val currentList = _bookings.value
        val targetIndex = currentList.indexOfFirst { it.id == bookingId }
        if (targetIndex == -1) return false

        val currentBooking = currentList[targetIndex]
        val updatedBooking = currentBooking.copy(
            customerRating = rating,
            customerReview = comment
        )
        val updatedList = currentList.toMutableList().apply {
            set(targetIndex, updatedBooking)
        }
        _bookings.value = updatedList
        saveBookings(updatedList)

        // Add review to provider
        val currentProviders = _providers.value
        val providerIndex = currentProviders.indexOfFirst { it.id == currentBooking.providerId }
        if (providerIndex != -1) {
            val provider = currentProviders[providerIndex]
            val newReview = Review(
                id = "rev_" + UUID.randomUUID().toString().take(6),
                author = currentBooking.customerName.ifBlank { "Customer" },
                rating = rating,
                comment = comment.ifBlank { "Great service! Very satisfied." },
                date = "Today"
            )
            val updatedReviews = listOf(newReview) + provider.reviews
            val newAvgRating = updatedReviews.map { it.rating }.average().toFloat()
            val updatedProvider = provider.copy(
                reviews = updatedReviews,
                reviewCount = updatedReviews.size,
                rating = String.format(java.util.Locale.US, "%.1f", newAvgRating).toFloat()
            )
            val updatedProviderList = currentProviders.toMutableList().apply {
                set(providerIndex, updatedProvider)
            }
            _providers.value = updatedProviderList
            saveProviders(updatedProviderList)
        }

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

    fun dismissBookingFromHome(bookingId: String) {
        val updated = _dismissedHomeBookingIds.value + bookingId
        _dismissedHomeBookingIds.value = updated
        prefs.edit().putStringSet(KEY_DISMISSED_HOME_BOOKINGS, updated).apply()
    }

    private fun saveWallet(balance: Double, txList: List<WalletTransaction>) {
        prefs.edit()
            .putFloat(KEY_WALLET_BALANCE, balance.toFloat())
            .putString(KEY_WALLET_TRANSACTIONS, gson.toJson(txList))
            .apply()
    }

    fun addMoneyToWallet(amount: Double, upiApp: String = "UPI", upiId: String = ""): Boolean {
        if (amount <= 0) return false
        val newBalance = _walletBalance.value + amount
        val refNum = (100000..999999).random()
        val newTx = WalletTransaction(
            id = "tx_" + UUID.randomUUID().toString().take(8),
            amount = amount,
            type = "DEPOSIT",
            description = "Deposit via $upiApp (${if (upiId.isNotBlank()) upiId else "Direct UPI"})",
            timestamp = "Today",
            upiRefId = "UPI/$refNum"
        )
        val updatedTx = listOf(newTx) + _walletTransactions.value
        _walletBalance.value = newBalance
        _walletTransactions.value = updatedTx
        saveWallet(newBalance, updatedTx)
        return true
    }

    fun deductWallet(amount: Double, description: String): Boolean {
        if (amount <= 0 || _walletBalance.value < amount) return false
        val newBalance = _walletBalance.value - amount
        val newTx = WalletTransaction(
            id = "tx_" + UUID.randomUUID().toString().take(8),
            amount = amount,
            type = "PAYMENT",
            description = description.ifBlank { "Service Booking Payment" },
            timestamp = "Today",
            upiRefId = "PAY/${(100000..999999).random()}"
        )
        val updatedTx = listOf(newTx) + _walletTransactions.value
        _walletBalance.value = newBalance
        _walletTransactions.value = updatedTx
        saveWallet(newBalance, updatedTx)
        return true
    }

    fun addSavedAddress(label: String, addressLine: String, isDefault: Boolean = false): Boolean {
        if (addressLine.isBlank()) return false
        val newId = "addr_" + UUID.randomUUID().toString().take(6)
        val cleanLabel = label.ifBlank { "Home" }
        val currentList = _savedAddresses.value
        val updatedList = if (isDefault || currentList.isEmpty()) {
            currentList.map { it.copy(isDefault = false) } + SavedAddress(id = newId, label = cleanLabel, addressLine = addressLine.trim(), isDefault = true)
        } else {
            currentList + SavedAddress(id = newId, label = cleanLabel, addressLine = addressLine.trim(), isDefault = false)
        }
        _savedAddresses.value = updatedList

        // Persist on current user
        _currentUser.value?.let { u ->
            val updatedUser = u.copy(
                address = if (isDefault || u.address.isBlank()) addressLine.trim() else u.address,
                savedAddresses = updatedList
            )
            _currentUser.value = updatedUser
            saveUser(updatedUser)

            val registered = getRegisteredUsers().map { if (it.id == updatedUser.id) updatedUser else it }
            saveRegisteredUsers(registered)
        }
        return true
    }

    fun deleteSavedAddress(id: String) {
        val filtered = _savedAddresses.value.filter { it.id != id }
        val finalAddresses = if (filtered.none { it.isDefault } && filtered.isNotEmpty()) {
            filtered.mapIndexed { idx, addr -> if (idx == 0) addr.copy(isDefault = true) else addr }
        } else {
            filtered
        }
        _savedAddresses.value = finalAddresses
        _currentUser.value?.let { u ->
            val defaultAddr = finalAddresses.firstOrNull { it.isDefault }?.addressLine ?: (finalAddresses.firstOrNull()?.addressLine ?: "")
            val updatedUser = u.copy(address = defaultAddr, savedAddresses = finalAddresses)
            _currentUser.value = updatedUser
            saveUser(updatedUser)

            val registered = getRegisteredUsers().map { if (it.id == updatedUser.id) updatedUser else it }
            saveRegisteredUsers(registered)
        }
    }

    fun setDefaultAddress(id: String) {
        val updated = _savedAddresses.value.map {
            it.copy(isDefault = (it.id == id))
        }
        _savedAddresses.value = updated
        val defaultLine = updated.firstOrNull { it.id == id }?.addressLine ?: return
        _currentUser.value?.let { u ->
            val updatedUser = u.copy(address = defaultLine, savedAddresses = updated)
            _currentUser.value = updatedUser
            saveUser(updatedUser)

            val registered = getRegisteredUsers().map { if (it.id == updatedUser.id) updatedUser else it }
            saveRegisteredUsers(registered)
        }
    }

    companion object {
        private const val KEY_CUSTOM_PROVIDERS = "key_custom_providers_only_v2"
        private const val KEY_CUSTOM_BOOKINGS = "key_custom_bookings_v2"
        private const val KEY_NOTIFICATIONS = "key_notifications_list"
        private const val KEY_CURRENT_USER = "key_current_user"
        private const val KEY_REGISTERED_USERS = "key_registered_users_v2"
        private const val KEY_LOGGED_IN_PHONE = "key_logged_in_phone_v2"
        private const val KEY_DISMISSED_HOME_BOOKINGS = "key_dismissed_home_bookings_v1"
        private const val KEY_WALLET_BALANCE = "key_wallet_balance_v1"
        private const val KEY_WALLET_TRANSACTIONS = "key_wallet_transactions_v1"

        @Volatile
        private var INSTANCE: ServiceSyncRepository? = null

        fun getInstance(context: Context): ServiceSyncRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ServiceSyncRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
