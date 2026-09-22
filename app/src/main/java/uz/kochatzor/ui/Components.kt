package uz.kochatzor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.kochatzor.data.*

/** Frosted "glass" surface: translucent tint + soft tinted shadow + hairline light rim, used in place of plain Card across the app. */
@Composable fun GlassCard(
 modifier: Modifier = Modifier,
 shape: Shape = RoundedCornerShape(28.dp),
 tint: Color = MaterialTheme.colorScheme.surface,
 tintAlpha: Float = 0.68f,
 elevation: Dp = 18.dp,
 onClick: (() -> Unit)? = null,
 content: @Composable () -> Unit
) {
 val glow = MaterialTheme.colorScheme.primary
 Box(
  modifier
   .shadow(elevation, shape, ambientColor = glow.copy(alpha = 0.10f), spotColor = glow.copy(alpha = 0.18f))
   .clip(shape)
   .background(tint.copy(alpha = tintAlpha))
   .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.06f))), shape)
   .let { if (onClick != null) it.clickable(onClick = onClick) else it }
 ) { content() }
}

/** Soft pastel blobs behind translucent glass content — the light source the glass cards catch. */
@Composable fun AppBackground(content: @Composable BoxScope.() -> Unit) {
 val scheme = MaterialTheme.colorScheme
 Box(Modifier.fillMaxSize().background(scheme.background)) {
  Box(
   Modifier.align(Alignment.TopEnd).offset(x = 70.dp, y = (-90).dp).size(260.dp)
    .background(Brush.radialGradient(listOf(scheme.primaryContainer.copy(alpha = 0.55f), Color.Transparent)), CircleShape)
  )
  Box(
   Modifier.align(Alignment.CenterStart).offset(x = (-130).dp, y = 220.dp).size(240.dp)
    .background(Brush.radialGradient(listOf(scheme.secondaryContainer.copy(alpha = 0.45f), Color.Transparent)), CircleShape)
  )
  Box(
   Modifier.align(Alignment.BottomEnd).offset(x = 90.dp, y = 140.dp).size(300.dp)
    .background(Brush.radialGradient(listOf(scheme.tertiaryContainer.copy(alpha = 0.40f), Color.Transparent)), CircleShape)
  )
  content()
 }
}

@Composable fun Field(label:String,value:String,keyboard:KeyboardType=KeyboardType.Text,onChange:(String)->Unit) {
 val isPhone = keyboard == KeyboardType.Phone
 val vTrans = if (isPhone) androidx.compose.ui.text.input.VisualTransformation { text ->
  val t = text.text.filter { it.isDigit() }.take(9)
  var out = ""
  for (i in t.indices) {
   if (i==0) out += "("; if (i==2) out += ") "; if (i==5 || i==7) out += "-"
   out += t[i]
  }
  androidx.compose.ui.text.input.TransformedText(androidx.compose.ui.text.AnnotatedString(if(out.isNotEmpty()) "+998 $out" else ""), object : androidx.compose.ui.text.input.OffsetMapping {
   override fun originalToTransformed(offset: Int): Int = if (offset==0) 0 else out.length + 5
   override fun transformedToOriginal(offset: Int): Int = text.length
  })
 } else androidx.compose.ui.text.input.VisualTransformation.None

 val icon = when {
  "F.I.Sh" in label || "Ism" in label -> Icons.Outlined.Person
  "Phone" in label || "Telefon" in label -> Icons.Outlined.Phone
  "Yer" in label || "maydon" in label -> Icons.Outlined.SquareFoot
  "navi" in label -> Icons.Outlined.Category
  "soni" in label -> Icons.Outlined.FormatListNumbered
  "Payvandtag" in label -> Icons.Outlined.Spa
  else -> Icons.Outlined.Edit
 }

 OutlinedTextField(
  value = value,
  onValueChange = onChange,
  label = { Text(label) },
  modifier = Modifier.fillMaxWidth(),
  singleLine = true,
  keyboardOptions = KeyboardOptions(keyboardType=keyboard),
  visualTransformation = vTrans,
  leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
  shape = RoundedCornerShape(16.dp),
  colors = OutlinedTextFieldDefaults.colors(
   focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
   unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
  ),
  prefix = if(isPhone && value.isEmpty()) { { Text("+998 ") } } else null
 )
}

@Composable fun Choice(label:String,value:String,options:List<Pair<Long,String>>,enabled:Boolean=true,onSelect:(Long,String)->Unit) {
 var open by remember {mutableStateOf(false)};var query by rememberSaveable {mutableStateOf("")}
 
 OutlinedCard(
  onClick = { query=""; open=true },
  enabled = enabled,
  modifier = Modifier.fillMaxWidth().height(56.dp),
  shape = RoundedCornerShape(16.dp),
  colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
 ) {
  Row(
   Modifier.fillMaxSize().padding(horizontal = 16.dp),
   horizontalArrangement = Arrangement.SpaceBetween,
   verticalAlignment = Alignment.CenterVertically
  ) {
   Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
     if ("Viloyat" in label || "Tuman" in label) Icons.Outlined.LocationCity else Icons.Outlined.Nature,
     contentDescription = null,
     tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    )
    Spacer(Modifier.width(12.dp))
    Text(
     if(value.isBlank()) label else "$label: $value",
     style = MaterialTheme.typography.bodyLarge,
     fontWeight = if (value.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
     color = if(value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    )
   }
   Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }

 if(open) {
  Dialog(onDismissRequest={open=false}) {
   Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
     Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
     OutlinedTextField(
      value = query,
      onValueChange = { query = it },
      placeholder = { Text("Qidirish...") },
      modifier = Modifier.fillMaxWidth(),
      leadingIcon = { Icon(Icons.Outlined.Search, null) },
      singleLine = true,
      shape = RoundedCornerShape(16.dp)
     )
     LazyColumn(Modifier.heightIn(max=350.dp).fillMaxWidth()) {
      items(options.filter { normalize(it.second).contains(normalize(query)) }, key={it.first}) { p ->
       Surface(
        onClick = { onSelect(p.first, p.second); open = false },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        color = if (value == p.second) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
       ) {
        Text(
         p.second,
         Modifier.padding(16.dp),
         style = MaterialTheme.typography.bodyLarge,
         fontWeight = if (value == p.second) FontWeight.Bold else FontWeight.Normal
        )
       }
      }
     }
     TextButton(onClick={open=false}, modifier = Modifier.align(Alignment.End)) { Text("Yopish") }
    }
   }
  }
 }
}

@Composable fun MahallaChoice(vm:AppViewModel,district:Long,value:String,onSelect:(Long,String)->Unit) {
 var open by remember {mutableStateOf(false)};var query by rememberSaveable {mutableStateOf("")};var limit by remember {mutableIntStateOf(100)}
 var rows by remember {mutableStateOf<List<Mahalla>>(emptyList())};var loading by remember {mutableStateOf(false)};var more by remember {mutableStateOf(false)}
 
 LaunchedEffect(open,district,query,limit) {
  if(open) {
   loading=true
   try {
    rows=withContext(Dispatchers.IO) {
     buildList {
      var offset=0
      while(offset<limit) {
       val page=vm.ref.mahallas(district,normalize(query),offset)
       addAll(page)
       if(page.size<100)break
       offset+=100
      }
     }
    }
    more=rows.size==limit
   } catch(e:Exception) {
    vm.notify("MFYlarni yuklab bo‘lmadi")
   } finally {
    loading=false
   }
  }
 }

 OutlinedCard(
  onClick = { query=""; limit=100; open=true },
  enabled = district > 0,
  modifier = Modifier.fillMaxWidth().height(56.dp),
  shape = RoundedCornerShape(16.dp),
  colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
 ) {
  Row(
   Modifier.fillMaxSize().padding(horizontal = 16.dp),
   horizontalArrangement = Arrangement.SpaceBetween,
   verticalAlignment = Alignment.CenterVertically
  ) {
   Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
     Icons.Outlined.HomeWork,
     contentDescription = null,
     tint = if (district > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    )
    Spacer(Modifier.width(12.dp))
    Text(
     if(value.isBlank()) "MFYni tanlang" else "MFY: $value",
     style = MaterialTheme.typography.bodyLarge,
     fontWeight = if (value.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
     color = if(value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    )
   }
   Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }

 if(open) {
  Dialog(onDismissRequest={open=false}) {
   Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
     Text("MFYni tanlang", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
     OutlinedTextField(
      value = query,
      onValueChange = { query = it; limit = 100 },
      placeholder = { Text("MFY nomidan qidirish...") },
      modifier = Modifier.fillMaxWidth(),
      leadingIcon = { Icon(Icons.Outlined.Search, null) },
      singleLine = true,
      shape = RoundedCornerShape(16.dp)
     )
     if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
     LazyColumn(Modifier.heightIn(max=360.dp).fillMaxWidth()) {
      items(rows, key={it.id}) { r ->
       Surface(
        onClick = { onSelect(r.id, r.name); open = false },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        color = if (value == r.name) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
       ) {
        Text(
         r.name,
         Modifier.padding(16.dp),
         style = MaterialTheme.typography.bodyLarge,
         fontWeight = if (value == r.name) FontWeight.Bold else FontWeight.Normal
        )
       }
      }
      if(more) item { TextButton(onClick={limit+=100}, modifier=Modifier.fillMaxWidth()){ Text("Yana yuklash") } }
      if(rows.isEmpty() && !loading) item { Text("MFY topilmadi", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
     }
     TextButton(onClick={open=false}, modifier = Modifier.align(Alignment.End)) { Text("Yopish") }
    }
   }
  }
 }
}

@Composable fun Address(vm:AppViewModel,region:Long,regionName:String,district:Long,districtName:String,mahallaName:String,showMahalla:Boolean=true,onRegion:(Long,String)->Unit,onDistrict:(Long,String)->Unit,onMahalla:(Long,String)->Unit) {
 val regions by vm.regions.collectAsState();val flow=remember(region){vm.ref.districts(region)};val districts by flow.collectAsState(initial=emptyList())
 Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
  Choice("Viloyat",regionName,regions.map {it.id to it.name},onSelect=onRegion)
  Choice("Tuman/shahar",districtName,districts.map {it.id to it.name},region>0,onDistrict)
  if(showMahalla) MahallaChoice(vm,district,mahallaName,onMahalla)
 }
}

@Composable fun Info(label:String,value:String) { 
 Column(Modifier.fillMaxWidth().padding(vertical=4.dp)) {
  Text(label, style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
  Text(value, style=MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
 } 
}
