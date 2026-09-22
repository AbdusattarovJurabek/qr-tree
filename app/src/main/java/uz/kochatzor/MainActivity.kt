package uz.kochatzor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import uz.kochatzor.ui.*

class MainActivity: ComponentActivity() {
 override fun onCreate(savedInstanceState:Bundle?) { 
  super.onCreate(savedInstanceState)
  enableEdgeToEdge()
  setContent {
   val vm:AppViewModel=viewModel()
   val theme by vm.theme.collectAsStateWithLifecycle()
   val dark = theme == "To‘q" || (theme == "Tizim" && isSystemInDarkTheme())
   
   val lightColors = lightColorScheme(
    primary = Color(0xFF006C4C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF8AF5C6),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF00687A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFADEDFF),
    onSecondaryContainer = Color(0xFF001F26),
    tertiary = Color(0xFF6A527A),
    tertiaryContainer = Color(0xFFF0DBFF),
    onTertiaryContainer = Color(0xFF2A1533),
    background = Color(0xFFF8FAF9),
    onBackground = Color(0xFF161D19),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF161D19),
    surfaceVariant = Color(0xFFDEE5DF),
    onSurfaceVariant = Color(0xFF414945),
    outline = Color(0xFF717975),
    outlineVariant = Color(0xFFC1C9C3)
   )

   val darkColors = darkColorScheme(
    primary = Color(0xFF6EDBAB),
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF005238),
    onPrimaryContainer = Color(0xFF8AF5C6),
    secondary = Color(0xFF5DD5FC),
    onSecondary = Color(0xFF003643),
    secondaryContainer = Color(0xFF004E5C),
    onSecondaryContainer = Color(0xFFADEDFF),
    tertiary = Color(0xFFD8B4E8),
    tertiaryContainer = Color(0xFF4A3358),
    onTertiaryContainer = Color(0xFFF0DBFF),
    background = Color(0xFF0F1512),
    onBackground = Color(0xFFE0E3DE),
    surface = Color(0xFF131B17),
    onSurface = Color(0xFFE0E3DE),
    surfaceVariant = Color(0xFF414945),
    onSurfaceVariant = Color(0xFFC1C9C3)
   )

   MaterialTheme(colorScheme = if(dark) darkColors else lightColors) { 
    App(vm) 
   }
  } 
 }
}
