package uz.kochatzor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
 val vm: LoginViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
 var username by remember { mutableStateOf("") }
 var password by remember { mutableStateOf("") }
 var showPassword by remember { mutableStateOf(false) }
 var error by remember { mutableStateOf<String?>(null) }
 var loading by remember { mutableStateOf(false) }
 val scope = rememberCoroutineScope()

 fun doLogin() {
  if (username.isBlank() || password.isBlank() || loading) return
  loading = true; error = null
  scope.launch {
   val result = vm.login(username, password)
   loading = false
   result.onSuccess { onLoggedIn() }.onFailure { error = it.message }
  }
 }

 AppBackground {
  Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
   GlassCard(modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
     Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(56.dp)) {
      Box(contentAlignment = Alignment.Center) {
       Icon(Icons.Outlined.Eco, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
      }
     }
     Spacer(Modifier.height(14.dp))
     Text("Ko'chatzor", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
     Text("Tizimga kirish", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
     Spacer(Modifier.height(24.dp))

     OutlinedTextField(
      value = username, onValueChange = { username = it },
      label = { Text("Login") },
      leadingIcon = { Icon(Icons.Outlined.Person, null) },
      singleLine = true,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.fillMaxWidth(),
     )
     Spacer(Modifier.height(12.dp))
     OutlinedTextField(
      value = password, onValueChange = { password = it },
      label = { Text("Parol") },
      leadingIcon = { Icon(Icons.Outlined.Lock, null) },
      trailingIcon = {
       IconButton(onClick = { showPassword = !showPassword }) {
        Icon(if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null)
       }
      },
      visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
      singleLine = true,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.fillMaxWidth(),
     )

     if (error != null) {
      Spacer(Modifier.height(10.dp))
      Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
     }

     Spacer(Modifier.height(20.dp))
     Button(
      onClick = { doLogin() },
      enabled = !loading,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.fillMaxWidth().height(48.dp),
     ) {
      if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
      else Text("Kirish", fontWeight = FontWeight.Bold)
     }
    }
   }
  }
 }
}
