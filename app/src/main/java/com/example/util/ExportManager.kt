package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class ReportTable(
    val title: String,
    val subtitle: String,
    val businessName: String,
    val ownerContact: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val summaryLines: List<Pair<String, String>>
)

object ExportManager {

    private fun exportsDir(context: Context): File =
        File(context.cacheDir, "exports").apply { mkdirs() }

    private fun fileUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    suspend fun generatePdf(context: Context, table: ReportTable, filePrefix: String): File =
        withContext(Dispatchers.IO) {
            val file = File(exportsDir(context), "${filePrefix}_${System.currentTimeMillis()}.pdf")
            val pdf = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 32f

            val navyPaint = Paint().apply { color = Color.rgb(11, 25, 44) }
            val goldLinePaint = Paint().apply {
                color = Color.rgb(212, 148, 26)
                strokeWidth = 2f
            }
            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subHeaderTextPaint = Paint().apply {
                color = Color.rgb(220, 230, 242)
                textSize = 10f
            }
            val titlePaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = Paint().apply {
                color = Color.rgb(30, 41, 59)
                textSize = 9.5f
            }
            val boldBodyPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val tableHeaderBg = Paint().apply { color = Color.rgb(234, 240, 246) }
            val dividerPaint = Paint().apply {
                color = Color.rgb(203, 213, 225)
                strokeWidth = 0.8f
            }

            var pageNumber = 1
            var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = page.canvas

            fun drawHeader() {
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 74f, navyPaint)
                canvas.drawLine(0f, 74f, pageWidth.toFloat(), 74f, goldLinePaint)
                canvas.drawText(table.businessName, margin, 32f, headerTextPaint)
                val rightInfo = "INR (₹) | ${IndiaFormatters.formatDateKey(IndiaFormatters.todayDateKey())}"
                canvas.drawText(rightInfo, pageWidth - margin - 130f, 32f, subHeaderTextPaint)
                if (table.ownerContact.isNotBlank()) {
                    canvas.drawText(table.ownerContact, margin, 52f, subHeaderTextPaint)
                }
            }

            drawHeader()
            var y = 102f
            canvas.drawText(table.title, margin, y, titlePaint)
            y += 16f
            if (table.subtitle.isNotBlank()) {
                canvas.drawText(table.subtitle, margin, y, bodyPaint)
                y += 18f
            }

            val colCount = table.headers.size.coerceAtLeast(1)
            val usableWidth = pageWidth - margin * 2
            val colWidth = usableWidth / colCount

            fun drawTableHeader(currentY: Float): Float {
                canvas.drawRect(margin, currentY - 12f, pageWidth - margin, currentY + 8f, tableHeaderBg)
                table.headers.forEachIndexed { idx, h ->
                    val text = h.take(18)
                    canvas.drawText(text, margin + idx * colWidth + 4f, currentY + 1f, boldBodyPaint)
                }
                return currentY + 20f
            }

            y = drawTableHeader(y)

            table.rows.forEach { row ->
                if (y > pageHeight - 90f) {
                    pdf.finishPage(page)
                    pageNumber++
                    page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    canvas = page.canvas
                    drawHeader()
                    y = drawTableHeader(100f)
                }
                row.forEachIndexed { idx, cell ->
                    val clipped = if (cell.length > 20) cell.take(19) + "…" else cell
                    canvas.drawText(clipped, margin + idx * colWidth + 4f, y, bodyPaint)
                }
                canvas.drawLine(margin, y + 6f, pageWidth - margin, y + 6f, dividerPaint)
                y += 18f
            }

            if (table.summaryLines.isNotEmpty()) {
                if (y > pageHeight - (table.summaryLines.size * 20f + 45f)) {
                    pdf.finishPage(page)
                    pageNumber++
                    page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    canvas = page.canvas
                    drawHeader()
                    y = 102f
                }
                y += 12f
                canvas.drawLine(margin, y, pageWidth - margin, y, goldLinePaint)
                y += 18f
                table.summaryLines.forEach { (label, value) ->
                    canvas.drawText(label, margin, y, boldBodyPaint)
                    canvas.drawText(value, pageWidth - margin - 140f, y, boldBodyPaint)
                    y += 18f
                }
            }

            pdf.finishPage(page)
            FileOutputStream(file).use { out -> pdf.writeTo(out) }
            pdf.close()
            file
        }

    suspend fun generateCsv(context: Context, table: ReportTable, filePrefix: String): File =
        withContext(Dispatchers.IO) {
            val file = File(exportsDir(context), "${filePrefix}_${System.currentTimeMillis()}.csv")
            val sb = StringBuilder()
            // UTF-8 BOM for clean Devanagari & ₹ rendering in Excel
            sb.append('\uFEFF')
            fun esc(v: String): String = "\"${v.replace("\"", "\"\"")}\""
            sb.appendLine(esc(table.businessName) + "," + esc(table.title))
            if (table.subtitle.isNotBlank()) sb.appendLine(esc(table.subtitle))
            sb.appendLine()
            sb.appendLine(table.headers.joinToString(",") { esc(it) })
            table.rows.forEach { row ->
                sb.appendLine(row.joinToString(",") { esc(it) })
            }
            if (table.summaryLines.isNotEmpty()) {
                sb.appendLine()
                table.summaryLines.forEach { (k, v) ->
                    sb.appendLine("${esc(k)},${esc(v)}")
                }
            }
            file.writeText(sb.toString(), Charsets.UTF_8)
            file
        }

    suspend fun generateExcelXml(context: Context, table: ReportTable, filePrefix: String): File =
        withContext(Dispatchers.IO) {
            val file = File(exportsDir(context), "${filePrefix}_${System.currentTimeMillis()}.xls")
            fun xmlEsc(s: String): String = s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")

            val sb = StringBuilder()
            sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            sb.appendLine("""<?mso-application progid="Excel.Sheet"?>""")
            sb.appendLine("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">""")
            sb.appendLine("""<Styles><Style ss:ID="Header"><Font ss:Bold="1" ss:Color="#FFFFFF"/><Interior ss:Color="#0B192C" ss:Pattern="Solid"/></Style><Style ss:ID="Bold"><Font ss:Bold="1"/></Style></Styles>""")
            sb.appendLine("""<Worksheet ss:Name="HisabBookReport"><Table>""")
            sb.appendLine("""<Row><Cell ss:StyleID="Bold"><Data ss:Type="String">${xmlEsc(table.businessName)} - ${xmlEsc(table.title)}</Data></Cell></Row>""")
            if (table.subtitle.isNotBlank()) {
                sb.appendLine("""<Row><Cell><Data ss:Type="String">${xmlEsc(table.subtitle)}</Data></Cell></Row>""")
            }
            sb.append("<Row>")
            table.headers.forEach { h ->
                sb.append("""<Cell ss:StyleID="Header"><Data ss:Type="String">${xmlEsc(h)}</Data></Cell>""")
            }
            sb.appendLine("</Row>")
            table.rows.forEach { row ->
                sb.append("<Row>")
                row.forEach { cell ->
                    sb.append("""<Cell><Data ss:Type="String">${xmlEsc(cell)}</Data></Cell>""")
                }
                sb.appendLine("</Row>")
            }
            table.summaryLines.forEach { (k, v) ->
                sb.appendLine("""<Row><Cell ss:StyleID="Bold"><Data ss:Type="String">${xmlEsc(k)}</Data></Cell><Cell ss:StyleID="Bold"><Data ss:Type="String">${xmlEsc(v)}</Data></Cell></Row>""")
            }
            sb.appendLine("</Table></Worksheet></Workbook>")
            file.writeText(sb.toString(), Charsets.UTF_8)
            file
        }

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = fileUri(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun shareOnWhatsApp(context: Context, message: String, attachment: File? = null, mimeType: String = "application/pdf") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, message)
            if (attachment != null) {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri(context, attachment))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
            setPackage("com.whatsapp")
        }
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, message)
                if (attachment != null) {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, fileUri(context, attachment))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }
            }
            val chooser = Intent.createChooser(fallback, "Share Report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    fun printPdfFile(context: Context, pdfFile: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(pdfFile.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }
}
