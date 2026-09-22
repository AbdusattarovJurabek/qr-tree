package uz.kochatzor.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

data class Session(
 val username: String,
 val token: String,
 val role: String,
 val regionId: Long,
 val region: String,
 val districtId: Long,
 val district: String,
)

class AuthRepository(context: Context) {
 private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
 val session = MutableStateFlow(load())

 private fun load(): Session? {
  val token = prefs.getString("token", null) ?: return null
  return Session(
   username = prefs.getString("username", "") ?: "",
   token = token,
   role = prefs.getString("role", "field") ?: "field",
   regionId = prefs.getLong("regionId", 0L),
   region = prefs.getString("region", "") ?: "",
   districtId = prefs.getLong("districtId", 0L),
   district = prefs.getString("district", "") ?: "",
  )
 }

 suspend fun login(username: String, password: String): Result<Session> {
  val api = RemoteApi.api ?: return Result.failure(IllegalStateException("Server manzili sozlanmagan"))
  return try {
   val res = api.login(LoginRequest(username.trim().lowercase(), password))
   val s = Session(username.trim().lowercase(), res.token, res.role, res.regionId ?: 0L, res.region ?: "", res.districtId ?: 0L, res.district ?: "")
   prefs.edit()
    .putString("token", s.token).putString("username", s.username).putString("role", s.role)
    .putLong("regionId", s.regionId).putString("region", s.region)
    .putLong("districtId", s.districtId).putString("district", s.district)
    .apply()
   session.value = s
   Result.success(s)
  } catch (e: retrofit2.HttpException) {
   val msg = if (e.code() == 401) "Login yoki parol noto'g'ri" else "Serverga ulanib bo'lmadi"
   Result.failure(Exception(msg))
  } catch (e: Exception) {
   Result.failure(Exception("Internetga ulanib bo'lmadi. Qayta urinib ko'ring."))
  }
 }

 fun logout() {
  prefs.edit().clear().apply()
  session.value = null
 }

 fun token(): String? = session.value?.token
}
