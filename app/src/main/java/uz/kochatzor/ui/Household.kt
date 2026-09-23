package uz.kochatzor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * "Xonadon" tab: create a household (owner + address) only.
 * Handing the just-saved household off to the "Ko'chat" tab goes through the shared
 * [AppViewModel.draft] rather than local state: [AppViewModel.saveHouseholdOnly] does not touch
 * the draft, so it still describes this household when [onGoToTree] switches tabs.
 */
@Composable fun XonadonScreen(vm:AppViewModel, onGoToTree:()->Unit) {
 val d by vm.draft.collectAsStateWithLifecycle()
 val busy by vm.busy.collectAsStateWithLifecycle()
 val saved by vm.lastSaved.collectAsStateWithLifecycle()
 val ready by vm.ready.collectAsStateWithLifecycle()

 LazyColumn(
  Modifier.fillMaxSize().padding(horizontal=20.dp),
  contentPadding=PaddingValues(top=16.dp, bottom=120.dp),
  verticalArrangement=Arrangement.spacedBy(16.dp)
 ) {
  item { Text("Xonadon qo'shish", style=MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }

  if (saved != null) {
   item {
    GlassCard(shape = RoundedCornerShape(24.dp), tint = MaterialTheme.colorScheme.primaryContainer, tintAlpha = 0.8f) {
     Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)){
      Text("Muvaffaqiyatli saqlandi!", style=MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
      Button(
       onClick = { vm.lastSaved.value = null; onGoToTree() },
       modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
       shape = RoundedCornerShape(16.dp)
      ){
       Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
       Spacer(Modifier.width(8.dp))
       Text("Ko'chat biriktirish")
      }
      OutlinedButton(
       onClick = { vm.newDraft(); vm.lastSaved.value = null },
       modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
       shape = RoundedCornerShape(16.dp)
      ){
       Text("Yangi xonadon qo‘shish")
      }
     }
    }
   }
  } else if (d.region == 0L || d.district == 0L) {
   item {
    GlassCard(shape = RoundedCornerShape(20.dp), tint = MaterialTheme.colorScheme.errorContainer, tintAlpha = 0.8f) {
     Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("Hudud tanlanmagan!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
      Text("Pastki o'ngdagi Sozlamalar bo'limidan viloyat va tumanni doimiy qilib biriktiring.", color = MaterialTheme.colorScheme.onErrorContainer)
     }
    }
   }
  } else {
   item { HouseholdForm(vm, d, busy, ready) }
  }
 }
}

/** Owner + address fields, used by [XonadonScreen]. */
@Composable private fun HouseholdForm(vm:AppViewModel, d:Draft, busy:Boolean, ready:Boolean) {
 GlassCard(shape = RoundedCornerShape(24.dp)) {
  Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
   Info("Hudud (Sozlamalardan)", "${d.regionName}, ${d.districtName}")
   MahallaChoice(vm, d.district, d.mahallaName) { id, n -> vm.update(d.copy(mahalla=id, mahallaName=n)) }
   Field("Xonadon egasining F.I.Sh.",d.fio) { vm.update(d.copy(fio=it)) }
   Field("Telefon raqami",d.phone,KeyboardType.Phone) { vm.update(d.copy(phone=it)) }
   Field("Yer maydoni, ga",d.area,KeyboardType.Decimal) { vm.update(d.copy(area=it)) }

   Button(
    onClick = { vm.saveHouseholdOnly() },
    enabled = !busy && ready,
    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
    shape = RoundedCornerShape(16.dp)
   ) { Text(if(busy) "Saqlanmoqda…" else "Saqlash", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }
  }
 }
}
