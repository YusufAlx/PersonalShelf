package za.ac.personalshelf

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Runs only when Android reports network connectivity, keeping offline changes in sync. */
class SyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val repository = ShelfRepository(applicationContext)
        return repository.syncToCloud(repository.localItems()).fold(
            onSuccess = { Result.success() }, onFailure = { Result.retry() }
        )
    }
}
