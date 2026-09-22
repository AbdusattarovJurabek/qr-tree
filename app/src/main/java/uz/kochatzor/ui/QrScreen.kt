package uz.kochatzor.ui
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.kochatzor.data.Survey
import uz.kochatzor.util.*
import java.io.File
@Composable fun QrScreen(s:Survey,vm:AppViewModel) {
 val context=LocalContext.current
 var bitmap by remember(s){mutableStateOf<android.graphics.Bitmap?>(null)}
 LaunchedEffect(s) {try {bitmap=withContext(Dispatchers.Default){Qr.bitmap(Qr.payload(s))}}catch(e:Exception){vm.notify("QR yaratilmadi: ${e.message}")}}
 fun save() {vm.action {withContext(Dispatchers.IO){Files.gallery(context,Qr.png(Qr.payload(s)))};vm.notify("QR galereyaga saqlandi")}}
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {granted->if(granted)save() else vm.notify("Android 7–9 da galereyaga yozish ruxsati kerak. Ulashishdan foydalanishingiz mumkin.")}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Text("Xonadon QR kodi",style=MaterialTheme.typography.headlineMedium)
  if(bitmap!=null)Image(bitmap!!.asImageBitmap(),"${s.fio} QR kodi",Modifier.fillMaxWidth().aspectRatio(1f)) else CircularProgressIndicator()
  Text(s.fio,style=MaterialTheme.typography.titleLarge)
  Text("${s.district} • ${s.mahalla}\n${s.tree} - ${s.variety}")
  Button(onClick={if(Build.VERSION.SDK_INT<=28 && ContextCompat.checkSelfPermission(context,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else save()},modifier=Modifier.fillMaxWidth()){Text("QR NI GALEREYAGA SAQLASH")}
  OutlinedButton(onClick={vm.action {val file=withContext(Dispatchers.IO){File(context.cacheDir,"exports").mkdirs();File(context.cacheDir,"exports/qr_${s.id}.png").apply {writeBytes(Qr.png(Qr.payload(s)))}};Files.share(context,file,"image/png")}},modifier=Modifier.fillMaxWidth()){Text("ULASHISH")}
  Button(
    onClick = { Printer.printCard(context, s) },
    modifier = Modifier.fillMaxWidth().height(50.dp),
    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
  ) {
    Icon(Icons.Outlined.Print, "", Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text("PRINTERDA CHIQARISH (CHOP ETISH)")
  }
 }
}
