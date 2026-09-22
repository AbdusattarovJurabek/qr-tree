package uz.kochatzor.ui
import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import uz.kochatzor.KochatzorApp
import uz.kochatzor.data.*
import uz.kochatzor.domain.*
import uz.kochatzor.util.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.io.File

data class Draft(val id:String="",val region:Long=0,val regionName:String="",val district:Long=0,val districtName:String="",val mahalla:Long=0,val mahallaName:String="",val fio:String="",val phone:String="",val area:String="",val tree:String="",val variety:String="",val count:String="",val planting:String="",val source:String="",val payvandtag:String="") {
 fun json()=JSONObject().apply { put("id",id);put("region",region);put("regionName",regionName);put("district",district);put("districtName",districtName);put("mahalla",mahalla);put("mahallaName",mahallaName);put("fio",fio);put("phone",phone);put("area",area);put("tree",tree);put("variety",variety);put("count",count);put("planting",planting);put("source",source);put("payvandtag",payvandtag) }.toString()
 companion object { fun read(s:String):Draft { val j=JSONObject(s);return Draft(j.optString("id"),j.optLong("region"),j.optString("regionName"),j.optLong("district"),j.optString("districtName"),j.optLong("mahalla"),j.optString("mahallaName"),j.optString("fio"),j.optString("phone"),j.optString("area"),j.optString("tree"),j.optString("variety"),j.optString("count"),j.optString("planting"),j.optString("source"),j.optString("payvandtag")) } }
}
val trees=listOf("Olma","Nok","Behi","O‘rik","Olxo‘ri","Shaftoli","Gilos","Olcha","Xurmo","Yong‘oq","Bodom","Pista","Malina","Ejevika","Qulupnay","Golubika","Anor","Zaytun","Anjir","Namatak","Unabi","Do‘lana","Limon","Mandarin","Apelsin","Banan","Kivi","Papaya","Uzum","Boshqa")
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app:Application,private val saved:SavedStateHandle):AndroidViewModel(app) {
 private val graph=app as KochatzorApp
 private val repo=graph.repository
 val ref=graph.reference.dao()
 private val messages=Channel<String>(Channel.BUFFERED);val events=messages.receiveAsFlow()
 val ready=MutableStateFlow(false)
 val busy=MutableStateFlow(false)
 val draft=MutableStateFlow(runCatching { Draft.read(saved["draft"]?:"{}") }.getOrDefault(Draft()))
 val lastSaved=MutableStateFlow<Survey?>(null)
 val filters=MutableStateFlow(Filters())
 val all=repo.observe().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val records=filters.flatMapLatest { repo.observe(it) }.catch { notify("Ma’lumotlarni o‘qishda xato: ${it.message}") }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val regions=ref.regions().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val exportFile=MutableStateFlow<File?>(null)
 private val prefs=app.getSharedPreferences("settings",0)
 val theme=MutableStateFlow(prefs.getString("theme","Tizim")?:"Tizim")
 val defRegion=MutableStateFlow(prefs.getLong("defRegion",0L))
 val defRegionName=MutableStateFlow(prefs.getString("defRegionName","")?:"")
 val defDistrict=MutableStateFlow(prefs.getLong("defDistrict",0L))
 val defDistrictName=MutableStateFlow(prefs.getString("defDistrictName","")?:"")
 init { 
  action { withContext(Dispatchers.IO) { graph.reference.seed(app) };ready.value=true }
  if (draft.value.id.isBlank() && draft.value.region == 0L) newDraft()
 }
 fun notify(text:String) { viewModelScope.launch { messages.send(text) } }
 fun action(block:suspend ()->Unit) { viewModelScope.launch { try { block() } catch(e:CancellationException) { throw e } catch(e:Exception) { notify(e.message?:"Amal bajarilmadi. Qayta urinib ko‘ring.") } } }
 fun update(d:Draft) { draft.value=d;saved["draft"]=d.json();lastSaved.value=null }
 fun newDraft() { update(Draft(region=defRegion.value,regionName=defRegionName.value,district=defDistrict.value,districtName=defDistrictName.value)) }
 fun addTreeToSameOwner() { val d=draft.value;update(Draft(region=d.region,regionName=d.regionName,district=d.district,districtName=d.districtName,mahalla=d.mahalla,mahallaName=d.mahallaName,fio=d.fio,phone=d.phone,area=d.area)) }
 fun addTreeToXonadon(s: Survey) { update(Draft(region=s.regionId,regionName=s.region,district=s.districtId,districtName=s.district,mahalla=s.mahallaId,mahallaName=s.mahalla,fio=s.fio,phone=s.phone,area=s.area.toString())) }
 fun setLocation(rId:Long,rName:String,dId:Long,dName:String) { 
  defRegion.value=rId;defRegionName.value=rName;defDistrict.value=dId;defDistrictName.value=dName
  prefs.edit().putLong("defRegion",rId).putString("defRegionName",rName).putLong("defDistrict",dId).putString("defDistrictName",dName).apply()
  if(draft.value.id.isBlank() || draft.value.region == 0L) update(draft.value.copy(region=rId,regionName=rName,district=dId,districtName=dName)) 
 }
 fun setTheme(s:String) {theme.value=s;prefs.edit().putString("theme",s).apply()}
 fun applyFilters(f:Filters):Boolean=try { f.range();filters.value=f;true } catch(e:Exception) {notify("Sanalarni YYYY-MM-DD shaklida to‘g‘ri kiriting");false}
 fun edit(s:Survey) { update(Draft(s.id,s.regionId,s.region,s.districtId,s.district,s.mahallaId,s.mahalla,s.fio,s.phone,s.area.toString(),s.tree,s.variety,s.count.toString(),s.planting,s.source,s.payvandtag)) }
 fun save() {
  if(busy.value)return
  val d=draft.value
  action {
   require(ready.value) {"Manzillar bazasi hali tayyor emas"}
   require(d.region>0 && d.district>0 && d.mahalla>0) {"Viloyat, tuman va MFYni tanlang"}
   require(listOf(d.fio,d.phone,d.tree,d.variety,d.planting,d.source).all {it.isNotBlank()}) {"Barcha maydonlarni to‘ldiring"}
   require(d.phone.matches(Regex("\\+?[0-9 ()-]{7,22}")) && d.phone.count {it.isDigit()} in 7..15) {"Telefon raqamini tekshiring"}
   require(listOf(d.fio,d.variety,d.source).all { it.length<=200 && '\n' !in it && '\r' !in it }) {"Matnlar 200 belgidan oshmasin va bir qatorda bo‘lsin"}
   val area=d.area.replace(',','.').toDoubleOrNull();require(area!=null && area.isFinite() && area>0) {"Yer maydoni musbat o‘nli son bo‘lsin"}
   val count=d.count.toIntOrNull();require(count!=null && count>0) {"Ko‘chat soni musbat butun son bo‘lsin"}
   require(runCatching { d.planting.toInt() in 1900..2100 }.getOrDefault(false)) {"Ekish yili: 1900 dan 2100 gacha son bo'lishi kerak"}
   busy.value=true
   try {
    val old=if(d.id.isNotEmpty())repo.get(d.id) else null
    val now=System.currentTimeMillis()
    val s=Survey(old?.id?:UUID.randomUUID().toString(),d.region,d.district,d.mahalla,d.regionName,d.districtName,d.mahallaName,d.fio.trim(),d.phone.trim(),area,d.tree,d.variety.trim(),count,d.planting,d.source.trim(),d.payvandtag.trim(),old?.createdAt?:now,now,if(old==null)"LOCAL" else "PENDING",false,normalize(listOf(d.fio,d.mahallaName,d.districtName,d.tree,d.variety).joinToString(" ")))
    repo.save(s);update(d.copy(id=s.id));lastSaved.value=s;notify("Ma’lumot saqlandi")
   } finally {busy.value=false}
  }
 }
 fun saveHouseholdOnly() {
  if(busy.value)return
  val d=draft.value
  action {
   require(ready.value) {"Manzillar bazasi hali tayyor emas"}
   require(d.region>0 && d.district>0 && d.mahalla>0) {"Viloyat, tuman va MFYni tanlang"}
   require(listOf(d.fio,d.phone).all {it.isNotBlank()}) {"F.I.Sh. va telefon raqamini kiriting"}
   require(d.phone.matches(Regex("\\+?[0-9 ()-]{7,22}")) && d.phone.count {it.isDigit()} in 7..15) {"Telefon raqamini tekshiring"}
   val area=d.area.replace(',','.').toDoubleOrNull();require(area!=null && area.isFinite() && area>0) {"Yer maydoni musbat o‘nli son bo‘lsin"}
   busy.value=true
   try {
    val now=System.currentTimeMillis()
    val s=Survey(UUID.randomUUID().toString(),d.region,d.district,d.mahalla,d.regionName,d.districtName,d.mahallaName,d.fio.trim(),d.phone.trim(),area,"","",0,"","","",now,now,"LOCAL",false,normalize(listOf(d.fio,d.mahallaName,d.districtName).joinToString(" ")))
    repo.save(s);lastSaved.value=s;notify("Xonadon muvaffaqiyatli saqlandi!")
   } finally {busy.value=false}
  }
 }
 fun saveTreeForHousehold(h: Survey, tree: String, variety: String, payvandtag: String, countStr: String, planting: String, source: String) {
  if(busy.value)return
  action {
   require(listOf(tree,variety,planting,source).all {it.isNotBlank()}) {"Barcha ko‘chat maydonlarini to‘ldiring"}
   val count=countStr.toIntOrNull();require(count!=null && count>0) {"Ko‘chat soni musbat butun son bo‘lsin"}
   require(runCatching { planting.toInt() in 1900..2100 }.getOrDefault(false)) {"Ekish yili: 1900 dan 2100 gacha son bo'lishi kerak"}
   busy.value=true
   try {
    val now=System.currentTimeMillis()
    // Editing an existing tree (h.id set via edit()) must update that same row, not create a duplicate.
    val editing = h.id.takeIf { it.isNotBlank() }?.let { repo.get(it) }
    val emptyEntry = if (editing==null) repo.observe().first().find { it.mahallaId == h.mahallaId && it.fio == h.fio && it.phone == h.phone && it.count == 0 && it.tree.isBlank() } else null
    val target = editing ?: emptyEntry
    val surveyId = target?.id ?: UUID.randomUUID().toString()
    val s=Survey(surveyId,h.regionId,h.districtId,h.mahallaId,h.region,h.district,h.mahalla,h.fio,h.phone,h.area,tree,variety.trim(),count,planting,source.trim(),payvandtag.trim(),target?.createdAt?:now,now,if(editing!=null)"PENDING" else "LOCAL",false,normalize(listOf(h.fio,h.mahalla,h.district,tree,variety).joinToString(" ")))
    repo.save(s);lastSaved.value=s;notify("Ko‘chat muvaffaqiyatli saqlandi!")
   } finally {busy.value=false}
  }
 }
 fun delete(s:Survey,done:()->Unit)=action {repo.delete(s.id);done();notify("Ko‘chat o‘chirildi")}
 fun export(everything:Boolean) {
  if(busy.value)return
  busy.value=true
  action { try {
   val snapshot=repo.observe(if(everything)Filters() else filters.value).first().filter { it.tree.isNotBlank() }
   require(snapshot.isNotEmpty()) {"Eksport uchun ko‘chatlar yo‘q"}
   exportFile.value=withContext(Dispatchers.IO) {
    val dir=File(getApplication<Application>().cacheDir,"exports").apply {mkdirs()}
    val file=File(dir,"kochatzor_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))}.xlsx")
    try {file.outputStream().use {Xlsx.write(snapshot,it)};file} catch(e:Exception) {file.delete();throw e}
   };notify("Excel tayyor: ${snapshot.size} ta ko‘chat")
  } finally {busy.value=false} }
 }
}
