package uz.kochatzor.util

import android.content.Context
import android.graphics.Bitmap
import androidx.print.PrintHelper
import uz.kochatzor.data.Survey

object Printer {
    fun printCard(context: Context, s: Survey) {
        val size = 600
        val qrPayload = Qr.payload(s)
        val qrBitmap = Qr.bitmap(qrPayload, size)

        val printHelper = PrintHelper(context).apply {
            scaleMode = PrintHelper.SCALE_MODE_FIT
        }
        printHelper.printBitmap("QR_${s.fio}", qrBitmap)
    }
}
