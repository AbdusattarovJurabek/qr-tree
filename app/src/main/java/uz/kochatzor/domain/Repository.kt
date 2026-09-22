package uz.kochatzor.domain
import uz.kochatzor.data.*
import kotlinx.coroutines.flow.Flow
import java.time.*

data class Filters(val query: String="",val region: Long=0,val district: Long=0,val mahalla: Long=0,val tree: String="",val period: String="Barchasi",val from: String="",val to: String="",val regionName:String="",val districtName:String="",val mahallaName:String="") {
 fun range(today: LocalDate=LocalDate.now(),zone: ZoneId=ZoneId.systemDefault()): Pair<Long,Long> {
  val week=today.minusDays((today.dayOfWeek.value-1).toLong())
  val pair=when(period) {
   "Bugun" -> today to today.plusDays(1)
   "Kecha" -> today.minusDays(1) to today
   "Shu hafta" -> week to week.plusWeeks(1)
   "O‘tgan hafta" -> week.minusWeeks(1) to week
   "Shu oy" -> today.withDayOfMonth(1) to today.withDayOfMonth(1).plusMonths(1)
   "O‘tgan oy" -> today.withDayOfMonth(1).minusMonths(1) to today.withDayOfMonth(1)
   "Sana oralig‘i" -> LocalDate.parse(from) to LocalDate.parse(to).plusDays(1)
   else -> return 0L to Long.MAX_VALUE
  }
  require(pair.first < pair.second) { "Sana oralig‘i noto‘g‘ri" }
  return pair.first.atStartOfDay(zone).toInstant().toEpochMilli() to pair.second.atStartOfDay(zone).toInstant().toEpochMilli()
 }
}
interface SurveyRepository {
 fun observe(filters: Filters=Filters()): Flow<List<Survey>>
 suspend fun get(id: String): Survey?
 suspend fun save(row: Survey)
 suspend fun delete(id: String)
 suspend fun pendingSync(): List<Survey>
}
class LocalSurveyRepository(private val dao: SurveyDao): SurveyRepository {
 override fun observe(filters: Filters): Flow<List<Survey>> { val (a,b)=filters.range(); return dao.observe(normalize(filters.query),filters.region,filters.district,filters.mahalla,filters.tree,a,b) }
 override suspend fun get(id:String)=dao.get(id)
 override suspend fun save(row:Survey)=dao.save(row)
 override suspend fun delete(id:String)=dao.delete(id,System.currentTimeMillis())
 override suspend fun pendingSync()=dao.pendingSync()
}
