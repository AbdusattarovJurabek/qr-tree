package uz.kochatzor.util
import android.content.*
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
object Files {
 const val XLSX="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
 fun share(c:Context,file:File,mime:String) {
  val uri=FileProvider.getUriForFile(c,"${c.packageName}.files",file)
  c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type=mime; putExtra(Intent.EXTRA_STREAM,uri); clipData=ClipData.newRawUri("Fayl",uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) },"Ulashish"))
 }
 fun gallery(c:Context,bytes:ByteArray):android.net.Uri {
  val values=ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME,"kochatzor_${System.currentTimeMillis()}.png");put(MediaStore.Images.Media.MIME_TYPE,"image/png"); if(Build.VERSION.SDK_INT>=29) { put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/Kochatzor");put(MediaStore.Images.Media.IS_PENDING,1) } }
  val uri=c.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values) ?: error("Galereyada fayl yaratilmadi")
  try { c.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Rasm saqlanmadi"); if(Build.VERSION.SDK_INT>=29) c.contentResolver.update(uri,ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING,0) },null,null); return uri }
  catch(e:Exception) { c.contentResolver.delete(uri,null,null);throw e }
 }
}
