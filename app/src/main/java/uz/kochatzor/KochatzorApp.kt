package uz.kochatzor
import android.app.Application
import uz.kochatzor.data.*
import uz.kochatzor.domain.*
class KochatzorApp: Application() {
 val reference by lazy { Databases.reference(this) }
 val repository: SurveyRepository by lazy { LocalSurveyRepository(Databases.surveys(this).dao()) }
 val auth: AuthRepository by lazy { AuthRepository(this) }
 override fun onCreate() {
  super.onCreate()
  SyncWorker.schedule(this)
 }
}
