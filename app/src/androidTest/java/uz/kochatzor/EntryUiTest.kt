package uz.kochatzor

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

/** UI flow test using UI Automator public input APIs. Requires an unlocked test device. */
@RunWith(AndroidJUnit4::class)
class EntryUiTest {
 @Test fun enterSaveAndShowQr() {
  val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
  device.wakeUp();device.pressMenu()
  ActivityScenario.launch(MainActivity::class.java).use { scenario ->
   scenario.onActivity {
    it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    if(android.os.Build.VERSION.SDK_INT>=27) {it.setShowWhenLocked(true);it.setTurnScreenOn(true)}
    if(android.os.Build.VERSION.SDK_INT>=26) it.getSystemService(android.app.KeyguardManager::class.java).requestDismissKeyguard(it,null)
   }
   fun tap(text:String) {val obj=device.wait(Until.findObject(By.text(text)),10000);if(obj==null) { val dump=java.io.ByteArrayOutputStream();device.dumpWindowHierarchy(dump);error("Missing: $text; ${dump.toString().take(5000)}") };obj.click();device.waitForIdle()}
   fun scrollTo(text:String) {if(!device.hasObject(By.text(text))) UiScrollable(UiSelector().scrollable(true)).scrollTextIntoView(text)}
   fun input(label:String,value:String) {scrollTo(label);val node=device.wait(Until.findObject(By.text(label)),5000);assertNotNull("Missing field: $label",node);if(node.className=="android.widget.EditText")node.text=value else {val parent=node.parent;parent.text=value};device.waitForIdle()}
   tap("Kiritish")
   tap("Viloyat  ▾");tap("Farg‘ona viloyati")
   tap("Tuman/shahar  ▾");tap("Quvasoy shahar")
   tap("MFYni tanlang  ▾");tap("Soy bo‘yi MFY")
   input("Xonadon egasining F.I.Sh.","Sinov Xonadon Egasi")
   input("Telefon raqami","+998901234567")
   input("Yer maydoni, ga","0.1")
   scrollTo("Ko‘chat turi  ▾");tap("Ko‘chat turi  ▾");tap("Olma")
   input("Ko‘chat navi","Golden")
   input("Ko‘chat soni, dona","20")
   input("Ekish sanasi: YYYY yoki YYYY-MM-DD","2026")
   input("Ko‘chat manbasi","Quva agro star MChJ")
   scenario.recreate();device.waitForIdle()
   scrollTo("SAQLASH");tap("SAQLASH")
   scrollTo("QR kodni ko‘rish");tap("QR kodni ko‘rish")
   assertTrue(device.wait(Until.hasObject(By.text("Xonadon QR kodi")),5000))
   device.takeScreenshot(java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,"qr-screen.png"))
  }
 }
}
