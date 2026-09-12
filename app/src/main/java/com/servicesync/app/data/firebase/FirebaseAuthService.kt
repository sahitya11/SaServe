package com.servicesync.app.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.servicesync.app.data.model.User
import com.servicesync.app.data.model.UserRole
import kotlinx.coroutines.tasks.await

/**
 * Service managing Firebase Authentication and Cloud Firestore user profile synchronization.
 * Stores user profiles under the 'users' collection with document ID matching the user's Auth UID.
 */
class FirebaseAuthService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Registers a new user with Firebase Authentication (email & password).
     * Upon successful registration, writes the profile data to Cloud Firestore document:
     * 'users/{uid}'
     */
    suspend fun registerUser(
        username: String,
        email: String,
        password: String,
        phone: String = ""
    ): Result<User> {
        return try {
            val cleanEmail = email.trim()
            val cleanUsername = username.trim()
            val cleanPhone = phone.trim()

            // 1. Create user in Firebase Authentication
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val firebaseUser: FirebaseUser = authResult.user
                ?: return Result.failure(Exception("Registration succeeded but user profile was null."))

            val uid = firebaseUser.uid

            // 2. Update Firebase user display name
            try {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanUsername)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
            } catch (e: Exception) {
                // Non-fatal, proceed to Firestore save
            }

            // 3. Save profile data to Cloud Firestore inside collection 'users' using uid as document ID
            val userProfileData = hashMapOf<String, Any>(
                "uid" to uid,
                "username" to cleanUsername,
                "email" to cleanEmail,
                "phone" to cleanPhone,
                "role" to "CUSTOMER",
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )

            firestore.collection("users")
                .document(uid)
                .set(userProfileData, SetOptions.merge())
                .await()

            val appUser = User(
                id = uid,
                name = cleanUsername,
                email = cleanEmail,
                phone = cleanPhone,
                role = UserRole.CUSTOMER
            )

            Result.success(appUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logs in an existing user with Firebase Authentication.
     * Fetches their profile from Cloud Firestore 'users/{uid}'.
     */
    suspend fun loginUser(
        email: String,
        password: String
    ): Result<User> {
        return try {
            val cleanEmail = email.trim()

            // 1. Sign in with Firebase Authentication
            val authResult = auth.signInWithEmailAndPassword(cleanEmail, password).await()
            val firebaseUser: FirebaseUser = authResult.user
                ?: return Result.failure(Exception("Login succeeded but user profile was null."))

            val uid = firebaseUser.uid

            // 2. Retrieve user document from Cloud Firestore 'users/{uid}'
            val docSnapshot = try {
                firestore.collection("users").document(uid).get().await()
            } catch (e: Exception) {
                null
            }

            val username = docSnapshot?.getString("username")
                ?.ifBlank { firebaseUser.displayName }
                ?: (firebaseUser.displayName ?: cleanEmail.substringBefore("@"))
            val phone = docSnapshot?.getString("phone") ?: (firebaseUser.phoneNumber ?: "")
            val emailFromDoc = docSnapshot?.getString("email") ?: (firebaseUser.email ?: cleanEmail)
            val roleStr = docSnapshot?.getString("role") ?: "CUSTOMER"
            val role = if (roleStr == "PROVIDER") UserRole.PROVIDER else UserRole.CUSTOMER

            val appUser = User(
                id = uid,
                name = username,
                email = emailFromDoc,
                phone = phone,
                role = role
            )

            Result.success(appUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Returns the currently authenticated Firebase User, if any.
     */
    fun getCurrentFirebaseUser(): FirebaseUser? = auth.currentUser

    /**
     * Checks if a user is currently signed in.
     */
    fun isUserSignedIn(): Boolean = auth.currentUser != null

    /**
     * Signs out the current Firebase user.
     */
    fun signOut() {
        auth.signOut()
    }
}
