package com.servicesync.app.data.firebase

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.servicesync.app.data.model.*
import kotlinx.coroutines.tasks.await

/**
 * Service managing synchronization of SaServe application data with Cloud Firestore.
 * Handles collections: 'providers', 'bookings', 'feedback', and 'users'.
 */
class FirebaseSyncService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "FirebaseSyncService"
        const val COLLECTION_PROVIDERS = "providers"
        const val COLLECTION_BOOKINGS = "bookings"
        const val COLLECTION_FEEDBACK = "feedback"
        const val COLLECTION_USERS = "users"
    }

    // ----------------------------------------------------------------
    // SERVICE PROVIDERS SYNC
    // ----------------------------------------------------------------

    suspend fun syncProvider(provider: ServiceProvider): Result<Unit> {
        return try {
            val data = providerToMap(provider)
            firestore.collection(COLLECTION_PROVIDERS)
                .document(provider.id)
                .set(data, SetOptions.merge())
                .await()
            Log.d(TAG, "Provider synced to Firestore: ${provider.id} (${provider.name})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync provider: ${provider.id}", e)
            Result.failure(e)
        }
    }

    suspend fun syncAllProviders(providers: List<ServiceProvider>): Result<Int> {
        return try {
            if (providers.isEmpty()) return Result.success(0)
            val batch = firestore.batch()
            for (provider in providers) {
                val docRef = firestore.collection(COLLECTION_PROVIDERS).document(provider.id)
                batch.set(docRef, providerToMap(provider), SetOptions.merge())
            }
            batch.commit().await()
            Log.d(TAG, "All ${providers.size} providers successfully batch-synced to Firestore.")
            Result.success(providers.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error during bulk provider sync", e)
            Result.failure(e)
        }
    }

    suspend fun fetchProviders(): Result<List<ServiceProvider>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_PROVIDERS).get().await()
            val providers = snapshot.documents.mapNotNull { doc -> documentToProvider(doc) }
            Log.d(TAG, "Fetched ${providers.size} providers from Firestore.")
            Result.success(providers)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching providers from Firestore", e)
            Result.failure(e)
        }
    }

    fun listenToProviders(onUpdate: (List<ServiceProvider>) -> Unit): ListenerRegistration {
        return firestore.collection(COLLECTION_PROVIDERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to providers", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val providers = snapshot.documents.mapNotNull { doc -> documentToProvider(doc) }
                    onUpdate(providers)
                }
            }
    }

    // ----------------------------------------------------------------
    // BOOKINGS SYNC
    // ----------------------------------------------------------------

    suspend fun syncBooking(booking: Booking): Result<Unit> {
        return try {
            val data = bookingToMap(booking)
            firestore.collection(COLLECTION_BOOKINGS)
                .document(booking.id)
                .set(data, SetOptions.merge())
                .await()
            Log.d(TAG, "Booking synced to Firestore: ${booking.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync booking: ${booking.id}", e)
            Result.failure(e)
        }
    }

    suspend fun syncAllBookings(bookings: List<Booking>): Result<Int> {
        return try {
            if (bookings.isEmpty()) return Result.success(0)
            val batch = firestore.batch()
            for (booking in bookings) {
                val docRef = firestore.collection(COLLECTION_BOOKINGS).document(booking.id)
                batch.set(docRef, bookingToMap(booking), SetOptions.merge())
            }
            batch.commit().await()
            Log.d(TAG, "All ${bookings.size} bookings successfully batch-synced to Firestore.")
            Result.success(bookings.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error during bulk booking sync", e)
            Result.failure(e)
        }
    }

    suspend fun fetchBookings(): Result<List<Booking>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_BOOKINGS).get().await()
            val bookings = snapshot.documents.mapNotNull { doc -> documentToBooking(doc) }
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToBookings(onUpdate: (List<Booking>) -> Unit): ListenerRegistration {
        return firestore.collection(COLLECTION_BOOKINGS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to bookings", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val bookings = snapshot.documents.mapNotNull { doc -> documentToBooking(doc) }
                    onUpdate(bookings)
                }
            }
    }

    // ----------------------------------------------------------------
    // FEEDBACK SYNC
    // ----------------------------------------------------------------

    suspend fun syncFeedback(feedback: AppFeedback): Result<Unit> {
        return try {
            val data = feedbackToMap(feedback)
            firestore.collection(COLLECTION_FEEDBACK)
                .document(feedback.id)
                .set(data, SetOptions.merge())
                .await()
            Log.d(TAG, "Feedback synced to Firestore: ${feedback.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync feedback: ${feedback.id}", e)
            Result.failure(e)
        }
    }

    suspend fun syncAllFeedback(feedbackList: List<AppFeedback>): Result<Int> {
        return try {
            if (feedbackList.isEmpty()) return Result.success(0)
            val batch = firestore.batch()
            for (fb in feedbackList) {
                val docRef = firestore.collection(COLLECTION_FEEDBACK).document(fb.id)
                batch.set(docRef, feedbackToMap(fb), SetOptions.merge())
            }
            batch.commit().await()
            Log.d(TAG, "All ${feedbackList.size} feedback items batch-synced to Firestore.")
            Result.success(feedbackList.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error during bulk feedback sync", e)
            Result.failure(e)
        }
    }

    // ----------------------------------------------------------------
    // USERS SYNC
    // ----------------------------------------------------------------

    suspend fun syncUser(user: User): Result<Unit> {
        return try {
            val data = userToMap(user)
            firestore.collection(COLLECTION_USERS)
                .document(user.id)
                .set(data, SetOptions.merge())
                .await()
            Log.d(TAG, "User synced to Firestore: ${user.id} (${user.name})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync user: ${user.id}", e)
            Result.failure(e)
        }
    }

    suspend fun syncAllUsers(users: List<User>): Result<Int> {
        return try {
            if (users.isEmpty()) return Result.success(0)
            val batch = firestore.batch()
            for (user in users) {
                val docRef = firestore.collection(COLLECTION_USERS).document(user.id)
                batch.set(docRef, userToMap(user), SetOptions.merge())
            }
            batch.commit().await()
            Log.d(TAG, "All ${users.size} users batch-synced to Firestore.")
            Result.success(users.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error during bulk user sync", e)
            Result.failure(e)
        }
    }

    // ----------------------------------------------------------------
    // COMPREHENSIVE INITIAL DATA SYNC
    // ----------------------------------------------------------------

    suspend fun syncAllInitialData(
        providers: List<ServiceProvider>,
        bookings: List<Booking>,
        feedback: List<AppFeedback>,
        users: List<User>
    ): Map<String, Int> {
        val results = mutableMapOf<String, Int>()
        syncAllProviders(providers).onSuccess { results["providers"] = it }
        syncAllBookings(bookings).onSuccess { results["bookings"] = it }
        syncAllFeedback(feedback).onSuccess { results["feedback"] = it }
        syncAllUsers(users).onSuccess { results["users"] = it }
        return results
    }

    // ----------------------------------------------------------------
    // SERIALIZERS & MAPPERS
    // ----------------------------------------------------------------

    private fun providerToMap(p: ServiceProvider): Map<String, Any> {
        return hashMapOf(
            "id" to p.id,
            "userId" to p.userId,
            "name" to p.name,
            "phone" to p.phone,
            "email" to p.email,
            "category" to p.category.name,
            "categoryDisplayName" to p.category.displayName,
            "rating" to p.rating.toDouble(),
            "reviewCount" to p.reviewCount,
            "experienceYears" to p.experienceYears,
            "hourlyRate" to p.hourlyRate,
            "bio" to p.bio,
            "location" to p.location,
            "distanceMiles" to p.distanceMiles,
            "isAvailable" to p.isAvailable,
            "isVerified" to p.isVerified,
            "skills" to p.skills,
            "availableSlots" to p.availableSlots.map { slot ->
                mapOf("id" to slot.id, "timeRange" to slot.timeRange, "isAvailable" to slot.isAvailable)
            },
            "reviews" to p.reviews.map { rev ->
                mapOf(
                    "id" to rev.id,
                    "author" to rev.author,
                    "rating" to rev.rating.toDouble(),
                    "comment" to rev.comment,
                    "date" to rev.date
                )
            },
            "updatedAt" to Timestamp.now()
        )
    }

    private fun documentToProvider(doc: DocumentSnapshot): ServiceProvider? {
        return try {
            val id = doc.getString("id") ?: doc.id
            val userId = doc.getString("userId") ?: ("user_" + id)
            val name = doc.getString("name") ?: return null
            val phone = doc.getString("phone") ?: ""
            val email = doc.getString("email") ?: ""
            val categoryStr = doc.getString("category") ?: "ELECTRICIAN"
            val category = try { ServiceCategory.valueOf(categoryStr) } catch (e: Exception) { ServiceCategory.ELECTRICIAN }
            val rating = (doc.getDouble("rating") ?: 5.0).toFloat()
            val reviewCount = (doc.getLong("reviewCount") ?: 1L).toInt()
            val experienceYears = (doc.getLong("experienceYears") ?: 5L).toInt()
            val hourlyRate = doc.getDouble("hourlyRate") ?: 299.0
            val bio = doc.getString("bio") ?: ""
            val location = doc.getString("location") ?: "Local Area"
            val distanceMiles = doc.getDouble("distanceMiles") ?: 1.0
            val isAvailable = doc.getBoolean("isAvailable") ?: true
            val isVerified = doc.getBoolean("isVerified") ?: true
            val skills = (doc.get("skills") as? List<*>)?.mapNotNull { it?.toString() }
                ?: listOf("Installation", "Maintenance")

            val slotsRaw = doc.get("availableSlots") as? List<*>
            val availableSlots = slotsRaw?.mapNotNull { item ->
                if (item is Map<*, *>) {
                    TimeSlot(
                        id = item["id"]?.toString() ?: "slot",
                        timeRange = item["timeRange"]?.toString() ?: "11:00 AM - 01:00 PM",
                        isAvailable = (item["isAvailable"] as? Boolean) ?: true
                    )
                } else null
            } ?: listOf(
                TimeSlot("slot_1", "09:00 AM - 11:00 AM", true),
                TimeSlot("slot_2", "11:00 AM - 01:00 PM", true),
                TimeSlot("slot_3", "02:00 PM - 04:00 PM", true),
                TimeSlot("slot_4", "04:00 PM - 06:00 PM", true)
            )

            val reviewsRaw = doc.get("reviews") as? List<*>
            val reviews = reviewsRaw?.mapNotNull { item ->
                if (item is Map<*, *>) {
                    Review(
                        id = item["id"]?.toString() ?: "rev",
                        author = item["author"]?.toString() ?: "Customer",
                        rating = ((item["rating"] as? Number)?.toFloat()) ?: 5.0f,
                        comment = item["comment"]?.toString() ?: "Great service!",
                        date = item["date"]?.toString() ?: "Recently"
                    )
                } else null
            } ?: emptyList()

            ServiceProvider(
                id = id,
                userId = userId,
                name = name,
                phone = phone,
                email = email,
                category = category,
                rating = rating,
                reviewCount = reviewCount,
                experienceYears = experienceYears,
                hourlyRate = hourlyRate,
                bio = bio,
                location = location,
                distanceMiles = distanceMiles,
                isAvailable = isAvailable,
                isVerified = isVerified,
                skills = skills,
                availableSlots = availableSlots,
                reviews = reviews
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun bookingToMap(b: Booking): Map<String, Any> {
        val map = hashMapOf<String, Any>(
            "id" to b.id,
            "customerId" to b.customerId,
            "customerName" to b.customerName,
            "customerPhone" to b.customerPhone,
            "customerAddress" to b.customerAddress,
            "providerId" to b.providerId,
            "providerName" to b.providerName,
            "providerPhone" to b.providerPhone,
            "category" to b.category.name,
            "scheduledDate" to b.scheduledDate,
            "scheduledSlot" to b.scheduledSlot,
            "issueDescription" to b.issueDescription,
            "status" to b.status.name,
            "createdAt" to b.createdAt,
            "hourlyRate" to b.hourlyRate,
            "estimatedHours" to b.estimatedHours,
            "startOtp" to b.startOtp,
            "completionOtp" to b.completionOtp,
            "cancellationFee" to b.cancellationFee,
            "totalAmount" to b.totalAmount,
            "updatedAt" to Timestamp.now()
        )
        b.customerRating?.let { map["customerRating"] = it.toDouble() }
        b.customerReview?.let { map["customerReview"] = it }
        b.cancellationReason?.let { map["cancellationReason"] = it }
        return map
    }

    private fun documentToBooking(doc: DocumentSnapshot): Booking? {
        return try {
            val id = doc.getString("id") ?: doc.id
            val categoryStr = doc.getString("category") ?: "ELECTRICIAN"
            val category = try { ServiceCategory.valueOf(categoryStr) } catch (e: Exception) { ServiceCategory.ELECTRICIAN }
            val statusStr = doc.getString("status") ?: "PENDING"
            val status = try { BookingStatus.valueOf(statusStr) } catch (e: Exception) { BookingStatus.PENDING }

            Booking(
                id = id,
                customerId = doc.getString("customerId") ?: "",
                customerName = doc.getString("customerName") ?: "",
                customerPhone = doc.getString("customerPhone") ?: "",
                customerAddress = doc.getString("customerAddress") ?: "",
                providerId = doc.getString("providerId") ?: "",
                providerName = doc.getString("providerName") ?: "",
                providerPhone = doc.getString("providerPhone") ?: "",
                category = category,
                scheduledDate = doc.getString("scheduledDate") ?: "",
                scheduledSlot = doc.getString("scheduledSlot") ?: "",
                issueDescription = doc.getString("issueDescription") ?: "",
                status = status,
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                hourlyRate = doc.getDouble("hourlyRate") ?: 0.0,
                estimatedHours = (doc.getLong("estimatedHours") ?: 1L).toInt(),
                startOtp = doc.getString("startOtp") ?: "1234",
                completionOtp = doc.getString("completionOtp") ?: "5678",
                customerRating = doc.getDouble("customerRating")?.toFloat(),
                customerReview = doc.getString("customerReview"),
                cancellationReason = doc.getString("cancellationReason"),
                cancellationFee = doc.getDouble("cancellationFee") ?: 0.0
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun feedbackToMap(f: AppFeedback): Map<String, Any> {
        return hashMapOf(
            "id" to f.id,
            "userId" to f.userId,
            "userName" to f.userName,
            "userPhone" to f.userPhone,
            "issueCategory" to f.issueCategory,
            "rating" to f.rating,
            "suggestions" to f.suggestions,
            "timestamp" to f.timestamp,
            "updatedAt" to Timestamp.now()
        )
    }

    private fun userToMap(u: User): Map<String, Any> {
        val map = hashMapOf<String, Any>(
            "id" to u.id,
            "name" to u.name,
            "email" to u.email,
            "phone" to u.phone,
            "role" to u.role.name,
            "address" to u.address,
            "updatedAt" to Timestamp.now()
        )
        u.savedAddresses?.let { addresses ->
            map["savedAddresses"] = addresses.map { addr ->
                mapOf(
                    "id" to addr.id,
                    "label" to addr.label,
                    "addressLine" to addr.addressLine,
                    "flatHouseNo" to addr.flatHouseNo,
                    "streetArea" to addr.streetArea,
                    "landmark" to addr.landmark,
                    "reachInstructions" to addr.reachInstructions,
                    "isDefault" to addr.isDefault
                )
            }
        }
        return map
    }
}
