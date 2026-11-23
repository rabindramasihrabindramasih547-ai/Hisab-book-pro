package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.entity.WorkerEntity
import com.example.domain.ReportType
import com.example.ui.components.DateInputRow
import com.example.ui.components.EmptyStateCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.LocalHisabColors
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyPrimary
import com.example.util.ExportManager
import com.example.util.IndiaFormatters
import com.example.util.ReportTable
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
    workers: List<WorkerEntity>,
    onBuildReport: (ReportType, Long, Long, Long?, AppStrings) -> ReportTable
) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val todayKey = IndiaFormatters.todayDateKey()
    var selectedType by remember { mutableStateOf(ReportType.DAILY) }
    var startDateText by remember { mutableStateOf(IndiaFormatters.formatDateKey(todayKey)) }
    var endDateText by remember { mutableStateOf(IndiaFormatters.formatDateKey(todayKey)) }
    var selectedWorkerId by remember { mutableStateOf<Long?>(null) }

    val startKey = IndiaFormatters.parseDdMmYyyyToDateKey(startDateText) ?: todayKey
    val endKey = IndiaFormatters.parseDdMmYyyyToDateKey(endDateText) ?: todayKey
    val minKey = minOf(startKey, endKey)
    val maxKey = maxOf(startKey, endKey)

    val reportTable = onBuildReport(selectedType, minKey, maxKey, selectedWorkerId, strings)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Report Type Selector
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, extra.cardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.reportsTitle, style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val types = listOf(
                            ReportType.DAILY to strings.reportDaily,
                            ReportType.WEEKLY to strings.reportWeekly,
                            ReportType.MONTHLY to strings.reportMonthly,
                            ReportType.WORKER_WISE to strings.reportWorkerWise,
                            ReportType.ATTENDANCE to strings.reportAttendance,
                            ReportType.ADVANCE to strings.reportAdvance,
                            ReportType.PAYMENT to strings.reportPayment,
                            ReportType.OUTSTANDING to strings.reportOutstanding
                        )
                        types.forEach { (type, label) ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = {
                                    selectedType = type
                                    when (type) {
                                        ReportType.DAILY -> {
                                            startDateText = IndiaFormatters.formatDateKey(todayKey)
                                            endDateText = IndiaFormatters.formatDateKey(todayKey)
                                        }
                                        ReportType.WEEKLY -> {
                                            startDateText = IndiaFormatters.formatDateKey(IndiaFormatters.startOfWeekDateKey(todayKey))
                                            endDateText = IndiaFormatters.formatDateKey(todayKey)
                                        }
                                        ReportType.MONTHLY -> {
                                            startDateText = IndiaFormatters.formatDateKey(IndiaFormatters.startOfMonthDateKey(todayKey))
                                            endDateText = IndiaFormatters.formatDateKey(IndiaFormatters.endOfMonthDateKey(todayKey))
                                        }
                                        else -> {
                                            startDateText = IndiaFormatters.formatDateKey(IndiaFormatters.startOfMonthDateKey(todayKey))
                                            endDateText = IndiaFormatters.formatDateKey(todayKey)
                                        }
                                    }
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("report_chip_${type.name}")
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateInputRow(
                            label = strings.fromDate,
                            dateText = startDateText,
                            onDateTextChange = { startDateText = it },
                            modifier = Modifier.weight(1f)
                        )
                        DateInputRow(
                            label = strings.toDate,
                            dateText = endDateText,
                            onDateTextChange = { endDateText = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (selectedType == ReportType.WORKER_WISE && workers.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedWorkerId == null,
                                onClick = { selectedWorkerId = null },
                                label = { Text(strings.allCategories) }
                            )
                            workers.forEach { w ->
                                FilterChip(
                                    selected = selectedWorkerId == w.id,
                                    onClick = { selectedWorkerId = w.id },
                                    label = { Text(w.fullName) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Export Actions Card
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, extra.cardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val pdf = ExportManager.generatePdf(context, reportTable, "hisab_report")
                                    ExportManager.shareFile(context, pdf, "application/pdf", reportTable.title)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier.testTag("export_pdf_btn")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.exportPdf, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val xls = ExportManager.generateExcelXml(context, reportTable, "hisab_report")
                                    ExportManager.shareFile(context, xls, "application/vnd.ms-excel", reportTable.title)
                                }
                            },
                            modifier = Modifier.testTag("export_excel_btn")
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.exportExcel)
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val csv = ExportManager.generateCsv(context, reportTable, "hisab_report")
                                    ExportManager.shareFile(context, csv, "text/csv", reportTable.title)
                                }
                            },
                            modifier = Modifier.testTag("export_csv_btn")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.exportCsv)
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val pdf = ExportManager.generatePdf(context, reportTable, "hisab_whatsapp")
                                    val msg = buildString {
                                        appendLine("*${reportTable.businessName}*")
                                        appendLine(reportTable.title)
                                        appendLine(reportTable.subtitle)
                                        reportTable.summaryLines.forEach { (k, v) ->
                                            appendLine("$k: $v")
                                        }
                                    }
                                    ExportManager.shareOnWhatsApp(context, msg, pdf, "application/pdf")
                                }
                            },
                            modifier = Modifier.testTag("share_whatsapp_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.shareWhatsapp)
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val pdf = ExportManager.generatePdf(context, reportTable, "hisab_print")
                                    ExportManager.printPdfFile(context, pdf, reportTable.title)
                                }
                            },
                            modifier = Modifier.testTag("print_report_btn")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.printReport)
                        }
                    }
                }
            }
        }

        // Report Summary Banner
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = NavyDeep)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(reportTable.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(reportTable.subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                    reportTable.summaryLines.forEach { (k, v) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(k, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                            Text(v, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = extra.gold)
                        }
                    }
                }
            }
        }

        // Report Preview Rows
        if (reportTable.rows.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Assessment,
                    title = strings.noTransactionsYet,
                    subtitle = reportTable.subtitle
                )
            }
        } else {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        reportTable.headers.forEach { h ->
                            Text(
                                text = h,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            items(reportTable.rows.indices.toList(), key = { "rep_row_$it" }) { idx ->
                val row = reportTable.rows[idx]
                Card(
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, extra.cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { cell ->
                            Text(
                                text = cell,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
