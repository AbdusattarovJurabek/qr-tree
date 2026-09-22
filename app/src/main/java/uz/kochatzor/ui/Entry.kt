package uz.kochatzor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.kochatzor.data.Survey
import uz.kochatzor.data.normalize

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun Entry(vm:AppViewModel,onQr:(Survey)->Unit) {
 val d by vm.draft.collectAsStateWithLifecycle()
 val busy by vm.busy.collectAsStateWithLifecycle()
 val saved by vm.lastSaved.collectAsStateWithLifecycle()
 val ready by vm.ready.collectAsStateWithLifecycle()
 val allSurveys by vm.all.collectAsStateWithLifecycle()
 
 // Unique households list
 val households = remember(allSurveys) {
  allSurveys.groupBy { "${it.mahallaId}_${it.fio}_${it.phone}" }.values.map { it.first() }
 }

 var mode by rememberSaveable { mutableIntStateOf(1) } // 1: Xonadon qo'shish, 2: Ko'chat biriktirish
 var selectedHousehold by remember { mutableStateOf<Survey?>(null) }
 
 var treeType by rememberSaveable { mutableStateOf("") }
 var treeVariety by rememberSaveable { mutableStateOf("") }
 var treePayvandtag by rememberSaveable { mutableStateOf("") }
 var treeCount by rememberSaveable { mutableStateOf("") }
 var treePlanting by rememberSaveable { mutableStateOf("") }
 var treeSource by rememberSaveable { mutableStateOf("") }
 var customSourceDetail by rememberSaveable { mutableStateOf("") }
 var showHouseholdPicker by remember { mutableStateOf(false) }

 LaunchedEffect(d.id) {
  if (d.id.isNotBlank()) {
   mode = 2
   selectedHousehold = Survey(
    id = d.id, regionId = d.region, districtId = d.district, mahallaId = d.mahalla,
    region = d.regionName, district = d.districtName, mahalla = d.mahallaName,
    fio = d.fio, phone = d.phone, area = d.area.toDoubleOrNull() ?: 0.0,
    tree = d.tree, variety = d.variety, count = d.count.toIntOrNull() ?: 0,
    planting = d.planting, source = d.source, payvandtag = d.payvandtag,
    createdAt = 0, updatedAt = 0, searchText = ""
   )
   treeType = d.tree
   treeVariety = d.variety
   treePayvandtag = d.payvandtag
   treeCount = d.count
   treePlanting = d.planting
   treeSource = d.source
  }
 }

 LazyColumn(
  Modifier.fillMaxSize().padding(horizontal=20.dp),
  contentPadding=PaddingValues(top=16.dp, bottom=120.dp),
  verticalArrangement=Arrangement.spacedBy(16.dp)
 ) {
  // Mode Selector Tabs
  item {
   Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
   ) {
    SecondaryTabRow(
     selectedTabIndex = mode - 1, 
     modifier = Modifier.fillMaxWidth(),
     containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) {
     Tab(
      selected = mode == 1, 
      onClick = { mode = 1 }, 
      text = { 
       Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Home, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("1. Xonadon", fontWeight = FontWeight.Bold)
       }
      }
     )
     Tab(
      selected = mode == 2, 
      onClick = { mode = 2 }, 
      text = { 
       Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Nature, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("2. Ko'chat", fontWeight = FontWeight.Bold)
       }
      }
     )
    }
   }
  }

  if (saved != null) {
   item {
    Card(
     colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
     shape = RoundedCornerShape(24.dp)
    ) {
     Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)){
      Text("🎉 Muvaffaqiyatli saqlandi!", style=MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
      
      if (mode == 1) {
       Button(
        onClick = { 
         selectedHousehold = saved
         mode = 2
         vm.lastSaved.value = null
        }, 
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp)
       ){
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
        Spacer(Modifier.width(8.dp))
        Text("Shu xonadonga ko'chat biriktirish")
       }
       OutlinedButton(
        onClick = { 
         vm.newDraft()
         vm.lastSaved.value = null
        }, 
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp)
       ){
        Text("Yangi xonadon qo‘shish")
       }
      } else {
       Button(
        onClick = { 
         treeType = ""; treeVariety = ""; treePayvandtag = ""; treeCount = ""; treePlanting = ""; treeSource = ""; customSourceDetail = ""
         vm.lastSaved.value = null
        }, 
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp)
       ){
        Icon(Icons.Outlined.Add, null)
        Spacer(Modifier.width(8.dp))
        Text("Shu xonadonga YANA ko'chat qo'shish")
       }
       OutlinedButton(
        onClick = { onQr(saved!!) }, 
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp)
       ){
        Icon(Icons.Outlined.QrCode, null)
        Spacer(Modifier.width(8.dp))
        Text("QR kodni ko‘rish")
       }
       TextButton(
        onClick = { 
         selectedHousehold = null
         vm.lastSaved.value = null
        }, 
        modifier = Modifier.fillMaxWidth()
       ){
        Text("Boshqa xonadonga o'tish")
       }
      }
     }
    }
   }
  } else if (mode == 1) {
   // MODE 1: XONADON KIRITISH
   item { Text("Xonadon Qo'shish", style=MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
   
   if (d.region == 0L || d.district == 0L) {
    item {
     Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(20.dp)) {
      Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
       Text("Hudud tanlanmagan!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
       Text("Pastki o'ngdagi Sozlamalar bo'limidan viloyat va tumanni doimiy qilib biriktiring.", color = MaterialTheme.colorScheme.onErrorContainer)
      }
     }
    }
   } else {
    item {
     Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
     ) {
      Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
       Info("Hudud (Sozlamalardan)", "${d.regionName}, ${d.districtName}")
       MahallaChoice(vm, d.district, d.mahallaName) { id, n -> vm.update(d.copy(mahalla=id, mahallaName=n)) }
       Field("Xonadon egasining F.I.Sh.",d.fio) { vm.update(d.copy(fio=it)) }
       Field("Telefon raqami",d.phone,KeyboardType.Phone) { vm.update(d.copy(phone=it)) }
       Field("Yer maydoni, ga",d.area,KeyboardType.Decimal) { vm.update(d.copy(area=it)) }
       
       Button(
        onClick = { vm.saveHouseholdOnly() }, 
        enabled = !busy && ready,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp)
       ) { Text(if(busy) "Saqlanmoqda…" else "XONADONNI SAQLASH ✔", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }
      }
     }
    }
   }
  } else {
   // MODE 2: KO'CHAT BIRIKTIRISH
   item { Text("Xonadonga Ko'chat Biriktirish", style=MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
   
   item {
    OutlinedCard(
     onClick = { showHouseholdPicker = true },
     modifier = Modifier.fillMaxWidth().height(60.dp),
     shape = RoundedCornerShape(20.dp),
     colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
     Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Row(verticalAlignment = Alignment.CenterVertically) {
       Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.primary)
       Spacer(Modifier.width(12.dp))
       Text(
        if(selectedHousehold == null) "Xonadonni tanlang" else "Xonadon: ${selectedHousehold!!.fio}", 
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = if(selectedHousehold != null) FontWeight.Bold else FontWeight.Normal
       )
      }
      Icon(Icons.Outlined.ArrowDropDown, "")
     }
    }
   }
   
   if (selectedHousehold != null) {
    val h = selectedHousehold!!
    item {
     Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(20.dp)) {
      Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
       Text(h.fio, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
       Text("Manzil: ${h.district}, ${h.mahalla}", style = MaterialTheme.typography.bodyMedium)
       Text("Tel: ${h.phone} • Yer: ${h.area} ga", style = MaterialTheme.typography.bodySmall)
      }
     }
    }
    
    item {
     Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
     ) {
      Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
       Choice("Meva turi", treeType, trees.mapIndexed { i, s -> i.toLong() to s }) { _, s -> treeType = s }
       Field("Meva navi", treeVariety) { treeVariety = it }
       Field("Payvandtag", treePayvandtag) { treePayvandtag = it }
       Field("Ko‘chat soni, dona", treeCount, KeyboardType.Number) { treeCount = it }
       
       val currentYear = java.time.Year.now().value
       val years = (currentYear downTo 1990).map { it.toLong() to it.toString() }
       Choice("Ekilgan yili", treePlanting, years) { _, s -> treePlanting = s } 
       
       val sources = listOf(
        "In-Vitro laboratoriyasi",
        "Mahalliy ko‘chatchilik xo‘jaliklari",
        "Xorijiy korxonalar",
        "Savdo nuqtalari (bozor, do‘kon va boshqa sotuv joylari)"
       )
       Choice("Ko‘chat manbasi", treeSource, sources.mapIndexed { i, s -> i.toLong() to s }) { _, s -> treeSource = s } 
       
       if (treeSource.isNotBlank()) {
        Field("Manba bo'yicha batafsil (Nomi, joyi va h.k.)", customSourceDetail) { customSourceDetail = it }
       }
       
       Button(
        onClick = { 
         val finalSource = if (treeSource.isNotBlank() && customSourceDetail.isNotBlank()) "$treeSource ($customSourceDetail)" else treeSource
         vm.saveTreeForHousehold(h, treeType, treeVariety, treePayvandtag, treeCount, treePlanting, finalSource)
        }, 
        enabled = !busy && ready,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp)
       ) { Text(if(busy) "Saqlanmoqda…" else "KO'CHATNI BIRIKTIRISH ✔", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }
      }
     }
    }
   } else {
    item {
     Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp)) {
      Text("Ko'chat biriktirish uchun yuqoridagi 'Xonadonni tanlang' tugmasi orqali xonadonni tanlang.", Modifier.padding(20.dp), style = MaterialTheme.typography.bodyMedium)
     }
    }
   }
  }
 }

 // Household Picker Dialog
 if (showHouseholdPicker) {
  var searchQuery by remember { mutableStateOf("") }
  val filteredHouseholds = households.filter { 
   normalize(it.fio).contains(normalize(searchQuery)) || normalize(it.mahalla).contains(normalize(searchQuery)) || it.phone.contains(searchQuery)
  }
  
  Dialog(onDismissRequest = { showHouseholdPicker = false }) {
   Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
    Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
     Text("Xonadonni tanlang", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
     OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("F.I.Sh., MFY yoki tel bo'yicha...") },
      modifier = Modifier.fillMaxWidth(),
      leadingIcon = { Icon(Icons.Outlined.Search, "") },
      singleLine = true,
      shape = RoundedCornerShape(16.dp)
     )
     LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      items(filteredHouseholds, key = { "${it.mahallaId}_${it.fio}_${it.phone}" }) { item ->
       Surface(
        onClick = { 
         selectedHousehold = item
         mode = 2
         showHouseholdPicker = false
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
       ) {
        Column(Modifier.padding(16.dp)) {
         Text(item.fio, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
         Text("${item.mahalla} • ${item.phone}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
       }
      }
      if (filteredHouseholds.isEmpty()) {
       item { Text("Xonadonlar topilmadi. Avval '1. Xonadon qo'shish' bo'limida xonadonni saqlang.", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium) }
      }
     }
     TextButton(onClick = { showHouseholdPicker = false }, modifier = Modifier.align(Alignment.End)) {
      Text("Yopish")
     }
    }
   }
  }
 }
}
