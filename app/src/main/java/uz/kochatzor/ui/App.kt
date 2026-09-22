package uz.kochatzor.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.kochatzor.data.Survey
import uz.kochatzor.domain.Filters
import uz.kochatzor.util.Files
import java.time.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun App(vm:AppViewModel) {
 var tab by rememberSaveable {mutableIntStateOf(0)};var selected by rememberSaveable {mutableStateOf<String?>(null)};var qr by rememberSaveable {mutableStateOf<String?>(null)};var showSettings by rememberSaveable {mutableStateOf(false)}
 val all by vm.all.collectAsStateWithLifecycle();val busy by vm.busy.collectAsStateWithLifecycle();val ready by vm.ready.collectAsStateWithLifecycle()
 val snack=remember {SnackbarHostState()};val context=LocalContext.current
 LaunchedEffect(vm) {vm.events.collect {snack.showSnackbar(it)}}
 val file by vm.exportFile.collectAsStateWithLifecycle()
 val saveFile=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(Files.XLSX)) {uri->if(uri!=null) {val f=vm.exportFile.value;if(f!=null)vm.action {withContext(Dispatchers.IO) {context.contentResolver.openOutputStream(uri)?.use {out->f.inputStream().use {it.copyTo(out)}}?:error("Faylga yozib bo‘lmadi")};vm.exportFile.value=null;vm.notify("Excel saqlandi")}}}
 val names=listOf("Asosiy","Xonadon","Ko'chat","Baza","Skaner")
 val icons=listOf(Icons.Outlined.Home,Icons.Outlined.AddCircle,Icons.Outlined.Nature,Icons.AutoMirrored.Outlined.ListAlt,Icons.Outlined.QrCodeScanner)
 fun closeOverlay() {if(qr!=null)qr=null else if(selected!=null)selected=null else showSettings=false}
 BackHandler(selected!=null||qr!=null||showSettings) {closeOverlay()}

 AppBackground {
 Scaffold(
  containerColor = Color.Transparent,
  topBar={
   TopAppBar(
    title={
     Row(verticalAlignment = Alignment.CenterVertically) {
      Surface(
       shape = CircleShape,
       color = MaterialTheme.colorScheme.primaryContainer,
       modifier = Modifier.size(36.dp)
      ) {
       Box(contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.Eco, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
       }
      }
      Spacer(Modifier.width(10.dp))
      Column {
       Text("Ko‘chatzor", fontWeight=FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
       Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)) {
        Text("  OFFLINE  ", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 9.sp)
       }
      }
     }
    },
    navigationIcon={
     if(selected!=null||qr!=null||showSettings) {
      IconButton(onClick={closeOverlay()}){
       Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Orqaga")
      }
     }
    },
    actions={
     if(selected==null&&qr==null&&!showSettings) {
      IconButton(onClick={showSettings=true}){
       Icon(Icons.Outlined.Settings,"Sozlamalar")
      }
     }
    },
    colors = TopAppBarDefaults.topAppBarColors(
     containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
     scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    )
   )
  },
  snackbarHost={SnackbarHost(snack)},
  bottomBar={
   val isKeyboardVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
   if (!isKeyboardVisible) {
    Surface(tonalElevation = 0.dp, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)) {
     Column {
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
      NavigationBar(
       containerColor = Color.Transparent,
       tonalElevation = 0.dp
      ) {
       names.forEachIndexed {i,n->
        NavigationBarItem(
         selected=tab==i,
         onClick={tab=i;selected=null;qr=null;showSettings=false},
         icon={Icon(icons[i],n)},
         label={Text(n,maxLines=1,style=MaterialTheme.typography.labelSmall, fontWeight = if(tab==i) FontWeight.Bold else FontWeight.Normal)}
        )
       }
      }
     }
    }
   }
  }
 ) {padding->
  Column(Modifier.padding(padding).fillMaxSize().imePadding()) {
   if(busy || !ready)LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
   val qs=all.find {it.id==qr}
   val detailGroup=all.filter { "${it.mahallaId}_${it.fio}_${it.phone}" == selected }.takeIf { it.isNotEmpty() }
   when {
    showSettings -> Settings(vm)
    qs!=null -> QrScreen(qs,vm)
    detailGroup!=null -> Detail(detailGroup,vm,onEdit={vm.edit(it);selected=null;tab=2},onDelete={if(detailGroup.size<=1) selected=null},onQr={qr=it.id},onAddTree={vm.addTreeToXonadon(detailGroup.first());selected=null;tab=2})
    tab==0 -> Home(all,onEntry={tab=1},onRecords={tab=3})
    tab==1 -> XonadonScreen(vm,onGoToTree={tab=2})
    tab==2 -> KochatScreen(vm,onQr={qr=it.id},onGoToHousehold={tab=1})
    tab==3 -> Records(vm,onOpen={selected=it})
    else -> Scanner()
   }
  }
 }
 if(file!=null)AlertDialog(onDismissRequest={vm.exportFile.value=null},title={Text("Excel fayl tayyor")},text={Text(file!!.name)},confirmButton={TextButton(onClick={saveFile.launch(file!!.name)}){Text("Saqlash")}},dismissButton={Row {TextButton(onClick={vm.action {Files.share(context,file!!,Files.XLSX)};vm.exportFile.value=null}){Text("Ulashish")};TextButton(onClick={vm.exportFile.value=null}){Text("Yopish")}}})
 }
}

@Composable fun Home(rows:List<Survey>,onEntry:()->Unit,onRecords:()->Unit) {
 val today=Filters(period="Bugun").range();val week=Filters(period="Shu hafta").range()
 val totalTrees = rows.sumOf { it.count.toLong() }
 val uniqueHouseholds = remember(rows) { rows.groupBy { "${it.mahallaId}_${it.fio}_${it.phone}" }.size }

 LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(vertical=20.dp)) {
  item {
   val heroShape = RoundedCornerShape(32.dp)
   Box(
    Modifier
     .fillMaxWidth()
     .shadow(28.dp, heroShape, ambientColor = Color(0xFF00543B).copy(alpha = 0.30f), spotColor = Color(0xFF00543B).copy(alpha = 0.40f))
     .clip(heroShape)
     .background(Brush.linearGradient(colors = listOf(Color(0xFF00543B), Color(0xFF0C9163))))
     .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.04f))), heroShape)
     .padding(24.dp)
   ) {
    Column {
     Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Outlined.Forest, contentDescription = null, tint = Color(0xFF8AF5C6), modifier = Modifier.size(24.dp))
      Spacer(Modifier.width(8.dp))
      Text("UMUMIY STATISTIKA", style = MaterialTheme.typography.labelMedium, color = Color(0xFF8AF5C6), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
     }
     Spacer(Modifier.height(12.dp))
     Text("$totalTrees dona", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
     Text("Ro'yxatga olingan ko'chatlar", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
     Spacer(Modifier.height(16.dp))
     Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.15f)) {
      Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
       Icon(Icons.Outlined.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
       Spacer(Modifier.width(6.dp))
       Text("$uniqueHouseholds ta xonadon", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
      }
     }
    }
   }
  }

  item {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
    listOf("Bugun" to rows.count {it.createdAt>=today.first&&it.createdAt<today.second},"Hafta" to rows.count {it.createdAt>=week.first&&it.createdAt<week.second},"Jami" to rows.size).forEach {(name,n)->
     GlassCard(modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp)) {
      Column(Modifier.padding(16.dp)){
       Text(n.toString(),style=MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
       Text(name,style=MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
     }
    }
   }
  }

  item {
   Button(
    onClick=onEntry,
    modifier=Modifier.fillMaxWidth().heightIn(min = 56.dp),
    shape = RoundedCornerShape(20.dp),
    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp)
   ){
    Icon(Icons.Outlined.Add, contentDescription = null)
    Spacer(Modifier.width(8.dp))
    Text("Xonadon qo'shish", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
   }
  }
  
  item {
   Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Text("Meva turlari bo'yicha", style=MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("${rows.groupBy {it.tree}.size} xil", style=MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
   }
  }
  
  if(rows.isEmpty()) {
   item {
    GlassCard(shape = RoundedCornerShape(20.dp), tintAlpha = 0.45f) {
     Text("Hali ma'lumotlar yo‘q. Avval 'Xonadon qo'shish' bo'limi orqali xonadon va ko'chatlarni kiriting.", Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
   }
  } else {
   item {
    GlassCard(shape = RoundedCornerShape(24.dp)) {
     Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      val groupedTrees = rows.groupBy {it.tree}.entries.toList().sortedByDescending { it.value.sumOf { s->s.count } }
      groupedTrees.forEach { entry ->
       val count = entry.value.sumOf { it.count.toLong() }
       val progress = if (totalTrees > 0) count.toFloat() / totalTrees.toFloat() else 0f
       Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
         Text(entry.key, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
         Text("$count dona (${(progress * 100).toInt()}%)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
         progress = { progress },
         modifier = Modifier.fillMaxWidth().height(8.dp),
         color = MaterialTheme.colorScheme.primary,
         trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
         strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
       }
      }
     }
    }
   }
  }
  
  item {
   OutlinedButton(
    onClick=onRecords,
    modifier=Modifier.fillMaxWidth().heightIn(min = 52.dp),
    shape = RoundedCornerShape(20.dp)
   ){
    Icon(Icons.Outlined.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text("Barcha yozuvlarni ko'rish")
   }
  }
 }
}

@Composable fun Settings(vm:AppViewModel) {
 val theme by vm.theme.collectAsStateWithLifecycle()
 val defRegion by vm.defRegion.collectAsStateWithLifecycle()
 val defRegionName by vm.defRegionName.collectAsStateWithLifecycle()
 val defDistrict by vm.defDistrict.collectAsStateWithLifecycle()
 val defDistrictName by vm.defDistrictName.collectAsStateWithLifecycle()
 
 LazyColumn(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  item {Text("Sozlamalar",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)}
  
  // Theme Selector Card
  item {
   GlassCard(shape = RoundedCornerShape(24.dp), tintAlpha = 0.55f) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
     Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Outlined.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.width(8.dp))
      Text("Ilova ko'rinishi (Mavzu)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
     }
     
     val themeOptions = listOf(
      Triple("Tizim", "Tizim", Icons.Outlined.Contrast),
      Triple("Yorug‘", "Yorug‘ (Kun)", Icons.Outlined.LightMode),
      Triple("To‘q", "To‘q (Tun)", Icons.Outlined.DarkMode)
     )
     
     Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      themeOptions.forEach { (modeKey, label, icon) ->
       val isSelected = theme == modeKey
       Card(
        onClick = { vm.setTheme(modeKey) },
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
         containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
       ) {
        Column(
         modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp).fillMaxWidth(),
         horizontalAlignment = Alignment.CenterHorizontally,
         verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
         Icon(
          imageVector = icon,
          contentDescription = label,
          tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(22.dp)
         )
         Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
         )
        }
       }
      }
     }
    }
   }
  }
  
  item {
   GlassCard(shape = RoundedCornerShape(24.dp), tintAlpha = 0.55f) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
     Text("Doimiy hudud", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
     Text("Siz kiritadigan barcha xonadonlar uchun standart viloyat va tumanni belgilang.", style = MaterialTheme.typography.bodySmall)
     Address(vm, defRegion, defRegionName, defDistrict, defDistrictName, "", showMahalla = false,
        onRegion = { id, name -> vm.setLocation(id, name, 0L, "") }, 
        onDistrict = { id, name -> vm.setLocation(defRegion, defRegionName, id, name) }, 
        onMahalla = { _, _ -> }
     )
    }
   }
  }
  
  item {Info("Saqlash","Barcha yozuvlar shu telefondagi lokal bazada saqlanadi. Muhim ma'lumotlarni doim Excelga yuklab oling.")}
  item {Info("Versiya","1.0")}
 }
}

fun displayTime(time:Long)=Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
