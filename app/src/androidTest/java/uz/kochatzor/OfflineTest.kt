package uz.kochatzor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import uz.kochatzor.data.*
import uz.kochatzor.domain.*
import uz.kochatzor.util.*
import java.io.File
@RunWith(AndroidJUnit4::class)
class OfflineTest {
 private val context=InstrumentationRegistry.getInstrumentation().targetContext
 private fun row(id:String="one",time:Long=1000)=Survey(id,1,1,1,"Farg‘ona","Quvasoy shahar","Soy bo‘yi MFY","Қодиров Абдухаххор","+998901234567",0.1,"Olma","Golden",20,"2026","Quva agro star MChJ",time,time,searchText=normalize("Қодиров Абдухаххор Soy bo‘yi MFY Quvasoy shahar Olma Golden"))
 @Test fun qrPngRoundTripAndRealExcel() {
  val r=row();val bitmap=Qr.bitmap(Qr.payload(r));val pixels=IntArray(bitmap.width*bitmap.height);bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
  val decoded=MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width,bitmap.height,pixels))))
  assertEquals(Qr.payload(r),decoded.text);assertEquals(4,Qr.parse(decoded.text).size)
  File(context.cacheDir,"verified.xlsx").outputStream().use {Xlsx.write(listOf(r,r.copy(id="two",fio="Abdulla & Ali",tree="Gilos",variety="Kordia")),it)}
 }
 @Test fun bundledMlKitReadsOfflineQr() {
  val scanner=com.google.mlkit.vision.barcode.BarcodeScanning.getClient()
  try {
   val codes=com.google.android.gms.tasks.Tasks.await(scanner.process(com.google.mlkit.vision.common.InputImage.fromBitmap(Qr.bitmap(Qr.payload(row())),0)),20,java.util.concurrent.TimeUnit.SECONDS)
   assertEquals(Qr.payload(row()),codes.first().rawValue)
  } finally {scanner.close()}
 }
 @Test fun galleryStoresRealPng() {
  if(android.os.Build.VERSION.SDK_INT<29)return
  val bytes=Qr.png(Qr.payload(row()));val uri=Files.gallery(context,bytes)
  try {assertArrayEquals(bytes,context.contentResolver.openInputStream(uri)!!.use {it.readBytes()})} finally {context.contentResolver.delete(uri,null,null)}
 }
 @Test fun combinedRoomFiltersAndSoftDelete()=runBlocking {
  val db=Room.inMemoryDatabaseBuilder(context,SurveyDb::class.java).build()
  try {
   val repo=LocalSurveyRepository(db.dao());repo.save(row());repo.save(row("two").copy(districtId=2,mahallaId=2));repo.save(row("three").copy(tree="Gilos"))
   val matching=repo.observe(Filters(query="ҚОДИРОВ",region=1,district=1,mahalla=1,tree="Olma")).first()
   assertEquals(listOf("one"),matching.map {it.id})
   repo.save(matching.single().copy(count=55,syncStatus="PENDING"));assertEquals(55,repo.get("one")!!.count)
   repo.delete("one");assertTrue(repo.get("one")!!.isDeleted);assertEquals(2,repo.observe().first().size);assertEquals("PENDING",repo.get("one")!!.syncStatus)
  } finally {db.close()}
 }
 @Test fun referenceSearchStaysWithinDistrict()=runBlocking {
  val db=Room.inMemoryDatabaseBuilder(context,ReferenceDb::class.java).build()
  try {db.seed(context);assertEquals(2,db.dao().regions().first().size);assertEquals(1,db.dao().mahallas(1,normalize("SOY"),0).size);assertTrue(db.dao().mahallas(2,normalize("SOY"),0).isEmpty())} finally {db.close()}
 }
}
