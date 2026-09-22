package uz.kochatzor
import org.junit.Assert.*
import org.junit.Test
import uz.kochatzor.data.*
import uz.kochatzor.domain.*
import uz.kochatzor.util.*
import java.io.*
import java.time.*
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
class CoreTest {
 private fun sample()=Survey("private-id",1,1,1,"Farg‘ona","Quvasoy shahar","Soy bo‘yi MFY","Қодиров Абдухаххор","+998901234567",0.1,"Olma","Golden",20,"2026","Manba",100,200,searchText="")
 @Test fun qrHasExactlyFourFieldsAndUnicode() {
  val payload=Qr.payload(sample());val p=Qr.parse(payload)
  assertEquals(setOf("TUMAN","MFY","FIO","KOCHAT"),p.keys)
  assertEquals("Қодиров Абдухаххор",p["FIO"])
  assertEquals("Olma - Golden",p["KOCHAT"])
  listOf("private-id","+998901234567","Manba","2026").forEach {assertFalse(payload.contains(it))}
 }
 @Test fun qrRejectsForeignOrDuplicateFields() {
  listOf("https://google.com", "TUMAN=A\nMFY=B\nFIO=C\nFIO=D", "TUMAN=A\nMFY=B\nFIO=C\nKOCHAT=D\nPHONE=123").forEach {assertTrue(runCatching {Qr.parse(it)}.isFailure)}
 }
 @Test fun calendarBoundariesAndInclusiveCustomEnd() {
  val day=LocalDate.of(2026,9,14);val zone=ZoneId.of("Asia/Tashkent")
  fun start(s:String)=LocalDate.parse(s).atStartOfDay(zone).toInstant().toEpochMilli()
  assertEquals(start("2026-09-07") to start("2026-09-14"),Filters(period="O‘tgan hafta").range(day,zone))
  assertEquals(start("2026-08-01") to start("2026-09-01"),Filters(period="O‘tgan oy").range(day,zone))
  assertEquals(start("2026-09-14") to start("2026-09-15"),Filters(period="Sana oralig‘i",from="2026-09-14",to="2026-09-14").range(day,zone))
  assertTrue(runCatching {Filters(period="Sana oralig‘i",from="2026-09-15",to="2026-09-14").range(day,zone)}.isFailure)
 }
 @Test fun apostrophesAndCyrillicNormalize() {assertEquals(normalize("СОЙ БЎЙИ ‘Olma’"),normalize("сой бўйи 'olma'"))}
 @Test fun xlsxContainsImagesRelationshipsAndNumericCells() {
  val out=ByteArrayOutputStream();val bytes=byteArrayOf(1,2,3,4)
  Xlsx.write(listOf(sample(),sample().copy(fio="A&B <C>")),out){bytes}
  val entries=mutableMapOf<String,ByteArray>()
  ZipInputStream(ByteArrayInputStream(out.toByteArray())).use {zip->while(true){val e=zip.nextEntry?:break;entries[e.name]=zip.readBytes()}}
  assertArrayEquals(bytes,entries["xl/media/qr1.png"]);assertArrayEquals(bytes,entries["xl/media/qr2.png"])
  val factory=DocumentBuilderFactory.newInstance().apply {isNamespaceAware=true}
  entries.filterKeys {it.endsWith(".xml")||it.endsWith(".rels")}.forEach {(_,b)->factory.newDocumentBuilder().parse(ByteArrayInputStream(b))}
  val sheet=entries.getValue("xl/worksheets/sheet1.xml").toString(Charsets.UTF_8)
  assertTrue(sheet.contains("A&amp;B &lt;C&gt;"));assertTrue(sheet.contains("<c r=\"I2\" s=\"1\"><v>20</v></c>"))
  val drawing=entries.getValue("xl/drawings/drawing1.xml").toString(Charsets.UTF_8)
  assertTrue(drawing.contains("<xdr:row>1</xdr:row>"));assertTrue(drawing.contains("<xdr:row>2</xdr:row>"));assertTrue(drawing.contains("cx=\"952500\""))
 }
}
