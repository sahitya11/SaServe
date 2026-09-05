package com.servicesync.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.servicesync.app.MainActivity
import com.servicesync.app.R
import com.servicesync.app.data.model.Booking

object NotificationHelper {

    const val CHANNEL_ID_BOOKINGS = "channel_service_bookings"
    const val CHANNEL_NAME_BOOKINGS = "Service Booking Updates"
    private const val CHANNEL_DESC_BOOKINGS = "Notifications about service booking requests, acceptances, and status changes"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_BOOKINGS,
                CHANNEL_NAME_BOOKINGS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_BOOKINGS
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendBookingAcceptedNotification(context: Context, booking: Booking) {
        // Only trigger if permission is granted on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_BOOKING_ID", booking.id)
            putExtra("EXTRA_NAV_TARGET", "customer_bookings")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            booking.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BOOKINGS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🎉 Booking Accepted!")
            .setContentText("${booking.providerName} (${booking.category.displayName}) accepted your request for ${booking.scheduledDate} at ${booking.scheduledSlot}.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Great news! Your service provider ${booking.providerName} has accepted your request.\n\n" +
                                "• Service: ${booking.category.displayName}\n" +
                                "• Date: ${booking.scheduledDate}\n" +
                                "• Slot: ${booking.scheduledSlot}\n" +
                                "• Address: ${booking.customerAddress}\n" +
                                "• Estimated Rate: ₹${booking.hourlyRate.toInt()}/hr"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(booking.id.hashCode(), notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun sendNewBookingRequestNotification(context: Context, booking: Booking) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_BOOKING_ID", booking.id)
            putExtra("EXTRA_NAV_TARGET", "provider_requests")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            booking.id.hashCode() + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BOOKINGS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🛎️ New Booking Request!")
            .setContentText("New ${booking.category.displayName} request from ${booking.customerName} for ${booking.scheduledDate}.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "You received a new booking request from ${booking.customerName}!\n\n" +
                                "• Issue: ${booking.issueDescription}\n" +
                                "• Slot: ${booking.scheduledDate} (${booking.scheduledSlot})\n" +
                                "• Location: ${booking.customerAddress}"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(booking.id.hashCode() + 1, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
