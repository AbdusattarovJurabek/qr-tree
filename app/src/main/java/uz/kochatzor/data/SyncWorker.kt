package uz.kochatzor.data

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

private const val TAG = "SyncWorker"
private const val WORK_NAME = "kochatzor-sync"
private const val BATCH_SIZE = 50

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

 override suspend fun doWork(): Result {
  val api = RemoteApi.api ?: return Result.success() // sync sozlanmagan (baseUrl bo'sh) — jim tugaydi
  val token = AuthRepository(applicationContext).token() ?: return Result.success() // hali login qilinmagan

  val dao = Databases.surveys(applicationContext).dao()
  val pending = dao.pendingSync()
  if (pending.isEmpty()) return Result.success()

  return try {
   pending.chunked(BATCH_SIZE).forEach { batch ->
    val response = api.sync("Bearer $token", SyncRequest(batch.map { it.toDto() }))
    response.results.forEach { dao.markSynced(it.id, it.updatedAt) }
   }
   Result.success()
  } catch (e: retrofit2.HttpException) {
   if (e.code() == 401) Result.failure() else Result.retry() // token yaroqsiz bo'lsa qayta urinishning foydasi yo'q
  } catch (e: Exception) {
   Log.w(TAG, "Sinxronlash muvaffaqiyatsiz, keyinroq qayta urinadi", e)
   Result.retry()
  }
 }

 companion object {
  fun schedule(context: Context) {
   val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()
   val request = PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES)
    .setConstraints(constraints)
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
    .build()
   WorkManager.getInstance(context)
    .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
  }
 }
}
