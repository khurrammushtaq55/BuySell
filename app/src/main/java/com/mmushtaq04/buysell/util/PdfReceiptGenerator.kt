package com.mmushtaq04.buysell.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

data class ReceiptData(
    val shopName: String,
    val shopPhone: String,
    val receiptNo: String,
    val dateText: String,
    val partyName: String,
    val partyPhone: String,
    val itemName: String,
    val imeiText: String,
    val totalAmountRs: Long,
    val paidAmountRs: Long,
    val footerNote: String = "Shukriya! Clean & clear deals."
)

object PdfReceiptGenerator {

    fun generateReceiptPdf(context: Context, data: ReceiptData): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 page
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        paint.color = Color.BLACK
        paint.textSize = 14f

        var y = 50f

        // Title / Header
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText(data.shopName, 40f, y, paint)
        y += 24f

        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Phone: ${data.shopPhone}", 40f, y, paint)
        y += 20f
        canvas.drawText("Receipt No: ${data.receiptNo} | Date: ${data.dateText}", 40f, y, paint)
        y += 30f

        // Line Divider
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 20f

        // Party Info
        paint.isFakeBoldText = true
        canvas.drawText("Customer / Party:", 40f, y, paint)
        paint.isFakeBoldText = false
        canvas.drawText("${data.partyName} (${data.partyPhone})", 180f, y, paint)
        y += 25f

        // Item Details
        paint.isFakeBoldText = true
        canvas.drawText("Item:", 40f, y, paint)
        paint.isFakeBoldText = false
        canvas.drawText("${data.itemName} [IMEI: ${data.imeiText}]", 180f, y, paint)
        y += 30f

        canvas.drawLine(40f, y, 555f, y, paint)
        y += 25f

        // Payment Details
        val totalFormatted = CurrencyFormatter.formatAmount(context, data.totalAmountRs)
        val paidFormatted = CurrencyFormatter.formatAmount(context, data.paidAmountRs)
        val remaining = (data.totalAmountRs - data.paidAmountRs).coerceAtLeast(0L)
        val remFormatted = CurrencyFormatter.formatAmount(context, remaining)

        canvas.drawText("Total Amount:", 40f, y, paint)
        canvas.drawText(totalFormatted, 400f, y, paint)
        y += 20f

        canvas.drawText("Paid Amount:", 40f, y, paint)
        canvas.drawText(paidFormatted, 400f, y, paint)
        y += 20f

        paint.isFakeBoldText = true
        canvas.drawText("Remaining Balance:", 40f, y, paint)
        canvas.drawText(remFormatted, 400f, y, paint)
        y += 40f

        // Footer
        paint.isFakeBoldText = false
        paint.textSize = 10f
        canvas.drawText(data.footerNote, 40f, y, paint)

        document.finishPage(page)

        val outputFile = File(context.cacheDir, "receipt_${data.receiptNo}.pdf")
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return outputFile
    }

    fun shareReceiptOnWhatsApp(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Receipt PDF"))
    }
}
