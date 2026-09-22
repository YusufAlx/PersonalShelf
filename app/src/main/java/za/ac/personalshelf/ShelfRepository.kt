package za.ac.personalshelf

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

private val Context.shelfStore by preferencesDataStore("personal_shelf")

/** Local copy is always available; Firebase is the cloud source of truth once configured. */
class ShelfRepository(private val context: Context) {
    private object Keys {
        val count = intPreferencesKey("count")
        fun field(index: Int, name: String) = stringPreferencesKey("$index.$name")
        fun flag(index: Int, name: String) = booleanPreferencesKey("$index.$name")
        fun number(index: Int, name: String) = intPreferencesKey("$index.$name")
    }

    val items: Flow<List<ShelfItem>> = context.shelfStore.data.map { pref ->
        (0 until (pref[Keys.count] ?: 0)).mapNotNull { i ->
            val title = pref[Keys.field(i, "title")] ?: return@mapNotNull null
            ShelfItem(
                id = pref[Keys.field(i, "id")] ?: "local-$i", title = title,
                author = pref[Keys.field(i, "author")] ?: "Unknown",
                category = runCatching { Category.valueOf(pref[Keys.field(i, "category")] ?: "Books") }.getOrDefault(Category.Books),
                description = pref[Keys.field(i, "description")] ?: "",
                progress = pref[Keys.number(i, "progress")] ?: 0,
                favourite = pref[Keys.flag(i, "favourite")] ?: false,
                offlineReady = pref[Keys.flag(i, "offline")] ?: false,
                pendingSync = pref[Keys.flag(i, "pending")] ?: true
            )
        }
    }

    suspend fun save(items: List<ShelfItem>) = context.shelfStore.edit { p ->
        p[Keys.count] = items.size
        items.forEachIndexed { i, item ->
            p[Keys.field(i, "id")] = item.id; p[Keys.field(i, "title")] = item.title
            p[Keys.field(i, "author")] = item.author; p[Keys.field(i, "category")] = item.category.name
            p[Keys.field(i, "description")] = item.description; p[Keys.number(i, "progress")] = item.progress
            p[Keys.flag(i, "favourite")] = item.favourite; p[Keys.flag(i, "offline")] = item.offlineReady
            p[Keys.flag(i, "pending")] = item.pendingSync
        }
    }

    suspend fun localItems(): List<ShelfItem> = items.first()

    /** Synchronises local changes to Firestore. No passwords are ever stored by this app. */
    suspend fun syncToCloud(items: List<ShelfItem>): Result<Unit> = runCatching {
        val user = FirebaseAuth.getInstance().currentUser ?: return@runCatching
        val db = FirebaseFirestore.getInstance()
        items.forEach { item ->
            db.collection("users").document(user.uid).collection("library").document(item.id)
                .set(mapOf("title" to item.title, "author" to item.author, "category" to item.category.name,
                    "description" to item.description, "progress" to item.progress, "favourite" to item.favourite)).await()
        }
    }
}
