package uz.kochatzor.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import uz.kochatzor.KochatzorApp

class LoginViewModel(app: Application) : AndroidViewModel(app) {
 private val auth = (app as KochatzorApp).auth
 suspend fun login(username: String, password: String) = auth.login(username, password)
}
