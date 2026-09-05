# SaServe - On-Demand Service Booking Android App

**SaServe** is a modern, customer-centric Android application built with Kotlin and Jetpack Compose (Material 3) for booking home and vehicle service specialists (Electrician, Plumber, Carpenter, Mechanic, Appliance Repair, Painter).

## ✨ Key Features

1. **Pure Customer-Centric Experience**:
   - Clean, intuitive interface built exclusively for customers.
   - Browse services by category (Electrician, Plumber, Carpenter, Mechanic, Appliance Repair, Painter).
   - Dynamic catalog: Only service providers added by you are visible and bookable in the app!
   - Real-time in-app notification center for all booking alerts.

2. **Add & Manage Custom Service Providers**:
   - Tap **`+ Add Pro`** anytime from the top bar or category screens.
   - Enter provider name, trade category, contact phone/email, operating area, and hourly rate (in **₹ INR**).
   - Only your added providers will populate the service catalog.

3. **Booking & Time Slot Selection**:
   - Select appointment date and available time slots (Morning, Midday, Afternoon, Evening).
   - Enter issue notes and service address to confirm booking.
   - Live booking tracking with status badges (`PENDING`, `ACCEPTED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`).

4. **Booking Acceptance Notifications**:
   - High-priority Android `NotificationChannel` (`channel_service_bookings`).
   - Android system push notifications (`NotificationCompat.Builder`) triggered upon booking confirmation/acceptance.
   - Status updates automatically in the **Bookings** tab and **Alerts** badge.

5. **Indian Rupee (₹ INR) Pricing**:
   - All rates and price estimations are rendered in Indian Rupees (₹).

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

## 🧪 Testing Flow

1. **Add your first Service Provider**:
   - On the Home screen, tap **`+ Add Pro`** in the top bar.
   - Fill in the provider details (e.g. *Ramesh Electrician*, *Category: Electrician*, *Rate: ₹399/hr*).
   - Tap **Save & Add**.
   - Your newly added specialist now appears under **Electricians** and on the home feed!

2. **Book a Service Slot**:
   - Tap on the specialist card.
   - Select a Date and Time Slot (e.g. `Tomorrow, Sep 6` at `11:00 AM - 01:00 PM`).
   - Enter issue description (e.g. *"Switchboard sparking"*) and tap **Confirm & Book Slot**.

3. **Verify the Acceptance Notification**:
   - Go to the **Bookings** tab.
   - Under your pending booking, tap **`Simulate Acceptance`**.
   - Watch the Android system push notification fire immediately on your device status bar:
     > *"🎉 Booking Accepted by Ramesh Electrician! Service confirmed."*
   - Status updates to **ACCEPTED & SCHEDULED**!
