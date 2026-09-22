package uz.kochatzor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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

/**
 * "Ko'chat" tab: attach a sapling to a household. The target household travels through the
 * shared [AppViewModel.draft] instead of local tab state, because Compose drops a tab's local
 * state when the user switches away — [AppViewModel.edit] and [AppViewModel.addTreeToXonadon]
 * (called from the Detail screen or the Xonadon tab's success panel) and the picker below all
 * write into that same draft.
 */
@Composable fun KochatScreen(vm:AppViewModel, onQr:(Survey)->Unit, onGoToHousehold:()->Unit) {
 val d by vm.draft.collectAsStateWithLifecycle()
 val busy by vm.busy.collectAsStateWithLifecycle()
 val saved by vm.lastSaved.collectAsStateWithLifecycle()
 val ready by vm.ready.collectAsStateWithLifecycle()
 val allSurveys by vm.all.collectAsStateWithLifecycle()

 val households = remember(allSurveys) {
  allSurveys.groupBy { "${it.mahallaId}_${it.fio}_${it.phone}" }.values.map { it.first() }
 }
 var showHouseholdPicker by remember { mutableStateOf(false) }

 val treeTypeState = rememberSaveable { mutableStateOf("") }; var treeType by treeTypeState
 val treeVarietyState = rememberSaveable { mutableStateOf("") }; var treeVariety by treeVarietyState
 val treePayvandtagState = rememberSaveable { mutableStateOf("") }; var treePayvandtag by treePayvandtagState
 val treeCountState = rememberSaveable { mutableStateOf("") }; var treeCount by treeCountState
 val treePlantingState = rememberSaveable { mutableStateOf("") }; var treePlanting by treePlantingState
 val treeSourceState = rememberSaveable { mutableStateOf("") }; var treeSource by treeSourceState
 val customSourceDetailState = rememberSaveable { mutableStateOf("") }; var customSourceDetail by customSourceDetailState

 LaunchedEffect(d.id) {
  treeType = d.tree; treeVariety = d.variety; treePayvandtag = d.payvandtag
  treeCount = d.count; treePlanting = d.planting; treeSource = d.source
 }

 val household: Survey? = if (d.fio.isBlank()) null else Survey(
  id = d.id, regionId = d.region, districtId = d.district, mahallaId = d.mahalla,
  region = d.regionName, district = d.districtName, mahalla = d.mahallaName,
  fio = d.fio, phone = d.phone, area = d.area.toDoubleOrNull() ?: 0.0,
  tree = d.tree, variety = d.variety, count = d.count.toIntOrNull() ?: 0,
  planting = d.planting, source = d.source, payvandtag = d.payvandtag,
  createdAt = 0, updatedAt = 0, searchText = ""
 )

 LazyColumn(
  Modifier.fillMaxSize().padding(horizontal=20.dp),
  contentPadding=PaddingValues(top=16.dp, bottom=120.dp),
  verticalArrangement=Arrangement.spacedBy(16.dp)
 ) {
  item { Text("Ko'chat biriktirish", style=MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }

  if (saved != null) {
   item {
    GlassCard(shape = RoundedCornerShape(24.dp), tint = MaterialTheme.colorScheme.primaryContainer, tintAlpha = 0.8f) {
     Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)){
      Text("Muvaffaqiyatli saqlandi!", style=MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
      Button(
       onClick = {
        treeType=""; treeVariety=""; treePayvandtag=""; treeCount=""; treePlanting=""; treeSource=""; customSourceDetail=""
        vm.lastSaved.value = null
       },
       modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
       shape = RoundedCornerShape(16.dp)
      ){
       Icon(Icons.Outlined.Add, null)
       Spacer(Modifier.width(8.dp))
       Text("Yana ko'chat qo'shish")
      }
      OutlinedButton(
       onClick = { onQr(saved!!) },
       modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
       shape = RoundedCornerShape(16.dp)
      ){
       Icon(Icons.Outlined.QrCode, null)
       Spacer(Modifier.width(8.dp))
       Text("QR kodni ko‘rish")
      }
      TextButton(
       onClick = { vm.newDraft(); vm.lastSaved.value = null },
       modifier = Modifier.fillMaxWidth()
      ){
       Text("Boshqa xonadon")
      }
     }
    }
   }
  } else {
   item {
    OutlinedCard(
     onClick = { showHouseholdPicker = true },
     modifier = Modifier.fillMaxWidth().height(60.dp),
     shape = RoundedCornerShape(20.dp),
     colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
    ) {
     Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Row(verticalAlignment = Alignment.CenterVertically) {
       Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.primary)
       Spacer(Modifier.width(12.dp))
       Text(
        if(household == null) "Xonadonni tanlang" else "Xonadon: ${household.fio}",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = if(household != null) FontWeight.Bold else FontWeight.Normal
       )
      }
      Icon(Icons.Outlined.ArrowDropDown, "")
     }
    }
   }

   if (household != null) {
    item {
     GlassCard(shape = RoundedCornerShape(20.dp), tint = MaterialTheme.colorScheme.secondaryContainer, tintAlpha = 0.8f) {
      Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
       Text(household.fio, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
       Text("Manzil: ${household.district}, ${household.mahalla}", style = MaterialTheme.typography.bodyMedium)
       Text("Tel: ${household.phone} • Yer: ${household.area} ga", style = MaterialTheme.typography.bodySmall)
      }
     }
    }
    item {
     TreeForm(
      vm, household, busy, ready,
      treeTypeState, treeVarietyState, treePayvandtagState, treeCountState, treePlantingState, treeSourceState, customSourceDetailState,
      onSave = {
       val finalSource = if (treeSource.isNotBlank() && customSourceDetail.isNotBlank()) "$treeSource ($customSourceDetail)" else treeSource
       vm.saveTreeForHousehold(household, treeType, treeVariety, treePayvandtag, treeCount, treePlanting, finalSource)
      }
     )
    }
   } else {
    item {
     GlassCard(shape = RoundedCornerShape(20.dp), tintAlpha = 0.45f) {
      Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
       Text("Avval yuqoridagi tugma orqali xonadonni tanlang.", style = MaterialTheme.typography.bodyMedium)
       if (households.isEmpty()) {
        OutlinedButton(onClick = onGoToHousehold, modifier = Modifier.fillMaxWidth()) { Text("Xonadon qo'shish") }
       }
      }
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
         vm.addTreeToXonadon(item)
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
       item { Text("Xonadon topilmadi. Avval uni qo'shing.", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium) }
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

/** Sapling fields for the currently targeted household [h], used by [KochatScreen]. */
@Composable private fun TreeForm(
 vm:AppViewModel, h:Survey, busy:Boolean, ready:Boolean,
 treeType:MutableState<String>, treeVariety:MutableState<String>, treePayvandtag:MutableState<String>,
 treeCount:MutableState<String>, treePlanting:MutableState<String>, treeSource:MutableState<String>,
 customSourceDetail:MutableState<String>, onSave:()->Unit
) {
 GlassCard(shape = RoundedCornerShape(24.dp)) {
  Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
   Choice("Meva turi", treeType.value, trees.mapIndexed { i, s -> i.toLong() to s }) { _, s -> treeType.value = s }
   Field("Meva navi", treeVariety.value) { treeVariety.value = it }
   Field("Payvandtag", treePayvandtag.value) { treePayvandtag.value = it }
   Field("Ko‘chat soni, dona", treeCount.value, KeyboardType.Number) { treeCount.value = it }

   val currentYear = java.time.Year.now().value
   val years = (currentYear downTo 1990).map { it.toLong() to it.toString() }
   Choice("Ekilgan yili", treePlanting.value, years) { _, s -> treePlanting.value = s }

   val sources = listOf(
    "In-Vitro laboratoriyasi",
    "Mahalliy ko‘chatchilik xo‘jaliklari",
    "Xorijiy korxonalar",
    "Savdo nuqtalari (bozor, do‘kon va boshqa sotuv joylari)"
   )
   Choice("Ko‘chat manbasi", treeSource.value, sources.mapIndexed { i, s -> i.toLong() to s }) { _, s -> treeSource.value = s }

   if (treeSource.value.isNotBlank()) {
    Field("Manba tafsilotlari (nomi, joyi)", customSourceDetail.value) { customSourceDetail.value = it }
   }

   Button(
    onClick = onSave,
    enabled = !busy && ready,
    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
    shape = RoundedCornerShape(16.dp)
   ) { Text(if(busy) "Saqlanmoqda…" else "Biriktirish", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }
  }
 }
}
