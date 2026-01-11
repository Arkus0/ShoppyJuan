package com.arkus.shoppyjuan.domain.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.arkus.shoppyjuan.domain.model.ListItem
import com.arkus.shoppyjuan.domain.model.ShoppingList
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

sealed class ExportFormat {
    object PDF : ExportFormat()
    object PlainText : ExportFormat()
    object Markdown : ExportFormat()
}

sealed class ExportResult {
    data class Success(val uri: Uri, val filePath: String) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

@Singleton
class ListExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "ES"))
    private val fileNameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    suspend fun exportList(
        list: ShoppingList,
        items: List<ListItem>,
        format: ExportFormat
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            val fileName = generateFileName(list.name, format)
            val file = createExportFile(fileName, format)

            when (format) {
                ExportFormat.PDF -> exportToPdf(list, items, file)
                ExportFormat.PlainText -> exportToText(list, items, file)
                ExportFormat.Markdown -> exportToMarkdown(list, items, file)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            ExportResult.Success(uri, file.absolutePath)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Error al exportar")
        }
    }

    private fun generateFileName(listName: String, format: ExportFormat): String {
        val sanitizedName = listName.replace(Regex("[^a-zA-Z0-9áéíóúñÁÉÍÓÚÑ\\s]"), "")
            .replace(" ", "_")
            .take(30)
        val timestamp = fileNameFormat.format(Date())
        val extension = when (format) {
            ExportFormat.PDF -> "pdf"
            ExportFormat.PlainText -> "txt"
            ExportFormat.Markdown -> "md"
        }
        return "${sanitizedName}_$timestamp.$extension"
    }

    private fun createExportFile(fileName: String, format: ExportFormat): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "exports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, fileName)
    }

    private fun exportToPdf(list: ShoppingList, items: List<ListItem>, file: File) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var yPosition = 50f
        val leftMargin = 50f
        val pageWidth = 595f - 2 * leftMargin

        // Title paint
        val titlePaint = Paint().apply {
            color = Color.parseColor("#6200EE")
            textSize = 24f
            isFakeBoldText = true
        }

        // Subtitle paint
        val subtitlePaint = Paint().apply {
            color = Color.GRAY
            textSize = 12f
        }

        // Normal text paint
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
        }

        // Checked text paint (strikethrough)
        val checkedPaint = Paint().apply {
            color = Color.GRAY
            textSize = 14f
            isStrikeThruText = true
        }

        // Category paint
        val categoryPaint = Paint().apply {
            color = Color.parseColor("#6200EE")
            textSize = 16f
            isFakeBoldText = true
        }

        // Draw title
        canvas.drawText(list.name, leftMargin, yPosition, titlePaint)
        yPosition += 25f

        // Draw date
        canvas.drawText(
            "Exportado: ${dateFormat.format(Date())}",
            leftMargin,
            yPosition,
            subtitlePaint
        )
        yPosition += 30f

        // Draw statistics
        val checkedCount = items.count { it.checked }
        val totalCount = items.size
        canvas.drawText(
            "Completados: $checkedCount de $totalCount items",
            leftMargin,
            yPosition,
            subtitlePaint
        )
        yPosition += 30f

        // Draw separator line
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        canvas.drawLine(leftMargin, yPosition, pageWidth + leftMargin, yPosition, linePaint)
        yPosition += 20f

        // Group items by category
        val groupedItems = items.groupBy { it.category ?: "Sin categoría" }
            .toSortedMap()

        for ((category, categoryItems) in groupedItems) {
            // Check if we need a new page
            if (yPosition > 780) {
                document.finishPage(page)
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                val newPage = document.startPage(newPageInfo)
                yPosition = 50f
            }

            // Draw category header
            canvas.drawText(category, leftMargin, yPosition, categoryPaint)
            yPosition += 25f

            // Draw items in category
            for (item in categoryItems) {
                val paint = if (item.checked) checkedPaint else textPaint
                val checkbox = if (item.checked) "☑" else "☐"
                val emoji = item.emoji ?: ""
                val quantity = if (item.quantity != null && item.quantity > 0) {
                    " x${item.quantity}${item.unit?.let { " $it" } ?: ""}"
                } else ""

                val text = "$checkbox $emoji ${item.name}$quantity"
                canvas.drawText(text, leftMargin + 20f, yPosition, paint)
                yPosition += 20f

                // Check if we need a new page
                if (yPosition > 780) {
                    break
                }
            }

            yPosition += 10f // Space between categories
        }

        document.finishPage(page)

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    private fun exportToText(list: ShoppingList, items: List<ListItem>, file: File) {
        val sb = StringBuilder()

        sb.appendLine("═".repeat(50))
        sb.appendLine(list.name.uppercase())
        sb.appendLine("═".repeat(50))
        sb.appendLine()
        sb.appendLine("Exportado: ${dateFormat.format(Date())}")

        val checkedCount = items.count { it.checked }
        sb.appendLine("Completados: $checkedCount de ${items.size} items")
        sb.appendLine()
        sb.appendLine("─".repeat(50))
        sb.appendLine()

        // Group items by category
        val groupedItems = items.groupBy { it.category ?: "Sin categoría" }
            .toSortedMap()

        for ((category, categoryItems) in groupedItems) {
            sb.appendLine("▶ $category")
            sb.appendLine()

            for (item in categoryItems) {
                val checkbox = if (item.checked) "[x]" else "[ ]"
                val emoji = item.emoji?.let { "$it " } ?: ""
                val quantity = if (item.quantity != null && item.quantity > 0) {
                    " x${item.quantity}${item.unit?.let { " $it" } ?: ""}"
                } else ""

                sb.appendLine("  $checkbox $emoji${item.name}$quantity")
            }
            sb.appendLine()
        }

        sb.appendLine("─".repeat(50))
        sb.appendLine("Generado con ShoppyJuan")

        file.writeText(sb.toString())
    }

    private fun exportToMarkdown(list: ShoppingList, items: List<ListItem>, file: File) {
        val sb = StringBuilder()

        sb.appendLine("# ${list.name}")
        sb.appendLine()
        sb.appendLine("*Exportado: ${dateFormat.format(Date())}*")
        sb.appendLine()

        val checkedCount = items.count { it.checked }
        sb.appendLine("**Progreso:** $checkedCount/${items.size} items completados")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()

        // Group items by category
        val groupedItems = items.groupBy { it.category ?: "Sin categoría" }
            .toSortedMap()

        for ((category, categoryItems) in groupedItems) {
            sb.appendLine("## $category")
            sb.appendLine()

            for (item in categoryItems) {
                val checkbox = if (item.checked) "- [x]" else "- [ ]"
                val emoji = item.emoji?.let { "$it " } ?: ""
                val quantity = if (item.quantity != null && item.quantity > 0) {
                    " *(x${item.quantity}${item.unit?.let { " $it" } ?: ""})*"
                } else ""

                sb.appendLine("$checkbox $emoji**${item.name}**$quantity")
            }
            sb.appendLine()
        }

        sb.appendLine("---")
        sb.appendLine("*Generado con ShoppyJuan*")

        file.writeText(sb.toString())
    }

    fun createShareIntent(uri: Uri, format: ExportFormat): Intent {
        val mimeType = when (format) {
            ExportFormat.PDF -> "application/pdf"
            ExportFormat.PlainText -> "text/plain"
            ExportFormat.Markdown -> "text/markdown"
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
