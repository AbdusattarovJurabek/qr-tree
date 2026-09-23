package uz.kochatzor.data

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
 val latitude: Double, val longitude: Double,
)

fun Survey.toDto() = SurveyDto(
 id, regionId, districtId, mahallaId, region, district, mahalla, fio, phone, area, tree,
 variety, count, planting, source, payvandtag, createdAt, updatedAt, isDeleted,
 latitude, longitude,
)

data class SyncRequest(val records: List<SurveyDto>)
data class SyncResultItem(val id: String, val updatedAt: Long)
data class SyncResponse(val results: List<SyncResultItem>)

data class LoginRequest(val username: String, val password: String)
data class LoginResponse(
 val token: String, val role: String,
 val regionId: Long?, val region: String?,
 val districtId: Long?, val district: String?,
)

interface KochatzorApi {
 @POST("api/auth/login")
 suspend fun login(@Body body: LoginRequest): LoginResponse

 @POST("api/sync")
 suspend fun sync(
  @Header("Authorization") bearer: String,
  @Body body: SyncRequest,
 ): SyncResponse
}

object RemoteApi {
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
