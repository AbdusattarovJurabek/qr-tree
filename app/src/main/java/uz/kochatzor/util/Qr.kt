package uz.kochatzor.util
import android.graphics.Bitmap
import com.google.zxing.*
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import uz.kochatzor.data.Survey
import java.io.ByteArrayOutputStream
object Qr {
 fun payload(s: Survey)= listOf("FIO" to s.fio,"TUR" to s.tree,"NAV" to s.variety,"PAYVANDTAG" to s.payvandtag,"YIL" to s.planting.take(4)).joinToString("\n") { (k,v)-> "$k=${v.replace('\n',' ').replace('\r',' ')}" }
 fun parse(text: String): Map<String,String> {
  require(text.length<=4096) { "QR matni juda uzun" }
  val lines=text.trim().lines()
  val validKeys=setOf("TUMAN","MFY","FIO","KOCHAT","TUR","NAV","PAYVANDTAG","YIL")
  val pairs=lines.map {
   val p=it.split('=',limit=2)
   require(p.size==2 && p[0] in validKeys) { "Notanish QR format" }
   p[0] to p[1]
  }
  require(pairs.isNotEmpty()) { "Bo‘sh QR" }
  require(pairs.map { it.first }.toSet().size==pairs.size) { "Takrorlangan maydon" }
  return pairs.toMap()
 }
 fun bitmap(text:String,size:Int=768):Bitmap {
  val m=QRCodeWriter().encode(text,BarcodeFormat.QR_CODE,size,size,mapOf(EncodeHintType.CHARACTER_SET to "UTF-8",EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,EncodeHintType.MARGIN to 4))
  val pixels=IntArray(size*size) { i->if(m[i%size,i/size]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }
  return Bitmap.createBitmap(pixels,size,size,Bitmap.Config.ARGB_8888)
 }
 fun png(text:String):ByteArray=ByteArrayOutputStream().use { out -> val b=bitmap(text,400); b.compress(Bitmap.CompressFormat.PNG,100,out); b.recycle(); out.toByteArray() }
}
