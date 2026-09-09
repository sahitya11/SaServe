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

    private val _themeMode = MutableStateFlow<AppThemeMode>(AppThemeMode.SYSTEM)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _pendingCancellationFee = MutableStateFlow<Double>(0.0)
    val pendingCancellationFee: StateFlow<Double> = _pendingCancellationFee.asStateFlow()

    init {
        loadOrInitializeData()
    }

    private fun loadOrInitializeData() {
        // Clear out old pre-seeded provider caches
        prefs.edit().remove("key_providers_list").remove("key_providers_list_inr").apply()

        // 1. Providers: Seeded with realistic verified Indian specialists with varied ratings and reviews
        val savedProvidersJson = prefs.getString(KEY_CUSTOM_PROVIDERS, null)
        if (!savedProvidersJson.isNullOrEmpty()) {
            val type = object : TypeToken<List<ServiceProvider>>() {}.type
            val loaded: List<ServiceProvider>? = gson.fromJson(savedProvidersJson, type)
            if (!loaded.isNullOrEmpty()) {
                _providers.value = loaded
            } else {
                val initial = getInitialIndianProviders()
                _providers.value = initial
                saveProviders(initial)
            }
        } else {
            val initial = getInitialIndianProviders()
            _providers.value = initial
            saveProviders(initial)
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

        // 7. Theme Mode (Default to Black & Light Theme)
        val savedTheme = prefs.getString(KEY_THEME_MODE, AppThemeMode.DARK.name)
        _themeMode.value = try {
            AppThemeMode.valueOf(savedTheme ?: AppThemeMode.DARK.name)
        } catch (e: Exception) {
            AppThemeMode.DARK
        }

        // 8. Pending Cancellation Fee
        val savedPendingFee = prefs.getFloat(KEY_PENDING_CANCELLATION_FEE, 0.0f).toDouble()
        _pendingCancellationFee.value = savedPendingFee
    }

    fun savePendingCancellationFee(fee: Double) {
        _pendingCancellationFee.value = fee
        prefs.edit().putFloat(KEY_PENDING_CANCELLATION_FEE, fee.toFloat()).apply()
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
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

    fun registerCustomer(
        phone: String,
        password: String,
        name: String,
        address: String,
        flatHouseNo: String = "",
        streetArea: String = "",
        landmark: String = "",
        reachInstructions: String = ""
    ): Boolean {
        val cleanPhone = phone.trim()
        val cleanPassword = password.trim()
        if (cleanPhone.isBlank() || cleanPassword.isBlank()) return false

        val fullAddressLine = if (address.isNotBlank()) address.trim() else listOfNotNull(
            flatHouseNo.takeIf { it.isNotBlank() },
            streetArea.takeIf { it.isNotBlank() },
            landmark.takeIf { it.isNotBlank() }?.let { "Near $it" }
        ).joinToString(", ")

        val initialAddressList = if (fullAddressLine.isNotBlank() || flatHouseNo.isNotBlank()) {
            listOf(
                SavedAddress(
                    id = "addr_" + UUID.randomUUID().toString().take(6),
                    label = "Home",
                    addressLine = if (fullAddressLine.isNotBlank()) fullAddressLine else "Home Address",
                    flatHouseNo = flatHouseNo.trim(),
                    streetArea = streetArea.trim(),
                    landmark = landmark.trim(),
                    reachInstructions = reachInstructions.trim(),
                    isDefault = true
                )
            )
        } else {
            emptyList()
        }

        val primaryAddressString = initialAddressList.firstOrNull()?.formattedDisplayAddress
            ?: address.ifBlank { "Home Address" }

        val newUser = User(
            id = "cust_" + UUID.randomUUID().toString().take(8),
            name = name.ifBlank { "Customer (${cleanPhone.takeLast(4)})" },
            email = "",
            phone = cleanPhone,
            password = cleanPassword,
            role = UserRole.CUSTOMER,
            address = primaryAddressString,
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

    fun registerWithPhoneOtp(name: String, phone: String): Boolean {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "").trim()
        val cleanName = name.trim()
        if (cleanPhone.length < 10) return false

        val newUser = User(
            id = "cust_" + UUID.randomUUID().toString().take(8),
            name = if (cleanName.isNotBlank()) cleanName else "Customer (${cleanPhone.takeLast(4)})",
            email = "",
            phone = cleanPhone,
            password = "",
            role = UserRole.CUSTOMER,
            address = "",
            savedAddresses = emptyList()
        )

        val usersList = getRegisteredUsers().toMutableList()
        val existingIndex = usersList.indexOfFirst {
            it.phone.replace(Regex("[^0-9]"), "") == cleanPhone
        }
        if (existingIndex != -1) {
            usersList[existingIndex] = newUser
        } else {
            usersList.add(newUser)
        }
        saveRegisteredUsers(usersList)

        _currentUser.value = newUser
        _savedAddresses.value = emptyList()
        _isLoggedIn.value = true
        saveUser(newUser)
        prefs.edit().putString(KEY_LOGGED_IN_PHONE, cleanPhone).apply()

        // Welcome notification
        val welcomeNotif = AppNotification(
            id = UUID.randomUUID().toString(),
            title = "Welcome to SaServe! 🎉",
            message = "Hello ${newUser.name}! Your account has been verified via OTP."
        )
        val updatedNotifs = listOf(welcomeNotif) + _notifications.value
        _notifications.value = updatedNotifs
        saveNotifications(updatedNotifs)

        return true
    }

    fun loginWithPhoneOtp(phone: String): Boolean {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "").trim()
        if (cleanPhone.length < 10) return false

        val users = getRegisteredUsers()
        val matchingUser = users.firstOrNull {
            it.phone.replace(Regex("[^0-9]"), "") == cleanPhone
        }

        val userToLogin = matchingUser ?: User(
            id = "cust_" + UUID.randomUUID().toString().take(8),
            name = "Customer (${cleanPhone.takeLast(4)})",
            phone = cleanPhone,
            role = UserRole.CUSTOMER,
            address = "",
            savedAddresses = emptyList()
        )

        val usersList = getRegisteredUsers().toMutableList()
        if (matchingUser == null) {
            usersList.add(userToLogin)
            saveRegisteredUsers(usersList)
        }

        _currentUser.value = userToLogin
        _savedAddresses.value = userToLogin.safeSavedAddresses
        _isLoggedIn.value = true
        saveUser(userToLogin)
        prefs.edit().putString(KEY_LOGGED_IN_PHONE, cleanPhone).apply()
        return true
    }

    fun updateUserAddress(
        flatHouseNo: String,
        streetArea: String,
        landmark: String,
        reachInstructions: String,
        isDefault: Boolean = true
    ) {
        val currentUser = _currentUser.value ?: return
        val newAddress = SavedAddress(
            id = "addr_" + UUID.randomUUID().toString().take(6),
            label = "Home",
            addressLine = if (streetArea.isNotBlank()) streetArea else "Home Address",
            flatHouseNo = flatHouseNo.trim(),
            streetArea = streetArea.trim(),
            landmark = landmark.trim(),
            reachInstructions = reachInstructions.trim(),
            isDefault = isDefault
        )

        val updatedList = listOf(newAddress) + _savedAddresses.value.filter { it.id != newAddress.id }
        _savedAddresses.value = updatedList

        val updatedUser = currentUser.copy(
            address = newAddress.formattedDisplayAddress,
            savedAddresses = updatedList
        )
        _currentUser.value = updatedUser
        saveUser(updatedUser)

        val registered = getRegisteredUsers().map { if (it.id == updatedUser.id) updatedUser else it }
        saveRegisteredUsers(registered)
    }

    fun loginCustomer(phone: String, password: String): Boolean {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        val cleanPassword = password.trim()
        if (cleanPhone.isBlank()) return false

        val users = getRegisteredUsers()
        val matchingUser = users.firstOrNull {
            it.phone.replace(Regex("[^0-9]"), "") == cleanPhone && (it.password.isBlank() || it.password == cleanPassword)
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
     * Updates the current customer's profile info.
     */
    fun updateUserProfile(name: String, phone: String, email: String, address: String) {
        val user = _currentUser.value ?: return
        val updated = user.copy(
            name = name.trim().ifBlank { user.name },
            phone = phone.trim().ifBlank { user.phone },
            email = email.trim(),
            address = address.trim().ifBlank { user.address }
        )
        _currentUser.value = updated
        saveUser(updated)
        val registered = getRegisteredUsers().map { if (it.id == updated.id) updated else it }
        saveRegisteredUsers(registered)
    }

    /**
     * Ola/Uber-style on-demand service broadcast dispatch.
     * Broadcasts request to eligible verified specialists in the category,
     * simulates real-time acceptance, and assigns the specialist automatically.
     */
    suspend fun broadcastServiceDispatch(
        category: ServiceCategory,
        date: String,
        timeSlot: String,
        address: String,
        issueDescription: String
    ): Booking {
        val user = _currentUser.value
        val startOtp = ((1000..9999).random()).toString()
        val completionOtp = ((1000..9999).random()).toString()

        // Match existing provider or dynamically assign certified specialist
        val matchedProvider = _providers.value.firstOrNull { it.category == category && it.isAvailable }
            ?: run {
                val mockName = when (category) {
                    ServiceCategory.ELECTRICIAN -> "Ramesh Verma"
                    ServiceCategory.PLUMBER -> "Sunil Kumar"
                    ServiceCategory.CARPENTER -> "Mohan Lal"
                    ServiceCategory.MECHANIC -> "Vikram Singh"
                    ServiceCategory.APPLIANCE_REPAIR -> "Amit Saini"
                    ServiceCategory.PAINTER -> "Rajesh Patel"
                    ServiceCategory.MASON -> "Harish Rawat"
                    ServiceCategory.GARDENER -> "Manoj Saini"
                    ServiceCategory.HOUSE_CLEANING -> "Pooja Sharma"
                }
                addServiceProvider(
                    name = mockName,
                    phone = "+91 98${(10000000..99999999).random()}",
                    email = "${mockName.lowercase().replace(" ", "")}@saserve.com",
                    category = category,
                    hourlyRate = 399.0,
                    experienceYears = 6,
                    bio = "Certified SaServe ${category.displayName} specialist. Equipped with verified tools.",
                    location = "Nearby Specialist (1.2 km away)",
                    rating = 4.9f
                )
            }

        val appliedCancellationFee = _pendingCancellationFee.value
        if (appliedCancellationFee > 0.0) {
            savePendingCancellationFee(0.0)
        }

        val booking = Booking(
            id = "bk_" + UUID.randomUUID().toString().take(8),
            customerId = user?.id ?: "cust_user",
            customerName = user?.name ?: "Customer User",
            customerPhone = user?.phone ?: "+91 98765 43210",
            customerAddress = if (address.isNotBlank()) address else (user?.address ?: "Current Location"),
            providerId = matchedProvider.id,
            providerName = matchedProvider.name,
            providerPhone = matchedProvider.phone,
            category = category,
            scheduledDate = date,
            scheduledSlot = timeSlot,
            issueDescription = if (issueDescription.isNotBlank()) issueDescription else "On-demand ${category.displayName} service",
            status = BookingStatus.PENDING,
            hourlyRate = matchedProvider.hourlyRate,
            startOtp = startOtp,
            completionOtp = completionOtp,
            cancellationFee = appliedCancellationFee
        )

        val updatedBookings = listOf(booking) + _bookings.value
        _bookings.value = updatedBookings
        saveBookings(updatedBookings)

        // Notification: Broadcast initiated
        val newNotif = AppNotification(
            id = UUID.randomUUID().toString(),
            title = "Searching Nearby Specialists 📡",
            message = "Broadcasting your ${category.displayName} request to nearby specialists...",
            bookingId = booking.id
        )
        _notifications.value = listOf(newNotif) + _notifications.value
        saveNotifications(_notifications.value)

        // Real-time acceptance delay (Ola/Uber ride match simulation)
        kotlinx.coroutines.delay(2600)
        acceptBooking(booking.id)

        return _bookings.value.firstOrNull { it.id == booking.id } ?: booking.copy(status = BookingStatus.ACCEPTED)
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

    /**
     * Cancels a booking with reason and calculates late cancellation penalty.
     * If the service has already started (IN_PROGRESS), a ₹99 fine is recorded
     * and applied to the customer's next service booking.
     */
    fun cancelBookingWithDetails(bookingId: String, reason: String = "Customer cancelled"): Pair<Boolean, Double> {
        val currentList = _bookings.value
        val targetIndex = currentList.indexOfFirst { it.id == bookingId }
        if (targetIndex == -1) return Pair(false, 0.0)

        val currentBooking = currentList[targetIndex]
        val feeApplied = if (currentBooking.status == BookingStatus.IN_PROGRESS) 99.0 else 0.0
        if (feeApplied > 0.0) {
            val newPending = _pendingCancellationFee.value + feeApplied
            savePendingCancellationFee(newPending)
        }

        val updatedBooking = currentBooking.copy(
            status = BookingStatus.CANCELLED,
            cancellationReason = reason,
            cancellationFee = feeApplied
        )
        val updatedList = currentList.toMutableList().apply {
            set(targetIndex, updatedBooking)
        }
        _bookings.value = updatedList
        saveBookings(updatedList)

        val notifTitle = if (feeApplied > 0.0) "Booking Cancelled (₹${feeApplied.toInt()} Fine Applied)" else "Booking Cancelled"
        val notifMessage = if (feeApplied > 0.0) {
            "Your booking with ${currentBooking.providerName} was cancelled. Since the service had already started, a ₹${feeApplied.toInt()} technician mobilization fine was recorded and will be added to your next booking."
        } else {
            "Your booking with ${currentBooking.providerName} was cancelled ($reason). No cancellation charges were applied."
        }
        val cancelNotif = AppNotification(
            id = UUID.randomUUID().toString(),
            title = notifTitle,
            message = notifMessage,
            bookingId = currentBooking.id
        )
        _notifications.value = listOf(cancelNotif) + _notifications.value
        saveNotifications(_notifications.value)

        return Pair(true, feeApplied)
    }

    fun cancelBooking(bookingId: String): Boolean {
        return cancelBookingWithDetails(bookingId).first
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

    fun addSavedAddress(
        label: String,
        addressLine: String,
        flatHouseNo: String = "",
        streetArea: String = "",
        landmark: String = "",
        reachInstructions: String = "",
        isDefault: Boolean = false
    ): Boolean {
        val cleanLabel = label.ifBlank { "Home" }
        val newId = "addr_" + UUID.randomUUID().toString().take(6)
        val fullLine = if (addressLine.isNotBlank()) addressLine.trim() else listOfNotNull(
            flatHouseNo.takeIf { it.isNotBlank() },
            streetArea.takeIf { it.isNotBlank() },
            landmark.takeIf { it.isNotBlank() }?.let { "Near $it" }
        ).joinToString(", ")

        if (fullLine.isBlank() && flatHouseNo.isBlank() && streetArea.isBlank()) return false

        val newAddress = SavedAddress(
            id = newId,
            label = cleanLabel,
            addressLine = fullLine,
            flatHouseNo = flatHouseNo.trim(),
            streetArea = streetArea.trim(),
            landmark = landmark.trim(),
            reachInstructions = reachInstructions.trim(),
            isDefault = isDefault
        )

        val currentList = _savedAddresses.value
        val updatedList = if (isDefault || currentList.isEmpty()) {
            currentList.map { it.copy(isDefault = false) } + newAddress.copy(isDefault = true)
        } else {
            currentList + newAddress
        }
        _savedAddresses.value = updatedList

        // Persist on current user
        _currentUser.value?.let { u ->
            val updatedUser = u.copy(
                address = if (isDefault || u.address.isBlank()) newAddress.formattedDisplayAddress else u.address,
                savedAddresses = updatedList
            )
            _currentUser.value = updatedUser
            saveUser(updatedUser)

            val registered = getRegisteredUsers().map { if (it.id == updatedUser.id) updatedUser else it }
            saveRegisteredUsers(registered)
        }
        return true
    }

    fun updateSavedAddress(
        id: String,
        label: String,
        addressLine: String,
        flatHouseNo: String = "",
        streetArea: String = "",
        landmark: String = "",
        reachInstructions: String = "",
        isDefault: Boolean = false
    ): Boolean {
        val cleanLabel = label.ifBlank { "Home" }
        val fullLine = if (addressLine.isNotBlank()) addressLine.trim() else listOfNotNull(
            flatHouseNo.takeIf { it.isNotBlank() },
            streetArea.takeIf { it.isNotBlank() },
            landmark.takeIf { it.isNotBlank() }?.let { "Near $it" }
        ).joinToString(", ")

        if (fullLine.isBlank() && flatHouseNo.isBlank() && streetArea.isBlank()) return false

        val currentList = _savedAddresses.value
        val index = currentList.indexOfFirst { it.id == id }
        if (index == -1) return false

        val wasDefault = currentList[index].isDefault
        val shouldBeDefault = isDefault || wasDefault

        val updatedItem = SavedAddress(
            id = id,
            label = cleanLabel,
            addressLine = fullLine,
            flatHouseNo = flatHouseNo.trim(),
            streetArea = streetArea.trim(),
            landmark = landmark.trim(),
            reachInstructions = reachInstructions.trim(),
            isDefault = shouldBeDefault
        )

        val updatedList = currentList.map { item ->
            if (item.id == id) {
                updatedItem
            } else if (isDefault) {
                item.copy(isDefault = false)
            } else {
                item
            }
        }
        _savedAddresses.value = updatedList

        // Persist on current user
        _currentUser.value?.let { u ->
            val defaultAddr = updatedList.firstOrNull { it.isDefault }?.formattedDisplayAddress
                ?: updatedItem.formattedDisplayAddress
            val updatedUser = u.copy(
                address = if (shouldBeDefault) defaultAddr else u.address,
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

    fun clearAllBookings() {
        _bookings.value = emptyList()
        prefs.edit().remove(KEY_CUSTOM_BOOKINGS).apply()
    }

    private fun getInitialIndianProviders(): List<ServiceProvider> {
        return listOf(
            // Electricians
            ServiceProvider(
                id = "prov_elec_1",
                userId = "user_elec_1",
                name = "Ramesh Sharma",
                phone = "+91 98112 34567",
                email = "ramesh.sharma@saserve.com",
                category = ServiceCategory.ELECTRICIAN,
                rating = 4.92f,
                reviewCount = 184,
                experienceYears = 8,
                hourlyRate = 249.0,
                bio = "Certified Senior Electrician. Expert in MCB tripping, fan installation, switchboard repair & home wiring safety audits.",
                location = "Sector 14 (1.2 km away)",
                distanceMiles = 0.8,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Ceiling Fans", "MCB Tripping Fix", "Switchboards", "Wiring Safety"),
                reviews = listOf(
                    Review("rev_1", "Ananya Roy", 5.0f, "Fixed my ceiling fan regulator and MCB tripping issue in 20 minutes. Very polite.", "2 days ago"),
                    Review("rev_2", "Gaurav Verma", 4.8f, "Punctual and knowledgeable electrician. Clean work!", "1 week ago")
                )
            ),
            ServiceProvider(
                id = "prov_elec_2",
                userId = "user_elec_2",
                name = "Rajesh Verma",
                phone = "+91 98223 45678",
                email = "rajesh.verma@saserve.com",
                category = ServiceCategory.ELECTRICIAN,
                rating = 4.81f,
                reviewCount = 92,
                experienceYears = 5,
                hourlyRate = 199.0,
                bio = "Specialist in false ceiling COB LED lights, chandelier assembly & heavy appliance socket lines.",
                location = "Indira Nagar (2.5 km away)",
                distanceMiles = 1.6,
                isAvailable = true,
                isVerified = true,
                skills = listOf("LED Lights", "Chandeliers", "AC Power Points"),
                reviews = listOf(
                    Review("rev_3", "Pooja Mehta", 5.0f, "Installed 8 recessed lights and repaired switchboard sparking. Great job.", "3 days ago")
                )
            ),
            ServiceProvider(
                id = "prov_elec_3",
                userId = "user_elec_3",
                name = "Amit Patel",
                phone = "+91 98334 56789",
                email = "amit.patel@saserve.com",
                category = ServiceCategory.ELECTRICIAN,
                rating = 4.96f,
                reviewCount = 240,
                experienceYears = 10,
                hourlyRate = 299.0,
                bio = "Industrial & residential electrical safety specialist. Inverter wiring, geyser repair & earthing testing.",
                location = "Civil Lines (0.8 km away)",
                distanceMiles = 0.5,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Inverters", "Geysers", "Earthing & Short Circuit"),
                reviews = listOf(
                    Review("rev_4", "Kunal Bansal", 5.0f, "Quickly resolved an inverter backup fault that 2 other electricians could not find.", "Yesterday")
                )
            ),

            // Plumbers
            ServiceProvider(
                id = "prov_plumb_1",
                userId = "user_plumb_1",
                name = "Sunil Kumar",
                phone = "+91 98445 67890",
                email = "sunil.kumar@saserve.com",
                category = ServiceCategory.PLUMBER,
                rating = 4.86f,
                reviewCount = 130,
                experienceYears = 7,
                hourlyRate = 199.0,
                bio = "Concealed pipe leakage detection, kitchen sink drain unclogging & bathroom diverter mixer repair.",
                location = "Model Town (1.5 km away)",
                distanceMiles = 0.9,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Pipe Leakage", "Diverter Mixers", "Drain Unclogging"),
                reviews = listOf(
                    Review("rev_5", "Deepak Chopra", 5.0f, "Replaced our dripping shower mixer smoothly without damaging wall tiles.", "4 days ago")
                )
            ),
            ServiceProvider(
                id = "prov_plumb_2",
                userId = "user_plumb_2",
                name = "Deepak Joshi",
                phone = "+91 98556 78901",
                email = "deepak.joshi@saserve.com",
                category = ServiceCategory.PLUMBER,
                rating = 4.91f,
                reviewCount = 210,
                experienceYears = 9,
                hourlyRate = 249.0,
                bio = "High pressure water pumps, automatic float valves & complete toilet flush tank overhauls.",
                location = "Rajendra Nagar (3.0 km away)",
                distanceMiles = 1.9,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Water Pumps", "Flush Tanks", "Tank Cleaning"),
                reviews = listOf(
                    Review("rev_6", "Sonia Sengupta", 4.9f, "Very efficient! Fixed water pressure problem and installed jet spray.", "1 week ago")
                )
            ),
            ServiceProvider(
                id = "prov_plumb_3",
                userId = "user_plumb_3",
                name = "Manoj Yadav",
                phone = "+91 98667 89012",
                email = "manoj.yadav@saserve.com",
                category = ServiceCategory.PLUMBER,
                rating = 4.75f,
                reviewCount = 78,
                experienceYears = 4,
                hourlyRate = 149.0,
                bio = "Affordable domestic plumbing, silicone sealing, washbasin trap cleaning & new tap fittings.",
                location = "Gandhi Colony (2.1 km away)",
                distanceMiles = 1.3,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Tap Repair", "Washbasin", "Silicone Sealing"),
                reviews = listOf(
                    Review("rev_7", "Rajat Goel", 4.7f, "Reasonable rates and fast service for kitchen sink blockage.", "2 weeks ago")
                )
            ),

            // Carpenters
            ServiceProvider(
                id = "prov_carp_1",
                userId = "user_carp_1",
                name = "Mohan Lal Suthar",
                phone = "+91 98778 90123",
                email = "mohan.suthar@saserve.com",
                category = ServiceCategory.CARPENTER,
                rating = 4.93f,
                reviewCount = 310,
                experienceYears = 14,
                hourlyRate = 299.0,
                bio = "Master wood craftsman. Modular kitchen telescopic runners, mortise locks & custom wardrobe hydraulic hinges.",
                location = "Kirti Nagar (1.8 km away)",
                distanceMiles = 1.1,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Mortise Locks", "Modular Kitchen", "Wardrobe Channels"),
                reviews = listOf(
                    Review("rev_8", "Meera Krishnan", 5.0f, "Repaired our sagging wardrobe door and aligned the mortise lock perfectly.", "3 days ago")
                )
            ),
            ServiceProvider(
                id = "prov_carp_2",
                userId = "user_carp_2",
                name = "Anil Chauhan",
                phone = "+91 98889 01234",
                email = "anil.chauhan@saserve.com",
                category = ServiceCategory.CARPENTER,
                rating = 4.82f,
                reviewCount = 115,
                experienceYears = 6,
                hourlyRate = 249.0,
                bio = "Furniture assembly (IKEA, Custom Furniture), hydraulic bed lift support & sliding window track repair.",
                location = "Subhash Nagar (2.4 km away)",
                distanceMiles = 1.5,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Furniture Assembly", "Bed Hydraulics", "Window Mesh"),
                reviews = listOf(
                    Review("rev_9", "Vikas Malhotra", 4.8f, "Assembled king size storage bed and study table cleanly.", "5 days ago")
                )
            ),
            ServiceProvider(
                id = "prov_carp_3",
                userId = "user_carp_3",
                name = "Suresh Sharma",
                phone = "+91 98990 12345",
                email = "suresh.sharma@saserve.com",
                category = ServiceCategory.CARPENTER,
                rating = 4.70f,
                reviewCount = 64,
                experienceYears = 5,
                hourlyRate = 199.0,
                bio = "Door latch alignment, wood touchups, teak PU clear polish & floating wall shelf mounting.",
                location = "Shastri Nagar (3.2 km away)",
                distanceMiles = 2.0,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Wall Drill Work", "Wood Touchup", "Door Latches"),
                reviews = listOf(
                    Review("rev_10", "Sneha Rao", 5.0f, "Mounted 4 heavy floating shelves securely without any wall cracks.", "1 week ago")
                )
            ),

            // Mechanics
            ServiceProvider(
                id = "prov_mech_1",
                userId = "user_mech_1",
                name = "Vikram Singh Rathore",
                phone = "+91 98101 23456",
                email = "vikram.rathore@saserve.com",
                category = ServiceCategory.MECHANIC,
                rating = 4.95f,
                reviewCount = 275,
                experienceYears = 11,
                hourlyRate = 399.0,
                bio = "Automotive engineer. Engine diagnostic scan, synthetic oil replacement & disc brake skimming.",
                location = "Automobile Hub (2.0 km away)",
                distanceMiles = 1.2,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Engine Diagnostics", "Brake Pad Change", "Synthetic Oil"),
                reviews = listOf(
                    Review("rev_11", "Arjun Kapoor", 5.0f, "Identified check engine light code and changed brake pads at home. Super convenient!", "Yesterday")
                )
            ),
            ServiceProvider(
                id = "prov_mech_2",
                userId = "user_mech_2",
                name = "Pradeep Yadav",
                phone = "+91 98212 34567",
                email = "pradeep.yadav@saserve.com",
                category = ServiceCategory.MECHANIC,
                rating = 4.80f,
                reviewCount = 142,
                experienceYears = 7,
                hourlyRate = 249.0,
                bio = "Two-wheeler master mechanic. Royal Enfield tuning, carburettor cleaning, brake cable & oil service.",
                location = "Station Road (1.1 km away)",
                distanceMiles = 0.7,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Two Wheeler Tuneup", "Carburettor Tuning", "Chain Lubrication"),
                reviews = listOf(
                    Review("rev_12", "Mohit Jain", 4.8f, "Tuned my Bullet 350 engine smoothly, starts on first kick now.", "4 days ago")
                )
            ),
            ServiceProvider(
                id = "prov_mech_3",
                userId = "user_mech_3",
                name = "Rahul Saxena",
                phone = "+91 98323 45678",
                email = "rahul.saxena@saserve.com",
                category = ServiceCategory.MECHANIC,
                rating = 4.88f,
                reviewCount = 89,
                experienceYears = 6,
                hourlyRate = 299.0,
                bio = "Car AC cooling recharge, emergency SOS battery jumpstart & 50-point pre-trip inspection.",
                location = "Highway Bypass (4.0 km away)",
                distanceMiles = 2.5,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Battery Jumpstart", "Tyre Punctures", "Pre-Trip Inspection"),
                reviews = listOf(
                    Review("rev_13", "Sanjay Nair", 5.0f, "Jumpstarted my car battery in 15 minutes on a rainy morning.", "6 days ago")
                )
            ),

            // Appliance Repair
            ServiceProvider(
                id = "prov_app_1",
                userId = "user_app_1",
                name = "Amit Saini",
                phone = "+91 98434 56789",
                email = "amit.saini@saserve.com",
                category = ServiceCategory.APPLIANCE_REPAIR,
                rating = 4.90f,
                reviewCount = 198,
                experienceYears = 8,
                hourlyRate = 349.0,
                bio = "Certified appliance technician. Single/double door fridge cooling, thermostat & washing machine spin drum repair.",
                location = "Vikas Puri (1.4 km away)",
                distanceMiles = 0.9,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Refrigerator Fix", "Washing Machine Drum", "Thermostats"),
                reviews = listOf(
                    Review("rev_14", "Ritika Saxena", 5.0f, "My front load washing machine stopped spinning; Amit replaced the capacitor and drum belt.", "2 days ago")
                )
            ),
            ServiceProvider(
                id = "prov_app_2",
                userId = "user_app_2",
                name = "Sandeep Tiwari",
                phone = "+91 98545 67890",
                email = "sandeep.tiwari@saserve.com",
                category = ServiceCategory.APPLIANCE_REPAIR,
                rating = 4.83f,
                reviewCount = 160,
                experienceYears = 7,
                hourlyRate = 399.0,
                bio = "Air Conditioner deep jet clean, split AC gas refill & copper pipe leakage brazing specialist.",
                location = "Janakpuri (2.8 km away)",
                distanceMiles = 1.7,
                isAvailable = true,
                isVerified = true,
                skills = listOf("AC Jet Cleaning", "Gas Recharge", "Copper Brazing"),
                reviews = listOf(
                    Review("rev_15", "Hemant Pandey", 4.9f, "Thorough AC jet pump cleaning, cooling is ice-cold now.", "3 days ago")
                )
            ),
            ServiceProvider(
                id = "prov_app_3",
                userId = "user_app_3",
                name = "Neeraj Gupta",
                phone = "+91 98656 78901",
                email = "neeraj.gupta@saserve.com",
                category = ServiceCategory.APPLIANCE_REPAIR,
                rating = 4.78f,
                reviewCount = 85,
                experienceYears = 5,
                hourlyRate = 299.0,
                bio = "Microwave magnetron fix, RO water purifier multi-stage filter change & LED TV display repair.",
                location = "Preet Vihar (3.5 km away)",
                distanceMiles = 2.2,
                isAvailable = true,
                isVerified = true,
                skills = listOf("RO Filter Change", "Microwave Heating", "Smart TV Panels"),
                reviews = listOf(
                    Review("rev_16", "Alok Sinha", 4.8f, "Replaced RO sediment and carbon filters, TDS balanced accurately.", "1 week ago")
                )
            ),

            // Painters
            ServiceProvider(
                id = "prov_paint_1",
                userId = "user_paint_1",
                name = "Rajesh Patel",
                phone = "+91 98767 89012",
                email = "rajesh.patel@saserve.com",
                category = ServiceCategory.PAINTER,
                rating = 4.92f,
                reviewCount = 220,
                experienceYears = 12,
                hourlyRate = 449.0,
                bio = "Luxury wall textures, stencils, terrace waterproofing & anti-fungal dampness treatment.",
                location = "Lajpat Nagar (2.2 km away)",
                distanceMiles = 1.4,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Texture Walls", "Waterproofing", "Dampness Fix"),
                reviews = listOf(
                    Review("rev_17", "Divya Nambiar", 5.0f, "Beautiful metallic texture feature wall in my living room!", "3 days ago")
                )
            ),
            ServiceProvider(
                id = "prov_paint_2",
                userId = "user_paint_2",
                name = "Santosh Kumar",
                phone = "+91 98878 90123",
                email = "santosh.kumar@saserve.com",
                category = ServiceCategory.PAINTER,
                rating = 4.76f,
                reviewCount = 95,
                experienceYears = 6,
                hourlyRate = 349.0,
                bio = "Single room quick repaint, Asian Paints Royale/Apex emulsion, masking tape clean borders.",
                location = "Mayur Vihar (3.1 km away)",
                distanceMiles = 1.9,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Room Repainting", "Asian Paints Emulsion", "Masking Cleanliness"),
                reviews = listOf(
                    Review("rev_18", "Tarun Bajaj", 4.7f, "Repainted bedroom in 1 day with zero paint splatters on the floor.", "5 days ago")
                )
            ),
            ServiceProvider(
                id = "prov_paint_3",
                userId = "user_paint_3",
                name = "Mahendra Verma",
                phone = "+91 98989 01234",
                email = "mahendra.verma@saserve.com",
                category = ServiceCategory.PAINTER,
                rating = 4.85f,
                reviewCount = 140,
                experienceYears = 9,
                hourlyRate = 399.0,
                bio = "Exterior weatherproof coats, anti-rust grill enamel spraying & wood PU touchups.",
                location = "Rohini (4.5 km away)",
                distanceMiles = 2.8,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Exterior Emulsion", "Anti-Rust Enamel", "Wood Staining"),
                reviews = listOf(
                    Review("rev_19", "Bhavna Joshi", 5.0f, "Sprayed enamel on balcony metal railings, looks brand new.", "1 week ago")
                )
            ),
            ServiceProvider(
                id = "prov_paint_4",
                userId = "user_paint_4",
                name = "Preeti Kashyap",
                phone = "+91 98199 43210",
                email = "preeti.kashyap@saserve.com",
                category = ServiceCategory.PAINTER,
                rating = 4.95f,
                reviewCount = 178,
                experienceYears = 8,
                hourlyRate = 499.0,
                bio = "Color consultant & decorative wall designer. Metallic stucco, velvet accents, wallpaper & eco-friendly low-VOC paints.",
                location = "Greater Kailash (1.8 km away)",
                distanceMiles = 1.1,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Color Consultation", "Velvet Textures", "Low-VOC Paints"),
                reviews = listOf(
                    Review("rev_20", "Megha Kapoor", 5.0f, "Helped us pick the perfect accent shade for master bedroom. Flawless finish.", "4 days ago")
                )
            ),

            // Additional Female Specialists for Trades
            ServiceProvider(
                id = "prov_elec_4",
                userId = "user_elec_4",
                name = "Priya Sundaram",
                phone = "+91 98210 56781",
                email = "priya.sundaram@saserve.com",
                category = ServiceCategory.ELECTRICIAN,
                rating = 4.93f,
                reviewCount = 162,
                experienceYears = 7,
                hourlyRate = 279.0,
                bio = "Certified Electrical Wirewoman & Smart Home Technician. Smart automation switches, LED architectural lighting & safe circuit diagnostics.",
                location = "Hauz Khas (1.5 km away)",
                distanceMiles = 0.9,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Smart Switches", "Architectural Lighting", "Circuit Diagnostics"),
                reviews = listOf(
                    Review("rev_21", "Tanvi Sharma", 5.0f, "Priya configured our smart home touch switches seamlessly. Very efficient!", "2 days ago")
                )
            ),
            ServiceProvider(
                id = "prov_plumb_4",
                userId = "user_plumb_4",
                name = "Anita Maurya",
                phone = "+91 98321 67892",
                email = "anita.maurya@saserve.com",
                category = ServiceCategory.PLUMBER,
                rating = 4.88f,
                reviewCount = 110,
                experienceYears = 6,
                hourlyRate = 249.0,
                bio = "Sanitary & CPVC pipe specialist. Kitchen sink installation, mixer taps, drain clearing & pressure pump fittings.",
                location = "Vasant Kunj (2.0 km away)",
                distanceMiles = 1.3,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Kitchen Sinks", "Mixer Taps", "Pressure Pumps"),
                reviews = listOf(
                    Review("rev_22", "Rahul Saxena", 4.9f, "Solved our persistent bathroom leakage issue quickly and cleanly.", "3 days ago")
                )
            ),
            ServiceProvider(
                id = "prov_carp_4",
                userId = "user_carp_4",
                name = "Shalini Rathore",
                phone = "+91 98432 78903",
                email = "shalini.rathore@saserve.com",
                category = ServiceCategory.CARPENTER,
                rating = 4.91f,
                reviewCount = 145,
                experienceYears = 8,
                hourlyRate = 299.0,
                bio = "Modular woodcraft, custom modular wardrobes, drawer soft-close sliders & artistic wooden partition panels.",
                location = "Defence Colony (1.6 km away)",
                distanceMiles = 1.0,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Modular Wardrobes", "Soft-Close Sliders", "Wood Partitions"),
                reviews = listOf(
                    Review("rev_23", "Simran Gill", 5.0f, "Realigned our entire wardrobe sliding track with precision. Highly recommend!", "1 week ago")
                )
            ),
            ServiceProvider(
                id = "prov_mech_4",
                userId = "user_mech_4",
                name = "Neha Kulkarni",
                phone = "+91 98543 89014",
                email = "neha.kulkarni@saserve.com",
                category = ServiceCategory.MECHANIC,
                rating = 4.90f,
                reviewCount = 134,
                experienceYears = 7,
                hourlyRate = 349.0,
                bio = "Automobile diagnostics engineer. OBD scanning, hybrid/EV electrical systems, battery health checks & brake servicing.",
                location = "Saket (2.8 km away)",
                distanceMiles = 1.7,
                isAvailable = true,
                isVerified = true,
                skills = listOf("OBD Diagnostics", "Battery Testing", "Brake Overhaul"),
                reviews = listOf(
                    Review("rev_24", "Arjun Bhatia", 5.0f, "Diagnosed engine check light within 10 minutes. Extremely knowledgeable.", "4 days ago")
                )
            ),
            ServiceProvider(
                id = "prov_app_4",
                userId = "user_app_4",
                name = "Sunita Rao",
                phone = "+91 98654 90125",
                email = "sunita.rao@saserve.com",
                category = ServiceCategory.APPLIANCE_REPAIR,
                rating = 4.94f,
                reviewCount = 195,
                experienceYears = 9,
                hourlyRate = 399.0,
                bio = "Certified HVAC & washing machine specialist. Inverter PCB board repairs, front-load drum bearing fixes & gas charging.",
                location = "Pitampura (3.0 km away)",
                distanceMiles = 1.9,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Inverter PCB Repair", "Washing Machine Drums", "AC Gas Refill"),
                reviews = listOf(
                    Review("rev_25", "Swati Nanda", 5.0f, "Fixed my Samsung front-load error code quickly. Transparent and honest pricing.", "2 days ago")
                )
            ),

            // MASON SPECIALISTS (Male & Female)
            ServiceProvider(
                id = "prov_mason_1",
                userId = "user_mason_1",
                name = "Harish Rawat",
                phone = "+91 98101 23456",
                email = "harish.rawat@saserve.com",
                category = ServiceCategory.MASON,
                rating = 4.92f,
                reviewCount = 210,
                experienceYears = 14,
                hourlyRate = 449.0,
                bio = "Senior Master Mason. Specialized in boundary walls, RCC beam casting, plaster repair & heavy stone masonry.",
                location = "Karol Bagh (1.8 km away)",
                distanceMiles = 1.1,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Brickwork", "RCC Concrete", "Wall Plastering", "Boundary Walls"),
                reviews = listOf(
                    Review("rev_m1", "Satish Goel", 5.0f, "Built our courtyard boundary wall with laser-straight alignment.", "3 days ago"),
                    Review("rev_m2", "Kamal Nain", 4.8f, "Excellent plaster finish, seamless crack repairs.", "1 week ago")
                )
            ),
            ServiceProvider(
                id = "prov_mason_2",
                userId = "user_mason_2",
                name = "Geeta Yadav",
                phone = "+91 98202 34567",
                email = "geeta.yadav@saserve.com",
                category = ServiceCategory.MASON,
                rating = 4.89f,
                reviewCount = 135,
                experienceYears = 8,
                hourlyRate = 399.0,
                bio = "Tile & flooring specialist. Vitrified floor tiles, waterproof epoxy grouting, bathroom wall tiles & countertop granite fixing.",
                location = "Palam (2.5 km away)",
                distanceMiles = 1.6,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Tile Laying", "Epoxy Grout", "Granite Counters", "Surface Leveling"),
                reviews = listOf(
                    Review("rev_m3", "Rekha Sen", 5.0f, "Replaced broken bathroom tiles perfectly without damaging adjacent ones.", "2 days ago")
                )
            ),
            ServiceProvider(
                id = "prov_mason_3",
                userId = "user_mason_3",
                name = "Dinesh Prajapati",
                phone = "+91 98303 45678",
                email = "dinesh.prajapati@saserve.com",
                category = ServiceCategory.MASON,
                rating = 4.84f,
                reviewCount = 88,
                experienceYears = 10,
                hourlyRate = 349.0,
                bio = "Core hole drilling, concrete chipping, AC duct coring & damp wall brick repair.",
                location = "Janakpuri (3.4 km away)",
                distanceMiles = 2.1,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Core Drilling", "Concrete Chipping", "Damp Proofing"),
                reviews = listOf(
                    Review("rev_m4", "Vivek Anand", 4.8f, "Drilled 4-inch core hole for kitchen chimney exhaust very cleanly.", "5 days ago")
                )
            ),

            // GARDENER SPECIALISTS (Male & Female)
            ServiceProvider(
                id = "prov_gard_1",
                userId = "user_gard_1",
                name = "Manoj Saini",
                phone = "+91 98404 56789",
                email = "manoj.saini@saserve.com",
                category = ServiceCategory.GARDENER,
                rating = 4.94f,
                reviewCount = 260,
                experienceYears = 11,
                hourlyRate = 299.0,
                bio = "Senior Horticulturalist & Lawn Expert. Rotary lawn mowing, turf leveling, weed eradication & seasonal flowering care.",
                location = "Vasant Vihar (1.4 km away)",
                distanceMiles = 0.9,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Lawn Mowing", "Turf Care", "Organic Fertilizers", "Grass Dethatching"),
                reviews = listOf(
                    Review("rev_g1", "Nalini Krishnan", 5.0f, "Transformed our yellow lawn into lush green carpet within two visits!", "2 days ago"),
                    Review("rev_g2", "Pradeep Chawla", 4.9f, "Prompt, polite and brought all professional mowing gear.", "1 week ago")
                )
            ),
            ServiceProvider(
                id = "prov_gard_2",
                userId = "user_gard_2",
                name = "Kavita Nair",
                phone = "+91 98505 67890",
                email = "kavita.nair@saserve.com",
                category = ServiceCategory.GARDENER,
                rating = 4.91f,
                reviewCount = 175,
                experienceYears = 7,
                hourlyRate = 349.0,
                bio = "Balcony & terrace garden designer. Micro-drip irrigation installation, vertical planters, organic pest treatment & exotic plants potting.",
                location = "New Friends Colony (2.1 km away)",
                distanceMiles = 1.3,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Balcony Gardens", "Drip Irrigation", "Plant Potting", "Neem Pest Spray"),
                reviews = listOf(
                    Review("rev_g3", "Anjali Deshmukh", 5.0f, "Set up a gorgeous vertical balcony herbal garden. Great guidance on plant care.", "3 days ago")
                )
            ),
            ServiceProvider(
                id = "prov_gard_3",
                userId = "user_gard_3",
                name = "Raju Paswan",
                phone = "+91 98606 78901",
                email = "raju.paswan@saserve.com",
                category = ServiceCategory.GARDENER,
                rating = 4.82f,
                reviewCount = 94,
                experienceYears = 9,
                hourlyRate = 249.0,
                bio = "Hedge shearing, tree branch thinning, soil vermicompost conditioning & garden cleanup.",
                location = "Sarita Vihar (3.6 km away)",
                distanceMiles = 2.2,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Hedge Trimming", "Pruning", "Soil Vermicompost"),
                reviews = listOf(
                    Review("rev_g4", "Girish Sethi", 4.8f, "Pruned large bougainvillea and trimmed all hedges neatly.", "6 days ago")
                )
            ),

            // HOUSE CLEANING SPECIALISTS (Male & Female)
            ServiceProvider(
                id = "prov_clean_1",
                userId = "user_clean_1",
                name = "Pooja Sharma",
                phone = "+91 98707 89012",
                email = "pooja.sharma@saserve.com",
                category = ServiceCategory.HOUSE_CLEANING,
                rating = 4.96f,
                reviewCount = 310,
                experienceYears = 8,
                hourlyRate = 499.0,
                bio = "Certified Home Deep Cleaning Lead. Complete 2BHK/3BHK sanitization, industrial HEPA vacuuming & anti-allergen bedroom treatment.",
                location = "Golf Course Road (1.2 km away)",
                distanceMiles = 0.7,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Deep Home Cleaning", "HEPA Vacuuming", "Bathroom Sanitization", "Eco Chemicals"),
                reviews = listOf(
                    Review("rev_c1", "Rohit Aggarwal", 5.0f, "The house looks brand new! Every corner and ceiling was spotless.", "1 day ago"),
                    Review("rev_c2", "Sangeeta Rao", 5.0f, "Very courteous team and top-grade hospital-safe sanitizers used.", "4 days ago")
                )
            ),
            ServiceProvider(
                id = "prov_clean_2",
                userId = "user_clean_2",
                name = "Deepak Malviya",
                phone = "+91 98808 90123",
                email = "deepak.malviya@saserve.com",
                category = ServiceCategory.HOUSE_CLEANING,
                rating = 4.88f,
                reviewCount = 180,
                experienceYears = 6,
                hourlyRate = 399.0,
                bio = "Single-disc machine floor buffing, marble polishing, sofa injection-extraction shampooing & stain removal.",
                location = "Sushant Lok (2.3 km away)",
                distanceMiles = 1.4,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Machine Floor Buffing", "Sofa Shampooing", "Carpet Cleaning"),
                reviews = listOf(
                    Review("rev_c3", "Manish Mathur", 4.9f, "Removed year-old coffee stains from our 7-seater sofa. Smells fresh!", "3 days ago")
                )
            ),
            ServiceProvider(
                id = "prov_clean_3",
                userId = "user_clean_3",
                name = "Meena Kumari",
                phone = "+91 98909 01234",
                email = "meena.kumari@saserve.com",
                category = ServiceCategory.HOUSE_CLEANING,
                rating = 4.93f,
                reviewCount = 225,
                experienceYears = 9,
                hourlyRate = 349.0,
                bio = "Kitchen degreasing & bathroom tile descaling expert. Chimney mesh oil bath, hard-water lime removal & glass sparkle wash.",
                location = "Cyber City (2.7 km away)",
                distanceMiles = 1.7,
                isAvailable = true,
                isVerified = true,
                skills = listOf("Kitchen Degreasing", "Limescale Descaling", "Glass Windows"),
                reviews = listOf(
                    Review("rev_c4", "Alka Verma", 5.0f, "Removed tough hard-water deposits from shower glass doors. Highly satisfied!", "5 days ago")
                )
            )
        )
    }

    companion object {
        private const val KEY_CUSTOM_PROVIDERS = "key_custom_providers_indian_v4"
        private const val KEY_CUSTOM_BOOKINGS = "key_custom_bookings_v4"
        private const val KEY_NOTIFICATIONS = "key_notifications_list"
        private const val KEY_CURRENT_USER = "key_current_user"
        private const val KEY_REGISTERED_USERS = "key_registered_users_v2"
        private const val KEY_LOGGED_IN_PHONE = "key_logged_in_phone_v2"
        private const val KEY_DISMISSED_HOME_BOOKINGS = "key_dismissed_home_bookings_v1"
        private const val KEY_WALLET_BALANCE = "key_wallet_balance_v1"
        private const val KEY_WALLET_TRANSACTIONS = "key_wallet_transactions_v1"
        private const val KEY_THEME_MODE = "key_theme_mode_v1"
        private const val KEY_PENDING_CANCELLATION_FEE = "key_pending_cancellation_fee_v1"

        @Volatile
        private var INSTANCE: ServiceSyncRepository? = null

        fun getInstance(context: Context): ServiceSyncRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ServiceSyncRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
