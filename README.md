# SaServe - On-Demand Service Booking Android App

**SaServe** is a modern Android application built with Kotlin and Jetpack Compose (Material 3) for booking home and vehicle service specialists (Electrician, Plumber, Carpenter, Mechanic, Appliance Repair, Painter).

## ✨ Key Features

1. **Dual-Role Architecture**:
   - **Customer Mode**:
     - Browse services by category (Electrician, Plumber, Carpenter, Mechanic, etc.).
     - View verified providers sorted by **Ratings** (4.9 ★, 4.8 ★), distance, and hourly rates.
     - Select appointment date and available time slots (Morning, Midday, Afternoon, Evening).
     - Enter issue notes and service address to submit bookings.
     - Live booking tracking with status badges (`PENDING`, `ACCEPTED`, `IN_PROGRESS`, `COMPLETED`).
     - Real-time in-app notification center.
   - **Service Provider Mode**:
     - Register with trade category, hourly rate, and years of experience.
     - Incoming requests feed showing client details, problem descriptions, and booked time slots.
     - One-tap **Accept Booking** or **Decline** actions.
     - Active job manager: advance status from `ACCEPTED` to `IN_PROGRESS` and `COMPLETED`.
   - **Instant Role Switcher**:
     - Built-in floating switcher allows toggling between Customer and Provider on a single device or emulator to test the entire lifecycle immediately!

2. **Booking Acceptance Notifications**:
   - When a provider taps **Accept Booking**, an Android system notification is posted to the status bar (utilizing `NotificationCompat.Builder` and high-priority channel).
   - In-app notification badge and alert list update immediately.

3. **Pre-Seeded Data & Persistence**:
   - Includes certified specialists for all categories with realistic ratings, reviews, and hourly rates.
   - Local state persists across sessions.

---

## 🚀 How to Run in Android Studio

1. Open **Android Studio**.
2. Click **Open** (or `File -> Open`).
3. Select the folder:
   ```
   C:\Users\sk621\.gemini\antigravity\scratch\servicesync
   ```
4. Allow Android Studio to sync Gradle (Gradle 8.11.1 and AGP 8.7.3 with Kotlin 2.0.21).
5. Select an emulator or connected physical Android device.
6. Click **Run ▶** (`Shift + F10`).

---

## 🧪 Testing the Complete Booking & Notification Flow

1. **Start as Customer**:
   - Explore categories (e.g. tap **Plumber** or **Electrician**).
   - Tap on **Marcus Vance (Plumber, 4.9 ★)** or **Alex Turner (Electrician, 4.9 ★)**.
   - Select a Date and Time Slot (e.g. `Tomorrow, Sep 6` at `11:00 AM - 01:00 PM`).
   - Type an issue note (e.g. *"Sink pipe leaking under the cabinet"*) and hit **Confirm & Book Slot**.
   - Your booking will now show status **PENDING**.

2. **Switch to Provider Role**:
   - Tap the **Switch** button on the bottom bar (or top header) and choose **Provider Mode (Marcus Vance)**.
   - Tap on the **Requests** tab (or "New Customer Requests" on the dashboard).
   - You will see the incoming request from the customer with the selected date, slot, and problem description.
   - Tap the green **Accept Booking** button!

3. **Verify the Notification**:
   - A system push notification will fire on your Android status bar:
     > *"🎉 Booking Accepted! Marcus Vance (Plumber) accepted your request for Tomorrow at 11:00 AM - 01:00 PM."*
   - Switch back to **Customer Mode**:
     - Check the **Alerts** tab: the new acceptance notification is listed.
     - Check the **Bookings** tab: the status has updated from `Pending` to `ACCEPTED & SCHEDULED`!
