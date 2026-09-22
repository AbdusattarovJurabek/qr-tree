package uz.kochatzor.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.util.Locale

fun normalize(s: String) = s.lowercase(Locale.ROOT).replace('‘','\'').replace('’','\'').replace('ʻ','\'').replace('`','\'')
@Entity(tableName="reference_meta") data class ReferenceMeta(@PrimaryKey val id: Int=1,val checksum: String)
@Entity(tableName="regions") data class Region(@PrimaryKey val id: Long, val name: String)
@Entity(tableName="districts", indices=[Index("regionId")]) data class District(@PrimaryKey val id: Long, val regionId: Long, val name: String)
@Entity(tableName="mahallas", indices=[Index("districtId")]) data class Mahalla(@PrimaryKey val id: Long, val districtId: Long, val name: String, val searchName: String)
@Entity(tableName="surveys", indices=[Index("createdAt"), Index("regionId", "districtId", "mahallaId"), Index("isDeleted")])
data class Survey(
 @PrimaryKey val id: String, val regionId: Long, val districtId: Long, val mahallaId: Long,
 val region: String, val district: String, val mahalla: String,
 val fio: String, val phone: String, val area: Double, val tree: String, val variety: String,
 val count: Int, val planting: String, val source: String,
 val payvandtag: String = "",
 val createdAt: Long, val updatedAt: Long, val syncStatus: String = "LOCAL", val isDeleted: Boolean = false,
 val searchText: String
)
@Dao interface ReferenceDao {
 @Query("SELECT checksum FROM reference_meta WHERE id=1") suspend fun checksum(): String?
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun setMeta(meta: ReferenceMeta)
 @Query("DELETE FROM mahallas") suspend fun clearMahallas()
 @Query("DELETE FROM districts") suspend fun clearDistricts()
 @Query("DELETE FROM regions") suspend fun clearRegions()
 @Query("SELECT * FROM regions ORDER BY name") fun regions(): Flow<List<Region>>
 @Query("SELECT * FROM districts WHERE regionId=:region ORDER BY name") fun districts(region: Long): Flow<List<District>>
 @Query("SELECT * FROM mahallas WHERE districtId=:district AND instr(searchName,:search)>0 ORDER BY name LIMIT 100 OFFSET :offset") suspend fun mahallas(district: Long, search: String, offset: Int): List<Mahalla>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun putRegions(rows: List<Region>)
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun putDistricts(rows: List<District>)
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun putMahallas(rows: List<Mahalla>)
}
@Dao interface SurveyDao {
 @Query("SELECT * FROM surveys WHERE isDeleted=0 AND instr(searchText,:search)>0 AND (:region=0 OR regionId=:region) AND (:district=0 OR districtId=:district) AND (:mahalla=0 OR mahallaId=:mahalla) AND (:tree='' OR tree=:tree) AND createdAt>=:start AND createdAt<:end ORDER BY createdAt DESC")
 fun observe(search: String, region: Long, district: Long, mahalla: Long, tree: String, start: Long, end: Long): Flow<List<Survey>>
 @Query("SELECT * FROM surveys WHERE id=:id") suspend fun get(id: String): Survey?
 @Upsert suspend fun save(row: Survey)
 @Query("UPDATE surveys SET isDeleted=1, updatedAt=:now, syncStatus='PENDING' WHERE id=:id") suspend fun delete(id: String, now: Long)
 @Query("SELECT * FROM surveys WHERE syncStatus!='SYNCED'") suspend fun pendingSync(): List<Survey>
 // Faqat server tasdiqlagan updatedAt hali ham joriy bo'lsa SYNCED belgilanadi;
 // shu oraliqda foydalanuvchi qayta tahrirlagan bo'lsa (updatedAt o'zgargan), PENDING holida qoladi.
 @Query("UPDATE surveys SET syncStatus='SYNCED' WHERE id=:id AND updatedAt=:expectedUpdatedAt") suspend fun markSynced(id: String, expectedUpdatedAt: Long)
}
@Database(entities=[Region::class,District::class,Mahalla::class,ReferenceMeta::class],version=1,exportSchema=true)
abstract class ReferenceDb: RoomDatabase() {
 abstract fun dao(): ReferenceDao
 suspend fun seed(context: Context) = withTransaction {
  val text=context.assets.open("reference.json").bufferedReader().use { it.readText() }
  val checksum=java.security.MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
  if(dao().checksum()==checksum)return@withTransaction
  val j=JSONObject(text)
  dao().clearMahallas();dao().clearDistricts();dao().clearRegions()
  val r=j.getJSONArray("regions"); dao().putRegions((0 until r.length()).map { val x=r.getJSONObject(it); Region(x.getLong("id"),x.getString("name")) })
  val d=j.getJSONArray("districts"); dao().putDistricts((0 until d.length()).map { val x=d.getJSONObject(it); District(x.getLong("id"),x.getLong("regionId"),x.getString("name")) })
  val m=j.getJSONArray("mahallas"); dao().putMahallas((0 until m.length()).map { val x=m.getJSONObject(it); Mahalla(x.getLong("id"),x.getLong("districtId"),x.getString("name"),normalize(x.getString("name"))) })
  dao().setMeta(ReferenceMeta(checksum=checksum))
 }
}
@Database(entities=[Survey::class],version=2,exportSchema=true)
abstract class SurveyDb: RoomDatabase() { abstract fun dao(): SurveyDao }

val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
 override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
  db.execSQL("ALTER TABLE surveys ADD COLUMN payvandtag TEXT NOT NULL DEFAULT ''")
 }
}

// Register explicit versioned migrations here when the schema changes. Never destroy survey data.
object Databases {
 val migrations: Array<androidx.room.migration.Migration> = arrayOf(MIGRATION_1_2)
 fun reference(c: Context)=Room.databaseBuilder(c,ReferenceDb::class.java,"reference.db").addMigrations(*migrations).build()
 fun surveys(c: Context)=Room.databaseBuilder(c,SurveyDb::class.java,"surveys.db").addMigrations(*migrations).fallbackToDestructiveMigration(true).build()
}
