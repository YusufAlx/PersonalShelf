package za.ac.personalshelf

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ShelfMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val manager = getSystemService(NotificationManager::class.java)
        val channelId = "shelf_updates"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            manager.createNotificationChannel(NotificationChannel(channelId, "Shelf updates", NotificationManager.IMPORTANCE_DEFAULT))
        manager.notify(1, NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(message.notification?.title ?: "Personal Shelf")
            .setContentText(message.notification?.body ?: "Your shelf has been updated")
            .setAutoCancel(true).build())
    }
}
