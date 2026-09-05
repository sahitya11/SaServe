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
        // 1. Providers
        val savedProvidersJson = prefs.getString(KEY_PROVIDERS, null)
        if (!savedProvidersJson.isNullOrEmpty()) {
            val type = object : TypeToken<List<ServiceProvider>>() {}.type
            _providers.value = gson.fromJson(savedProvidersJson, type)
        } else {
            val initialProviders = getSeedProviders()
            _providers.value = initialProviders
            saveProviders(initialProviders)
        }

        // 2. Bookings
        val savedBookingsJson = prefs.getString(KEY_BOOKINGS, null)
        if (!savedBookingsJson.isNullOrEmpty()) {
            val type = object : TypeToken<List<Booking>>() {}.type
            _bookings.value = gson.fromJson(savedBookingsJson, type)
        } else {
            val initialBookings = getSeedBookings()
            _bookings.value = initialBookings
            saveBookings(initialBookings)
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
                    title = "Welcome to ServiceSync! 👋",
                    message = "Find top-rated local electricians, plumbers, carpenters, and mechanics ready to assist you.",
                    timestamp = System.currentTimeMillis() - 3600000,
                    isRead = false
                )
            )
            _notifications.value = initialNotifs
            saveNotifications(initialNotifs)
        }

        // 4. Current User
        val savedUserJson = prefs.getString(KEY_CURRENT_USER, null)
        if (!savedUserJson.isNullOrEmpty()) {
            _currentUser.value = gson.fromJson(savedUserJson, User::class.java)
        } else {
            // Default active profile: Customer Sarah
            val defaultCustomer = User(
                id = "cust_sarah",
                name = "Sarah Jenkins",
                email = "sarah.jenkins@example.com",
                phone = "+1 (555) 234-5678",
                role = UserRole.CUSTOMER,
                address = "742 Evergreen Terrace, Springfield"
            )
            _currentUser.value = defaultCustomer
            saveUser(defaultCustomer)
        }
    }

    private fun saveProviders(list: List<ServiceProvider>) {
        prefs.edit().putString(KEY_PROVIDERS, gson.toJson(list)).apply()
    }

    private fun saveBookings(list: List<Booking>) {
        prefs.edit().putString(KEY_BOOKINGS, gson.toJson(list)).apply()
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

    // Role switching helper for quick demo/testing
    fun switchUserRole(targetRole: UserRole) {
        val current = _currentUser.value
        if (current != null && current.role == targetRole) return

        if (targetRole == UserRole.PROVIDER) {
            // Switch to Marcus Vance (Plumber) or registered provider
            val defaultProvider = _providers.value.firstOrNull { it.id == "prov_marcus" }
                ?: _providers.value.firstOrNull()

            val providerUser = User(
                id = defaultProvider?.userId ?: "prov_user_marcus",
                name = defaultProvider?.name ?: "Marcus Vance",
                email = defaultProvider?.email ?: "marcus.plumbing@example.com",
                phone = defaultProvider?.phone ?: "+1 (555) 876-5432",
                role = UserRole.PROVIDER,
                address = "104 Baker Street, Industrial District",
                providerId = defaultProvider?.id ?: "prov_marcus"
            )
            _currentUser.value = providerUser
            saveUser(providerUser)
        } else {
            // Switch to Customer Sarah
            val customerUser = User(
                id = "cust_sarah",
                name = "Sarah Jenkins",
                email = "sarah.jenkins@example.com",
                phone = "+1 (555) 234-5678",
                role = UserRole.CUSTOMER,
                address = "742 Evergreen Terrace, Springfield"
            )
            _currentUser.value = customerUser
            saveUser(customerUser)
        }
    }

    fun registerCustomer(name: String, email: String, phone: String, address: String): User {
        val newUser = User(
            id = "cust_" + UUID.randomUUID().toString().take(8),
            name = name,
            email = email,
            phone = phone,
            role = UserRole.CUSTOMER,
            address = address
        )
        _currentUser.value = newUser
        saveUser(newUser)
        return newUser
    }

    fun registerProvider(
        name: String,
        email: String,
        phone: String,
        category: ServiceCategory,
        hourlyRate: Double,
        experienceYears: Int,
        bio: String,
        address: String = "Local Metro Area"
    ): ServiceProvider {
        val userId = "user_" + UUID.randomUUID().toString().take(8)
        val providerId = "prov_" + UUID.randomUUID().toString().take(8)

        val defaultSlots = listOf(
            TimeSlot("slot_1", "09:00 AM - 11:00 AM", true),
            TimeSlot("slot_2", "11:00 AM - 01:00 PM", true),
            TimeSlot("slot_3", "02:00 PM - 04:00 PM", true),
            TimeSlot("slot_4", "04:00 PM - 06:00 PM", true)
        )

        val newProvider = ServiceProvider(
            id = providerId,
            userId = userId,
            name = name,
            phone = phone,
            email = email,
            category = category,
            rating = 5.0f,
            reviewCount = 1,
            experienceYears = experienceYears,
            hourlyRate = hourlyRate,
            bio = bio,
            location = address,
            distanceMiles = 1.8,
            isAvailable = true,
            isVerified = true,
            skills = listOf("Residential ${category.displayName}", "Emergency Services", "Installations"),
            availableSlots = defaultSlots,
            reviews = listOf(
                Review(
                    id = "rev_welcome",
                    author = "ServiceSync Team",
                    rating = 5.0f,
                    comment = "Verified and licensed professional on ServiceSync.",
                    date = "Today"
                )
            )
        )

        val updatedProviders = listOf(newProvider) + _providers.value
        _providers.value = updatedProviders
        saveProviders(updatedProviders)

        val providerUser = User(
            id = userId,
            name = name,
            email = email,
            phone = phone,
            role = UserRole.PROVIDER,
            address = address,
            providerId = providerId
        )
        _currentUser.value = providerUser
        saveUser(providerUser)

        return newProvider
    }

    fun createBooking(
        provider: ServiceProvider,
        date: String,
        timeSlot: String,
        address: String,
        issueDescription: String
    ): Booking {
        val user = _currentUser.value
        val booking = Booking(
            id = "bk_" + UUID.randomUUID().toString().take(8),
            customerId = user?.id ?: "cust_sarah",
            customerName = user?.name ?: "Sarah Jenkins",
            customerPhone = user?.phone ?: "+1 (555) 234-5678",
            customerAddress = if (address.isNotBlank()) address else (user?.address ?: "742 Evergreen Terrace"),
            providerId = provider.id,
            providerName = provider.name,
            providerPhone = provider.phone,
            category = provider.category,
            scheduledDate = date,
            scheduledSlot = timeSlot,
            issueDescription = issueDescription,
            status = BookingStatus.PENDING,
            hourlyRate = provider.hourlyRate
        )

        val updatedBookings = listOf(booking) + _bookings.value
        _bookings.value = updatedBookings
        saveBookings(updatedBookings)

        // Trigger notification for provider
        NotificationHelper.sendNewBookingRequestNotification(context, booking)

        // In-app notification for customer confirming request creation
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
     * Provider accepts customer's booking request:
     * 1. Status changes to ACCEPTED
     * 2. Customer gets high-priority Android System Notification
     * 3. Customer gets in-app notification entry
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

    fun declineBooking(bookingId: String): Boolean {
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

        val declineNotif = AppNotification(
            id = UUID.randomUUID().toString(),
            title = "Booking Declined",
            message = "${currentBooking.providerName} was unavailable for your slot on ${currentBooking.scheduledDate}. Please select another provider.",
            bookingId = currentBooking.id
        )
        val updatedNotifs = listOf(declineNotif) + _notifications.value
        _notifications.value = updatedNotifs
        saveNotifications(updatedNotifs)

        return true
    }

    fun updateBookingStatus(bookingId: String, newStatus: BookingStatus) {
        val currentList = _bookings.value
        val targetIndex = currentList.indexOfFirst { it.id == bookingId }
        if (targetIndex == -1) return

        val updatedList = currentList.toMutableList().apply {
            set(targetIndex, get(targetIndex).copy(status = newStatus))
        }
        _bookings.value = updatedList
        saveBookings(updatedList)
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

    // Seed Data
    private fun getSeedProviders(): List<ServiceProvider> {
        val standardSlots = listOf(
            TimeSlot("slot_1", "09:00 AM - 11:00 AM", true),
            TimeSlot("slot_2", "11:00 AM - 01:00 PM", true),
            TimeSlot("slot_3", "02:00 PM - 04:00 PM", true),
            TimeSlot("slot_4", "04:00 PM - 06:00 PM", true)
        )

        return listOf(
            // Electricians
            ServiceProvider(
                id = "prov_alex",
                userId = "user_alex",
                name = "Alex Turner",
                phone = "+1 (555) 301-4455",
                email = "alex.turner@servicesync.pro",
                category = ServiceCategory.ELECTRICIAN,
                rating = 4.95f,
                reviewCount = 142,
                experienceYears = 8,
                hourlyRate = 48.0,
                bio = "Master certified electrician specializing in domestic wiring, smart switches, circuit breakers, and EV charger setup.",
                location = "Downtown Metro (2.1 miles)",
                distanceMiles = 2.1,
                skills = listOf("Wiring", "Circuit Breakers", "Lighting", "EV Chargers", "Short Circuit Repair"),
                availableSlots = standardSlots,
                reviews = listOf(
                    Review("r1", "Michael S.", 5.0f, "Alex arrived within 20 minutes and fixed our short circuit issue quickly. Very professional!", "2 days ago"),
                    Review("r2", "Jessica P.", 5.0f, "Installed our chandelier and patio lighting cleanly. Fair pricing!", "1 week ago")
                )
            ),
            ServiceProvider(
                id = "prov_elena",
                userId = "user_elena",
                name = "Elena Ramos",
                phone = "+1 (555) 442-9988",
                email = "elena.ramos@servicesync.pro",
                category = ServiceCategory.ELECTRICIAN,
                rating = 4.88f,
                reviewCount = 98,
                experienceYears = 6,
                hourlyRate = 42.0,
                bio = "Licensed residential electrician. Dedicated to fast diagnostic checks, safe switchboard installations, and home safety audits.",
                location = "Oakridge Park (3.4 miles)",
                distanceMiles = 3.4,
                skills = listOf("Switchboard Upgrades", "Ceiling Fan", "Smart Home Wiring", "Inverter Hookup"),
                availableSlots = standardSlots,
                reviews = listOf(
                    Review("r3", "David K.", 5.0f, "Elena is brilliant, diagnosed our flickering lights in 10 mins.", "4 days ago")
                )
            ),

            // Plumbers
            ServiceProvider(
                id = "prov_marcus",
                userId = "user_marcus",
                name = "Marcus Vance",
                phone = "+1 (555) 876-5432",
                email = "marcus.plumbing@servicesync.pro",
                category = ServiceCategory.PLUMBER,
                rating = 4.92f,
                reviewCount = 186,
                experienceYears = 10,
                hourlyRate = 52.0,
                bio = "Senior plumbing contractor with a decade of expertise in pipe leak detection, water heater servicing, bathroom remodeling, and unclogging.",
                location = "Westfield Commons (1.5 miles)",
                distanceMiles = 1.5,
                skills = listOf("Pipe Leakages", "Water Heaters", "Drain Unclogging", "Faucet Replacement", "Toilet Repair"),
                availableSlots = standardSlots,
                reviews = listOf(
                    Review("r4", "Amanda B.", 5.0f, "Marcus solved an emergency pipe burst on a Sunday morning. Lifesaver!", "Yesterday"),
                    Review("r5", "Tom W.", 4.8f, "Replaced our old water heater cleanly. Highly recommended.", "3 days ago")
                )
            ),
            ServiceProvider(
                id = "prov_david",
                userId = "user_david",
                name = "David Chen",
                phone = "+1 (555) 765-4321",
                email = "david.plumber@servicesync.pro",
                category = ServiceCategory.PLUMBER,
                rating = 4.79f,
                reviewCount = 84,
                experienceYears = 5,
                hourlyRate = 39.0,
                bio = "Reliable neighborhood plumber for kitchen sink fixtures, shower valves, and water filtration system installs.",
                location = "Eastside Green (4.2 miles)",
                distanceMiles = 4.2,
                skills = listOf("Sink Fixtures", "Water Filters", "Shower Valves", "Drain Jetting"),
                availableSlots = standardSlots,
                reviews = listOf(
                    Review("r6", "Karen G.", 5.0f, "Fixed the kitchen sink sprayer instantly. Great attitude!", "5 days ago")
                )
            ),

            // Carpenters
            ServiceProvider(
                id = "prov_robert",
                userId = "user_robert",
                name = "Robert Miller",
                phone = "+1 (555) 654-9870",
                email = "robert.woodwork@servicesync.pro",
                category = ServiceCategory.CARPENTER,
                rating = 4.91f,
                reviewCount = 115,
                experienceYears = 9,
                hourlyRate = 49.0,
                bio = "Craftsman carpenter specialized in bespoke cabinetry, door alignment, furniture refurbishment, and timber decking.",
                location = "Cedar Hills (2.8 miles)",
                distanceMiles = 2.8,
                skills = listOf("Custom Cabinets", "Door Hinges & Framing", "Furniture Assembly", "Wood Polishing"),
                availableSlots = standardSlots,
                reviews = listOf(
                    Review("r7", "Lucas T.", 5.0f, "Repaired our antique dining table beautifully. True craftsman.", "6 days ago")
                )
            ),
            ServiceProvider(
                id = "prov_liam",
                userId = "user_liam",
                name = "Liam Wright",
                phone = "+1 (555) 321-7890",
                email = "liam.carpentry@servicesync.pro",
                category = ServiceCategory.CARPENTER,
                rating = 4.82f,
                reviewCount = 73,
                experienceYears = 6,
                hourlyRate = 44.0,
                bio = "Precision carpentry: wooden partitions, floating shelves, modular wardrobes, and lock fitting.",
                location = "Maple Ridge (3.7 miles)",
                distanceMiles = 3.7,
                skills = listOf("Floating Shelves", "Lock Installations", "Wardrobe Fitting", "Drywall Wood Repair"),
                availableSlots = standardSlots,
                reviews = listOf(
                    Review("r8", "Sophia M.", 5.0f, "Assembled our huge IKEA wardrobe in record time!", "1 week ago")
                )
            ),

            // Mechanics
            ServiceProvider(
                id = "prov_carlos",
                userId = "user_carlos",
                name = "Carlos Gomez",
                phone = "+1 (555) 998-1122",
                email = "carlos.mechanic@servicesync.pro",
                category = ServiceCategory.MECHANIC,
                rating = 4.96f,
                reviewCount = 210,
                experienceYears = 12,
                hourlyRate = 58.0,
                bio = "Certified mobile automotive specialist. On-site brake servicing, battery jump/replacement, OBD2 diagnostics, and tune-ups.",
                location = "Industrial Way (1.9 miles)",
                distanceMiles = 1.9,
                skills = listOf("OBD Diagnostics", "Brake Pad Replacement", "Battery Jumpstart", "Suspension Check", "Oil Change"),
                availableSlots = standardSlots,
                reviews = listOf(
                    Review("r9", "Chris H.", 5.0f, "Came to my garage when my car wouldn't start. Diagnosed dead alternator and fixed it right there.", "3 days ago")
                )
            ),
            ServiceProvider(
                id = "prov_samir",
                userId = "user_samir",
                name = "Samir Patel",
                phone = "+1 (555) 777-6655",
                email = "samir.auto@servicesync.pro",
                category = ServiceCategory.MECHANIC,
                rating = 4.84f,
                reviewCount = 92,
                experienceYears = 7,
                hourlyRate = 46.0,
                bio = "Mobile car mechanic for seasonal servicing, tire rotations, AC recharge, and engine health checks.",
                location = "Bayside Boulevard (4.0 miles)",
                distanceMiles = 4.0,
                skills = listOf("Car AC Servicing", "Tire Fitting", "Transmission Fluids", "Spark Plugs"),
                availableSlots = standardSlots,
                reviews = listOf(
                    Review("r10", "Rachel L.", 5.0f, "Got my car AC blowing ice-cold again in summer heat. Fair pricing!", "4 days ago")
                )
            ),

            // Appliance Repair
            ServiceProvider(
                id = "prov_vikram",
                userId = "user_vikram",
                name = "Vikram Sharma",
                phone = "+1 (555) 333-8899",
                email = "vikram.appliances@servicesync.pro",
                category = ServiceCategory.APPLIANCE_REPAIR,
                rating = 4.90f,
                reviewCount = 135,
                experienceYears = 8,
                hourlyRate = 45.0,
                bio = "Specialist in high-end refrigerators, washing machines, dishwashers, and microwave ovens.",
                location = "South End (3.1 miles)",
                distanceMiles = 3.1,
                skills = listOf("Refrigerator Cooling", "Washing Machine Motor", "Dishwasher Drain", "Oven Heating"),
                availableSlots = standardSlots,
                reviews = listOf(
                    Review("r11", "Brenda N.", 5.0f, "Fixed our LG washing machine drum leak quickly.", "2 days ago")
                )
            )
        )
    }

    private fun getSeedBookings(): List<Booking> {
        return listOf(
            Booking(
                id = "bk_seed_101",
                customerId = "cust_sarah",
                customerName = "Sarah Jenkins",
                customerPhone = "+1 (555) 234-5678",
                customerAddress = "742 Evergreen Terrace, Springfield",
                providerId = "prov_alex",
                providerName = "Alex Turner",
                providerPhone = "+1 (555) 301-4455",
                category = ServiceCategory.ELECTRICIAN,
                scheduledDate = "Tomorrow, Sep 6",
                scheduledSlot = "02:00 PM - 04:00 PM",
                issueDescription = "Tripping circuit breaker when kitchen microwave and oven are on.",
                status = BookingStatus.PENDING,
                hourlyRate = 48.0
            ),
            Booking(
                id = "bk_seed_102",
                customerId = "cust_sarah",
                customerName = "Sarah Jenkins",
                customerPhone = "+1 (555) 234-5678",
                customerAddress = "742 Evergreen Terrace, Springfield",
                providerId = "prov_marcus",
                providerName = "Marcus Vance",
                providerPhone = "+1 (555) 876-5432",
                category = ServiceCategory.PLUMBER,
                scheduledDate = "Today, Sep 5",
                scheduledSlot = "11:00 AM - 01:00 PM",
                issueDescription = "Bathroom sink faucet leaking droplets continuously.",
                status = BookingStatus.ACCEPTED,
                hourlyRate = 52.0
            )
        )
    }

    companion object {
        private const val KEY_PROVIDERS = "key_providers_list"
        private const val KEY_BOOKINGS = "key_bookings_list"
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
