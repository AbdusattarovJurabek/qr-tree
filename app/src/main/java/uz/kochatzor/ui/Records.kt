package uz.kochatzor.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.kochatzor.data.Survey
import uz.kochatzor.domain.Filters

@Composable fun Records(vm:AppViewModel,onOpen:(String)->Unit) {
 val rows by vm.records.collectAsStateWithLifecycle();val filter by vm.filters.collectAsStateWithLifecycle();val busy by vm.busy.collectAsStateWithLifecycle()
 var show by rememberSaveable {mutableStateOf(false)}
 
 val grouped = remember(rows) { rows.groupBy { "${it.mahallaId}_${it.fio}_${it.phone}" }.values.toList() }

 LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),contentPadding=PaddingValues(top=16.dp, bottom=100.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  item {Text("Xonadonlar bazasi",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)}
  
  // Search & Filter Section
  item {
   Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
    OutlinedTextField(
        value = filter.query, 
        onValueChange = { vm.applyFilters(filter.copy(query=it)) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Qidirish (Ism, MFY, tuman, tur, nav)") },
        leadingIcon = { Icon(Icons.Outlined.Search, "Qidiruv") },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
     FilledTonalButton(onClick={show=true}, modifier=Modifier.weight(1f), shape = MaterialTheme.shapes.large) {
      Icon(Icons.Outlined.FilterAlt, "Filtrlar", modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(6.dp))
      Text("Filtrlar • ${filter.period}")
     }
    }
   }
  }
  
  // Stats
  item {
   GlassCard(shape = MaterialTheme.shapes.large, tint = MaterialTheme.colorScheme.secondaryContainer, tintAlpha = 0.8f) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
     Column {
      Text("Xonadonlar", style=MaterialTheme.typography.labelMedium)
      Text("${grouped.size} ta", style=MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
     }
     Column(horizontalAlignment = Alignment.End) {
      Text("Ko'chatlar", style=MaterialTheme.typography.labelMedium)
      Text("${rows.sumOf {it.count.toLong()}} dona", style=MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
     }
    }
   }
  }
  
  // Export Buttons
  item {
   Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    OutlinedButton(onClick={vm.export(true)},enabled=!busy,modifier=Modifier.weight(1f), shape = MaterialTheme.shapes.large){
     Icon(Icons.Outlined.FileDownload, "", Modifier.size(18.dp))
     Spacer(Modifier.width(4.dp))
     Text("Barchasi")
    }
    Button(onClick={vm.export(false)},enabled=!busy&&rows.isNotEmpty(),modifier=Modifier.weight(1f), shape = MaterialTheme.shapes.large){
     Icon(Icons.Outlined.FilterAlt, "", Modifier.size(18.dp))
     Spacer(Modifier.width(4.dp))
     Text("Filtrlangan")
    }
   }
  }
  
  if(grouped.isEmpty()) {
   item { 
    Column(Modifier.fillMaxWidth().padding(vertical=40.dp), horizontalAlignment=Alignment.CenterHorizontally) {
     Icon(Icons.Outlined.SearchOff, "Topilmadi", Modifier.size(64.dp), tint=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f))
     Spacer(Modifier.height(16.dp))
     Text("Ma'lumot topilmadi", style=MaterialTheme.typography.titleMedium)
    }
   }
  }
  
  items(grouped,key={it.first().id}) {g->
   val s = g.first()
   GlassCard(
       modifier=Modifier.fillMaxWidth(),
       shape=MaterialTheme.shapes.large,
       onClick={onOpen("${s.mahallaId}_${s.fio}_${s.phone}")}
   ) {
    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
     Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
      Text(s.fio,style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Bold)
      Surface(shape=MaterialTheme.shapes.small, color=MaterialTheme.colorScheme.primaryContainer) {
       Text("${g.size} xil meva", Modifier.padding(horizontal=8.dp, vertical=4.dp), style=MaterialTheme.typography.labelMedium, color=MaterialTheme.colorScheme.onPrimaryContainer)
      }
     }
     
     Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Outlined.LocationOn, "", Modifier.size(14.dp), tint=MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(Modifier.width(4.dp))
      Text("${s.district}, ${s.mahalla}",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
     }
     
     HorizontalDivider(Modifier.padding(vertical=4.dp), color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f))
     
     Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Column(Modifier.weight(1f)) {
       Text("Meva turlari", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
       Text(g.map { it.tree }.distinct().take(3).joinToString(", ") + if(g.map{it.tree}.distinct().size>3) "..." else "", style=MaterialTheme.typography.bodyMedium)
      }
      Column(horizontalAlignment = Alignment.End) {
       Text("Umumiy", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
       Text("${g.sumOf { it.count }} ta", style=MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color=MaterialTheme.colorScheme.primary)
      }
     }
    }
   }
  }
 }
 if(show)FilterDialog(vm,filter){show=false}
}

@Composable fun FilterDialog(vm:AppViewModel,current:Filters,close:()->Unit) {
 var f by remember {mutableStateOf(current)};var rn by rememberSaveable {mutableStateOf(f.regionName)};var dn by rememberSaveable {mutableStateOf(f.districtName)};var mn by rememberSaveable {mutableStateOf(f.mahallaName)}
 Dialog(onDismissRequest=close) {
  Surface(shape=MaterialTheme.shapes.extraLarge, color=MaterialTheme.colorScheme.surface) {
   Column(Modifier.fillMaxWidth().heightIn(max=620.dp).padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
    Text("Qidiruv filtrlari",style=MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Choice("Davr",f.period,listOf("Barchasi","Bugun","Kecha","Shu hafta","O‘tgan hafta","Shu oy","O‘tgan oy","Sana oralig‘i").mapIndexed {i,s->i.toLong() to s}){_,s->f=f.copy(period=s)}
    if(f.period=="Sana oralig‘i") {Field("Boshlanish: YYYY-MM-DD",f.from){f=f.copy(from=it)};Field("Tugash: YYYY-MM-DD",f.to){f=f.copy(to=it)}}
    Address(vm,f.region,rn,f.district,dn,mn, showMahalla = true, {id,n->rn=n;dn="";mn="";f=f.copy(region=id,district=0,mahalla=0,regionName=n,districtName="",mahallaName="")},{id,n->dn=n;mn="";f=f.copy(district=id,mahalla=0,districtName=n,mahallaName="")},{id,n->mn=n;f=f.copy(mahalla=id,mahallaName=n)})
    Choice("Ko‘chat turi",f.tree,(listOf("Barchasi")+trees).mapIndexed {i,s->i.toLong() to s}){_,s->f=f.copy(tree=if(s=="Barchasi")"" else s)}
    
    Spacer(Modifier.height(8.dp))
    Button(onClick={if(vm.applyFilters(f))close()},modifier=Modifier.fillMaxWidth().heightIn(min = 50.dp)){Text("Qo‘llash", fontWeight = FontWeight.Bold)}
    TextButton(onClick={f=Filters(query=current.query);rn="";dn="";mn=""}, modifier=Modifier.fillMaxWidth()){Text("Filtrlarni tozalash")}
    TextButton(onClick=close, modifier=Modifier.fillMaxWidth()){Text("Bekor qilish", color=MaterialTheme.colorScheme.onSurfaceVariant)}
   }
  }
 }
}

@Composable fun Detail(group:List<Survey>,vm:AppViewModel,onEdit:(Survey)->Unit,onDelete:(Survey)->Unit,onQr:(Survey)->Unit,onAddTree:()->Unit) {
 val s = group.first()
 var deleting by remember {mutableStateOf<Survey?>(null)}
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),contentPadding=PaddingValues(vertical=20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  item {
   GlassCard(modifier=Modifier.fillMaxWidth(), shape=MaterialTheme.shapes.extraLarge, tint = MaterialTheme.colorScheme.primaryContainer, tintAlpha = 0.8f) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
     Text(s.fio,style=MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color=MaterialTheme.colorScheme.onPrimaryContainer)
     Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Outlined.Phone, "", Modifier.size(16.dp), tint=MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.8f))
      Spacer(Modifier.width(8.dp))
      Text(s.phone, color=MaterialTheme.colorScheme.onPrimaryContainer, fontWeight=FontWeight.Medium)
     }
     Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Outlined.LocationOn, "", Modifier.size(16.dp), tint=MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.8f))
      Spacer(Modifier.width(8.dp))
      Text("${s.region}, ${s.district}, ${s.mahalla}", color=MaterialTheme.colorScheme.onPrimaryContainer)
     }
     Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Outlined.Map, "", Modifier.size(16.dp), tint=MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.8f))
      Spacer(Modifier.width(8.dp))
      Text("Yer maydoni: ${s.area} ga", color=MaterialTheme.colorScheme.onPrimaryContainer)
     }
     
     Spacer(Modifier.height(8.dp))
     val context = androidx.compose.ui.platform.LocalContext.current
     FilledTonalButton(
      onClick = {
       group.forEach { tree -> uz.kochatzor.util.Printer.printCard(context, tree) }
      },
      modifier = Modifier.fillMaxWidth().height(48.dp)
     ) {
      Icon(Icons.Outlined.Print, "", Modifier.size(18.dp))
      Spacer(Modifier.width(8.dp))
      Text("Printerda chop etish")
     }
    }
   }
  }
  
  item {
   Button(onClick=onAddTree, modifier=Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
    Icon(Icons.Outlined.Add, "")
    Spacer(Modifier.width(8.dp))
    Text("Yangi ko'chat qo'shish")
   }
  }
  
  item {
   Text("Mavjud ko'chatlar (${group.size} xil turi)", style=MaterialTheme.typography.titleMedium, fontWeight=FontWeight.Bold, color=MaterialTheme.colorScheme.primary, modifier=Modifier.padding(top=8.dp))
  }
  
  items(group, key={it.id}) { tree ->
   GlassCard(shape=MaterialTheme.shapes.large) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
     Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text(tree.tree, style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Bold, color=MaterialTheme.colorScheme.primary)
      Surface(shape=MaterialTheme.shapes.small, color=MaterialTheme.colorScheme.secondaryContainer) {
       Text("${tree.count} dona", Modifier.padding(horizontal=8.dp, vertical=4.dp), style=MaterialTheme.typography.labelMedium, color=MaterialTheme.colorScheme.onSecondaryContainer, fontWeight=FontWeight.Bold)
      }
     }
     
     Row(Modifier.fillMaxWidth()) {
      Column(Modifier.weight(1f)) { Info("Navi",tree.variety) }
      Column(Modifier.weight(1f)) { Info("Payvandtag",if(tree.payvandtag.isNotBlank()) tree.payvandtag else "-") }
     }
     Row(Modifier.fillMaxWidth()) {
      Column(Modifier.weight(1f)) { Info("Ekilgan yili",tree.planting) }
      Column(Modifier.weight(1f)) { Info("Manbasi",tree.source) }
     }
     
     HorizontalDivider(Modifier.padding(vertical=8.dp), color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f))
     
     val context = androidx.compose.ui.platform.LocalContext.current
     Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      TextButton(onClick={onEdit(tree)}, contentPadding = PaddingValues(horizontal = 8.dp)){Text("Tahrirlash")}
      TextButton(onClick={onQr(tree)}, contentPadding = PaddingValues(horizontal = 8.dp)){Text("QR kod")}
      IconButton(onClick={uz.kochatzor.util.Printer.printCard(context, tree)}) { Icon(Icons.Outlined.Print, "", tint=MaterialTheme.colorScheme.primary) }
      IconButton(onClick={deleting=tree}) { Icon(Icons.Outlined.Delete, "", tint=MaterialTheme.colorScheme.error) }
     }
    }
   }
  }
 }
 if(deleting != null) {
  val target = deleting!!
  AlertDialog(onDismissRequest={deleting=null},title={Text("Ko'chatni o‘chirish")},text={Text("Siz tanlagan '${target.tree}' ko'chati butunlay o‘chib ketadi.")},confirmButton={TextButton(onClick={val t = deleting!!; deleting=null; vm.delete(t, { onDelete(t) })}){Text("O‘chirish", color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton(onClick={deleting=null}){Text("Bekor qilish")}})
 }
}