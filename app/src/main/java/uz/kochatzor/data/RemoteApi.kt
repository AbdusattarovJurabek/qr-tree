package uz.kochatzor.data

import android.os.Build
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import uz.kochatzor.BuildConfig
import java.util.concurrent.TimeUnit

data class SurveyDto(
 val id: String, val regionId: Long, val districtId: Long, val mahallaId: Long,
 val region: String, val district: String, val mahalla: String,
 val fio: String, val phone: String, val area: Double, val tree: String, val variety: String,
 val count: Int, val planting: String, val source: String, val payvandtag: String,
 val createdAt: Long, val updatedAt: Long, val isDeleted: Boolean,
)

fun Survey.toDto() = SurveyDto(
 id, regionId, districtId, mahallaId, region, district, mahalla, fio, phone, area, tree,
 variety, count, planting, source, payvandtag, createdAt, updatedAt, isDeleted,
)

data class SyncRequest(val deviceId: String, val records: List<SurveyDto>)
data class SyncResultItem(val id: String, val updatedAt: Long)
data class SyncResponse(val results: List<SyncResultItem>)

interface KochatzorApi {
 @POST("api/sync")
 suspend fun sync(
  @Header("X-Sync-Key") key: String,
  @Body body: SyncRequest,
 ): SyncResponse
}

object RemoteApi {
 val deviceId: String by lazy { "android-" + Build.MODEL.replace(" ", "-") + "-" + Build.ID }

 val api: KochatzorApi? by lazy {
  val baseUrl = BuildConfig.SYNC_BASE_URL
  if (baseUrl.isBlank()) return@lazy null
  val client = OkHttpClient.Builder()
   .connectTimeout(15, TimeUnit.SECONDS)
   .readTimeout(30, TimeUnit.SECONDS)
   .build()
  Retrofit.Builder()
   .baseUrl(baseUrl)
   .client(client)
   .addConverterFactory(GsonConverterFactory.create())
   .build()
   .create(KochatzorApi::class.java)
 }
}
