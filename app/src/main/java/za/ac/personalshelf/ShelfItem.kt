package za.ac.personalshelf

import java.util.UUID

enum class Category { Books, Comics, Documents, Study }

data class ShelfItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val author: String,
    val category: Category,
    val description: String = "",
    val progress: Int = 0,
    val favourite: Boolean = false,
    /** null means the file remains cloud-only; true means its local copy is ready offline. */
    val offlineReady: Boolean = false,
    val pendingSync: Boolean = true
)
