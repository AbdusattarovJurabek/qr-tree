package uz.kochatzor.util
import uz.kochatzor.data.Survey
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Streaming OOXML package: images are embedded in xl/media, linked by drawing relationships. */
object Xlsx {
 private fun esc(s:String)=s.filter { it=='\n' || it=='\t' || it=='\r' || it.code>=32 }.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
 fun write(rows:List<Survey>,output:OutputStream,png:(Survey)->ByteArray={Qr.png(Qr.payload(it))}) {
  ZipOutputStream(output).use { z ->
   fun entry(name:String,text:String) { z.putNextEntry(ZipEntry(name)); z.write(text.toByteArray(Charsets.UTF_8)); z.closeEntry() }
   val ns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
   val rel="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
   val pkg="http://schemas.openxmlformats.org/package/2006/relationships"
   entry("[Content_Types].xml","""<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Default Extension="png" ContentType="image/png"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/><Override PartName="/xl/drawings/drawing1.xml" ContentType="application/vnd.openxmlformats-officedocument.drawing+xml"/></Types>""")
   entry("_rels/.rels","""<Relationships xmlns="$pkg"><Relationship Id="rId1" Type="$rel/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
   entry("xl/workbook.xml","""<workbook xmlns="$ns" xmlns:r="$rel"><sheets><sheet name="Ko‘chatzor" sheetId="1" r:id="rId1"/></sheets></workbook>""")
   entry("xl/_rels/workbook.xml.rels","""<Relationships xmlns="$pkg"><Relationship Id="rId1" Type="$rel/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="$rel/styles" Target="styles.xml"/></Relationships>""")
   
   // Premium Emerald Styles
   entry("xl/styles.xml","""<styleSheet xmlns="$ns"><fonts count="3"><font><sz val="11"/><name val="Segoe UI"/></font><font><b/><sz val="11"/><color rgb="FFFFFF"/><name val="Segoe UI"/></font><font><b/><sz val="11"/><color rgb="006C4C"/><name val="Segoe UI"/></font></fonts><fills count="4"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="006C4C"/><bgColor indexed="64"/></patternFill></fill><fill><patternFill patternType="solid"><fgColor rgb="F2F8F4"/><bgColor indexed="64"/></patternFill></fill></fills><borders count="2"><border/><border><left style="thin"><color rgb="D0D7D3"/></left><right style="thin"><color rgb="D0D7D3"/></right><top style="thin"><color rgb="D0D7D3"/></top><bottom style="thin"><color rgb="D0D7D3"/></bottom></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="9"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf><xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyAlignment="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf><xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf><xf numFmtId="0" fontId="0" fillId="3" borderId="1" xfId="0" applyAlignment="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf><xf numFmtId="0" fontId="0" fillId="3" borderId="1" xfId="0" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf><xf numFmtId="0" fontId="2" fillId="0" borderId="1" xfId="0" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf><xf numFmtId="0" fontId="2" fillId="3" borderId="1" xfId="0" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf><xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyAlignment="1"><alignment horizontal="right" vertical="center" wrapText="1"/></xf></cellXfs><cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>""")
   
   z.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
   fun raw(s:String) { z.write(s.toByteArray(Charsets.UTF_8)) }
   raw("""<worksheet xmlns="$ns" xmlns:r="$rel"><sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews><cols>""")
   listOf(7,22,24,26,34,20,16,20,20,18,14,16,14,14,28,18).forEachIndexed { i,n->raw("""<col min="${i+1}" max="${i+1}" width="$n" customWidth="1"/>""") }; raw("</cols><sheetData>")
   
   fun rowStyled(n:Int, values:List<Pair<Any, Int>>, isHeader:Boolean=false) {
    raw("""<row r="$n" ht="${if(isHeader) 36 else 80}" customHeight="1">""")
    values.forEachIndexed { i, (v, style) ->
     val ref="${'A'+i}$n"
     if(v is Number) raw("""<c r="$ref" s="$style"><v>$v</v></c>""")
     else raw("""<c r="$ref" s="$style" t="inlineStr"><is><t xml:space="preserve">${esc(v.toString())}</t></is></c>""")
    }
    raw("</row>")
   }

   // Header Row (style 1 = Emerald background, White bold text)
   val headers = listOf("T/r","Viloyat nomi","Tuman yoki shahar","MFY nomi","Xonadon egasi F.I.Sh.","Telefon raqami","Yer maydoni, ga","Ekilgan ko‘chat turi","Ko‘chat navi","Payvandtag","Soni, dona","Ekilgan yili","Ko‘chat manbasi","Kenglik","Uzunlik","QR kod")
   rowStyled(1, headers.map { it to 1 }, isHeader = true)

   // Data Rows with alternating Zebra striping and alignment
   rows.forEachIndexed { i, s ->
    val isEven = i % 2 == 0
    val leftStyle = if (isEven) 2 else 4
    val centerStyle = if (isEven) 3 else 5
    val boldCenterStyle = if (isEven) 6 else 7
    val rightStyle = 8

    val rowData = listOf(
     (i + 1) to centerStyle,
     s.region to leftStyle,
     s.district to leftStyle,
     s.mahalla to leftStyle,
     s.fio to leftStyle,
     s.phone to centerStyle,
     s.area to rightStyle,
     s.tree to leftStyle,
     s.variety to leftStyle,
     (if (s.payvandtag.isNotBlank()) s.payvandtag else "-") to leftStyle,
     s.count to boldCenterStyle,
     s.planting to centerStyle,
     s.source to leftStyle,
     (if (s.latitude != 0.0 || s.longitude != 0.0) s.latitude else "-") to centerStyle,
     (if (s.latitude != 0.0 || s.longitude != 0.0) s.longitude else "-") to centerStyle,
     "" to centerStyle
    )
    rowStyled(i + 2, rowData, isHeader = false)
   }

   raw("""</sheetData><autoFilter ref="A1:P${rows.size+1}"/><drawing r:id="rId1"/></worksheet>""");z.closeEntry()
   entry("xl/worksheets/_rels/sheet1.xml.rels","""<Relationships xmlns="$pkg"><Relationship Id="rId1" Type="$rel/drawing" Target="../drawings/drawing1.xml"/></Relationships>""")
   entry("xl/drawings/drawing1.xml",buildString {
    append("""<xdr:wsDr xmlns:xdr="http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="$rel">""")
    rows.indices.forEach { i->append("""<xdr:oneCellAnchor><xdr:from><xdr:col>15</xdr:col><xdr:colOff>47625</xdr:colOff><xdr:row>${i+1}</xdr:row><xdr:rowOff>47625</xdr:rowOff></xdr:from><xdr:ext cx="952500" cy="952500"/><xdr:pic><xdr:nvPicPr><xdr:cNvPr id="${i+1}" name="QR ${i+1}"/><xdr:cNvPicPr/></xdr:nvPicPr><xdr:blipFill><a:blip r:embed="rId${i+1}"/><a:stretch><a:fillRect/></a:stretch></xdr:blipFill><xdr:spPr><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></xdr:spPr></xdr:pic><xdr:clientData/></xdr:oneCellAnchor>""") };append("</xdr:wsDr>")
   })
   entry("xl/drawings/_rels/drawing1.xml.rels",buildString { append("""<Relationships xmlns="$pkg">"""); rows.indices.forEach { i->append("""<Relationship Id="rId${i+1}" Type="$rel/image" Target="../media/qr${i+1}.png"/>""") };append("</Relationships>") })
   rows.forEachIndexed { i,s->z.putNextEntry(ZipEntry("xl/media/qr${i+1}.png")); z.write(png(s));z.closeEntry() }
  }
 }
}
