package uz.kochatzor.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import uz.kochatzor.util.Qr
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable fun Scanner() {
 val context=LocalContext.current
 var granted by remember {mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)}
 var raw by rememberSaveable {mutableStateOf<String?>(null)};var error by remember {mutableStateOf<String?>(null)}
 val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {granted=it;if(!it)error="Kameradan foydalanishga ruxsat berilmadi. Sozlamalarda kamera ruxsatini yoqing."}
 
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Text("QR Skaner", style=MaterialTheme.typography.headlineMedium, fontWeight=FontWeight.Bold)
  Text("Ko‘chatzor QR kodini kamera chorchovasiga joylashtiring. Skaner internetsiz ishlaydi.", style=MaterialTheme.typography.bodyMedium, color=MaterialTheme.colorScheme.onSurfaceVariant)
  
  if(error!=null) {
   GlassCard(shape = RoundedCornerShape(20.dp), tint = MaterialTheme.colorScheme.errorContainer, tintAlpha = 0.8f) {
    Text(error!!, Modifier.padding(16.dp), color=MaterialTheme.colorScheme.onErrorContainer)
   }
  }
  
  if(!granted) {
   Button(onClick={launcher.launch(Manifest.permission.CAMERA)}, modifier=Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)){
    Icon(Icons.Outlined.CameraAlt, contentDescription = null)
    Spacer(Modifier.width(8.dp))
    Text("Kameraga ruxsat berish", fontWeight = FontWeight.Bold)
   }
  } else if(raw==null) {
   GlassCard(shape=RoundedCornerShape(28.dp), modifier=Modifier.fillMaxWidth()) {
    CameraPreview(onResult={text->raw=text;error=null},onError={error=it})
   }
  } else {
   val parsed = remember(raw) { runCatching { Qr.parse(raw!!) } }
   GlassCard(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(28.dp),
    tint = MaterialTheme.colorScheme.primaryContainer,
    tintAlpha = 0.82f
   ) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
     Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
      Row(verticalAlignment = Alignment.CenterVertically) {
       Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp)) {
        Box(contentAlignment = Alignment.Center) {
         Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
        }
       }
       Spacer(Modifier.width(10.dp))
       Text("QR KOD O'QILDI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
      }
      Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)) {
       Text("  RASMIY  ", modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      }
     }
     
     HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
     
     if(parsed.isSuccess) {
      val p = parsed.getOrNull() ?: emptyMap()
      if (p.isEmpty()) {
       Text("Aks etgan matn:\n$raw", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
      } else {
       Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        p["FIO"]?.let {
         Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(10.dp))
          Column {
           Text("Xonadon egasi F.I.Sh.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
           Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
          }
         }
        }
        
        p["TUR"]?.let {
         Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Outlined.Nature, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(10.dp))
          Column {
           Text("Meva turi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
           Text(it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
          }
         }
        }
        
        p["NAV"]?.let {
         Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Outlined.Category, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(10.dp))
          Column {
           Text("Meva navi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
           Text(it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
          }
         }
        }
        
        p["PAYVANDTAG"]?.let {
         Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Outlined.Spa, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(10.dp))
          Column {
           Text("Payvandtag", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
           Text(it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
          }
         }
        }
        
        p["YIL"]?.let {
         Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Outlined.CalendarToday, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(10.dp))
          Column {
           Text("Ekilgan yili", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
           Text("$it-yil", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
          }
         }
        }
        
        p["TUMAN"]?.let { Info("Tuman/shahar", it) }
        p["MFY"]?.let { Info("MFY", it) }
        p["KOCHAT"]?.let { Info("Ko‘chat", it) }
       }
      }
     } else {
      Text(parsed.exceptionOrNull()?.message?:"QR kodi tanilmadi", color=MaterialTheme.colorScheme.error)
     }
    }
   }
   
   Button(
    onClick={raw=null;error=null},
    modifier=Modifier.fillMaxWidth().heightIn(min = 54.dp),
    shape = RoundedCornerShape(16.dp)
   ){
    Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
    Spacer(Modifier.width(8.dp))
    Text("Qayta skanerlash", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
   }
  }
 }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable private fun CameraPreview(onResult:(String)->Unit,onError:(String)->Unit) {
 val context=LocalContext.current;val owner=LocalLifecycleOwner.current
 val view=remember {PreviewView(context).apply {implementationMode=PreviewView.ImplementationMode.COMPATIBLE}}
 val result by rememberUpdatedState(onResult);val error by rememberUpdatedState(onError)
 AndroidView(factory={view},modifier=Modifier.fillMaxWidth().height(360.dp))
 DisposableEffect(owner) {
  val executor=Executors.newSingleThreadExecutor();val scanner=BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
  val done=AtomicBoolean(false);var disposed=false;var provider:ProcessCameraProvider?=null
  val preview=Preview.Builder().build().also {it.surfaceProvider=view.surfaceProvider}
  val analysis=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
  analysis.setAnalyzer(executor) {proxy->
   val media=proxy.image
   if(media==null || done.get())proxy.close() else {
    try {
     scanner.process(InputImage.fromMediaImage(media,proxy.imageInfo.rotationDegrees))
      .addOnSuccessListener {codes->
       if(!disposed) {
        codes.firstOrNull {it.rawValue!=null}?.rawValue?.let {
         if(done.compareAndSet(false,true)) result(it)
        }
       }
      }
      .addOnFailureListener {if(!disposed)error("QR o‘qilmadi. Kamerani kodga yaqinlashtiring.")}
      .addOnCompleteListener {proxy.close()} 
    } catch(e:Exception){proxy.close()}
   }
  }
  val future=ProcessCameraProvider.getInstance(context)
  future.addListener({
   if(!disposed) {
    try {
     provider=future.get()
     provider!!.bindToLifecycle(owner,CameraSelector.DEFAULT_BACK_CAMERA,preview,analysis)
    } catch(e:Exception){
     error("Kamerani ochib bo‘lmadi: ${e.message}")
    }
   }
  },ContextCompat.getMainExecutor(context))
  onDispose {
   disposed=true;done.set(true);analysis.clearAnalyzer();provider?.unbind(preview,analysis);scanner.close();executor.shutdown()
  }
 }
}
