package com.example.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.model.AdherenceLog
import com.example.data.model.DoctorAppointment
import com.example.data.model.DoctorBrief
import com.example.data.model.HealthMetricsRecord
import com.example.data.model.MedicalReport
import com.example.data.model.MedicineReminder
import com.example.data.model.MoodEntry
import com.example.data.model.UserProfile
import com.example.i18n.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportSection(
    val heading: String,
    val lines: List<String>
)

data class ExportableHealthReport(
    val suggestedFileName: String,
    val title: String,
    val subtitle: String,
    val generatedDate: String,
    val disclaimer: String,
    val sections: List<ExportSection>
)

object HealthReportExporter {

    private fun nowFormatted(): String =
        SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US).format(Date())

    private fun fileStamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())

    fun buildSingleMedicalReport(
        report: MedicalReport,
        profile: UserProfile?,
        language: AppLanguage
    ): ExportableHealthReport {
        val isBn = language == AppLanguage.BN
        val safeTitleSlug = report.title.replace(Regex("[^a-zA-Z0-9]+"), "_").trim('_').take(24).ifBlank { "report" }
        val patientLines = buildList {
            add("${if (isBn) "নাম" else "Patient"}: ${profile?.displayName?.ifBlank { "ShasthoSathi User" } ?: "ShasthoSathi User"}")
            if (profile != null) {
                add("${if (isBn) "বয়স" else "Age"}: ${profile.age} | ${if (isBn) "অবস্থান" else "Location"}: ${profile.location.ifBlank { "-" }}")
                if (profile.emergencyNumber.isNotBlank()) {
                    add("${if (isBn) "জরুরি নম্বর" else "Emergency Contact"}: ${profile.emergencyNumber}")
                }
            }
        }
        val metaLines = buildList {
            add("${if (isBn) "সিরিয়াল নম্বর" else "Serial Number"}: #${report.serialNumber}")
            add("${if (isBn) "রিপোর্টের ধরন" else "Category"}: ${report.category}")
            add("${if (isBn) "তারিখ" else "Report Date"}: ${report.reportDate}")
            if (report.doctorOrLab.isNotBlank()) {
                add("${if (isBn) "ডাক্তার / ল্যাব" else "Doctor / Lab"}: ${report.doctorOrLab}")
            }
            if (report.attachmentName.isNotBlank() || report.attachmentType != "TEXT") {
                add("${if (isBn) "সংযুক্ত ফাইল" else "Attachment"}: ${report.attachmentType} ${if (report.attachmentName.isNotBlank()) "(${report.attachmentName})" else ""}")
            }
        }
        val sections = buildList {
            add(ExportSection(if (isBn) "রোগীর তথ্য" else "Patient Profile", patientLines))
            add(ExportSection(if (isBn) "রিপোর্ট পরিচিতি" else "Report Metadata", metaLines))
            add(ExportSection(if (isBn) "মূল সারসংক্ষেপ" else "Key Findings Summary", listOf(report.summary.ifBlank { "-" })))
            if (report.details.isNotBlank()) {
                add(ExportSection(if (isBn) "বিস্তারিত তথ্য ও নোট" else "Detailed Values & Notes", report.details.lines().filter { it.isNotBlank() }))
            }
        }
        return ExportableHealthReport(
            suggestedFileName = "ShasthoSathi_Report_${report.serialNumber}_${safeTitleSlug}.pdf",
            title = "#${report.serialNumber} — ${report.title}",
            subtitle = if (isBn) "স্বাস্থ্যসাথী এআই • সংরক্ষিত মেডিকেল রিপোর্ট" else "ShasthoSathi AI • Saved Medical Report",
            generatedDate = nowFormatted(),
            disclaimer = if (isBn) {
                "সতর্কতা: শুধুমাত্র নির্দেশনা ও সংরক্ষণের জন্য — এটি কোনো রোগ নির্ণয় নয়। সর্বদা নিবন্ধিত ডাক্তারের পরামর্শ নিন।"
            } else {
                "Disclaimer: Guidance & personal record only — Not a medical diagnosis. Always consult a licensed doctor."
            },
            sections = sections
        )
    }

    fun buildAllMedicalReportsVault(
        reports: List<MedicalReport>,
        profile: UserProfile?,
        language: AppLanguage
    ): ExportableHealthReport {
        val isBn = language == AppLanguage.BN
        val sections = mutableListOf<ExportSection>()
        sections.add(
            ExportSection(
                heading = if (isBn) "প্রোফাইল ও ভল্ট সারসংক্ষেপ" else "Patient & Vault Summary",
                lines = listOf(
                    "${if (isBn) "নাম" else "Name"}: ${profile?.displayName?.ifBlank { "ShasthoSathi User" } ?: "ShasthoSathi User"}",
                    "${if (isBn) "বয়স" else "Age"}: ${profile?.age ?: 30} | ${if (isBn) "অবস্থান" else "Location"}: ${profile?.location?.ifBlank { "-" } ?: "-"}",
                    "${if (isBn) "মোট সংরক্ষিত রিপোর্ট" else "Total Serial Reports"}: ${reports.size}"
                )
            )
        )
        reports.sortedBy { it.serialNumber }.forEach { rep ->
            val lines = buildList {
                add("${if (isBn) "তারিখ" else "Date"}: ${rep.reportDate} | ${if (isBn) "ধরন" else "Type"}: ${rep.category}")
                if (rep.doctorOrLab.isNotBlank()) add("${if (isBn) "ডাক্তার/ল্যাব" else "Doctor/Lab"}: ${rep.doctorOrLab}")
                if (rep.attachmentName.isNotBlank()) add("${if (isBn) "সংযুক্তি" else "Attachment"}: ${rep.attachmentType} • ${rep.attachmentName}")
                add("${if (isBn) "সারসংক্ষেপ" else "Summary"}: ${rep.summary}")
                if (rep.details.isNotBlank()) {
                    add("${if (isBn) "বিস্তারিত" else "Details"}: ${rep.details.take(600)}")
                }
            }
            sections.add(ExportSection("#${rep.serialNumber} — ${rep.title}", lines))
        }
        return ExportableHealthReport(
            suggestedFileName = "ShasthoSathi_All_Reports_${fileStamp()}.pdf",
            title = if (isBn) "আমার সিরিয়াল মেডিকেল রিপোর্ট ভল্ট" else "My Serial Medical Reports Vault",
            subtitle = if (isBn) "স্বাস্থ্যসাথী এআই • সম্পূর্ণ রিপোর্ট সংকলন" else "ShasthoSathi AI • Complete Report Archive",
            generatedDate = nowFormatted(),
            disclaimer = if (isBn) {
                "সতর্কতা: শুধুমাত্র নির্দেশনা ও সংরক্ষণের জন্য — এটি কোনো রোগ নির্ণয় নয়। সর্বদা নিবন্ধিত ডাক্তারের পরামর্শ নিন।"
            } else {
                "Disclaimer: Guidance & personal record only — Not a medical diagnosis. Always consult a licensed doctor."
            },
            sections = sections
        )
    }

    fun buildFullHealthDashboardReport(
        profile: UserProfile?,
        reminders: List<MedicineReminder>,
        adherenceLogs: List<AdherenceLog>,
        healthMetrics: List<HealthMetricsRecord>,
        moodEntries: List<MoodEntry>,
        medicalReports: List<MedicalReport>,
        doctorBriefs: List<DoctorBrief>,
        appointments: List<DoctorAppointment>,
        language: AppLanguage
    ): ExportableHealthReport {
        val isBn = language == AppLanguage.BN
        val activeReminders = reminders.filter { it.active }
        val takenToday = activeReminders.count { it.todayStatus == "TAKEN" }
        val adherencePct = if (activeReminders.isNotEmpty()) (takenToday * 100) / activeReminders.size else 0
        val takenLogs = adherenceLogs.count { it.action == "TAKEN" }
        val snoozedLogs = adherenceLogs.count { it.action == "SNOOZED" }
        val skippedLogs = adherenceLogs.count { it.action == "SKIPPED" }

        val latestMetrics = healthMetrics.firstOrNull()
        val latestBmi = healthMetrics.firstOrNull { it.bmiValue > 0.0 }
        val avgWater = if (healthMetrics.isNotEmpty()) {
            healthMetrics.take(7).map { it.waterIntakeMl }.average().toLong()
        } else 0L
        val avgMood = if (moodEntries.isNotEmpty()) {
            moodEntries.take(7).map { it.moodScore }.average()
        } else 0.0

        val sections = mutableListOf<ExportSection>()

        // 1. Profile
        sections.add(
            ExportSection(
                heading = if (isBn) "১. স্বাস্থ্য প্রোফাইল" else "1. Patient Health Profile",
                lines = buildList {
                    add("${if (isBn) "নাম" else "Name"}: ${profile?.displayName?.ifBlank { "ShasthoSathi User" } ?: "ShasthoSathi User"}")
                    add("${if (isBn) "বয়স" else "Age"}: ${profile?.age ?: 30} | ${if (isBn) "অবস্থান" else "Location"}: ${profile?.location?.ifBlank { "-" } ?: "-"}")
                    if (!profile?.emergencyNumber.isNullOrBlank()) {
                        add("${if (isBn) "জরুরি যোগাযোগ" else "Emergency Number"}: ${profile.emergencyNumber}")
                    }
                    val conds = profile?.healthConditions?.ifEmpty { listOf("General Wellness") }?.joinToString(", ") ?: "General Wellness"
                    add("${if (isBn) "স্বাস্থ্য ফোকাস" else "Health Focus"}: $conds")
                }
            )
        )

        // 2. BMI, Hydration & Life Condition
        sections.add(
            ExportSection(
                heading = if (isBn) "২. বিএমআই, পানি পান ও জীবন অবস্থা" else "2. BMI, Hydration & Life Condition Summary",
                lines = buildList {
                    if (latestBmi != null) {
                        add("BMI: ${String.format(Locale.US, "%.1f", latestBmi.bmiValue)} (${latestBmi.bmiCategory}) | ${latestBmi.heightCm} cm, ${latestBmi.weightKg} kg")
                    } else {
                        add(if (isBn) "বিএমআই: এখনো রেকর্ড করা হয়নি" else "BMI: Not recorded yet")
                    }
                    add("${if (isBn) "আজকের পানি পান" else "Latest Daily Hydration"}: ${latestMetrics?.waterIntakeMl ?: 0} ml / ${latestMetrics?.waterGoalMl ?: 2500} ml (${if (isBn) "৭ দিনের গড়" else "7-day avg"}: $avgWater ml)")
                    if (latestMetrics != null && latestMetrics.lifeConditionSummary.isNotBlank()) {
                        add("${if (isBn) "ওয়েলনেস স্কোর" else "Wellness Score"}: ${latestMetrics.lifeConditionScore}/100 — ${latestMetrics.lifeConditionTitle}")
                        add(latestMetrics.lifeConditionSummary)
                    }
                }
            )
        )

        // 3. Medication Adherence & Active Schedule
        sections.add(
            ExportSection(
                heading = if (isBn) "৩. ওষুধ গ্রহণ ও রিমাইন্ডার বিশ্লেষণ" else "3. Medication Adherence & Active Schedule",
                lines = buildList {
                    add("${if (isBn) "আজকের অগ্রগতি" else "Today's Adherence"}: $takenToday / ${activeReminders.size} ($adherencePct%)")
                    add("${if (isBn) "মোট লগ" else "Logged History"}: Taken=$takenLogs, Snoozed=$snoozedLogs, Skipped=$skippedLogs")
                    activeReminders.take(12).forEach { rem ->
                        add("• ${rem.medicineName} (${rem.dosage}) — ${rem.scheduledTime} [${rem.mealTiming}] (${rem.todayStatus})")
                    }
                    if (activeReminders.isEmpty()) {
                        add(if (isBn) "কোনো সক্রিয় ওষুধের রিমাইন্ডার নেই।" else "No active medicine reminders scheduled.")
                    }
                }
            )
        )

        // 4. Mood & Mental Wellness
        sections.add(
            ExportSection(
                heading = if (isBn) "৪. মুড ও মানসিক সুস্থতার ধারা" else "4. Mood & Mental Wellness Trend",
                lines = buildList {
                    if (moodEntries.isNotEmpty()) {
                        add("${if (isBn) "সাম্প্রতিক গড় মুড স্কোর" else "Recent Average Mood Score"}: ${String.format(Locale.US, "%.1f", avgMood)} / 5.0 (${moodEntries.size} entries)")
                        moodEntries.take(5).forEach { m ->
                            add("• ${m.dateLabel}: ${m.detectedMoodTag} (${m.moodScore}/5) — ${m.aiReflection.take(160)}")
                        }
                    } else {
                        add(if (isBn) "এখনো কোনো মুড এন্ট্রি নেই।" else "No mood journal entries logged yet.")
                    }
                }
            )
        )

        // 5. Medical Reports & Doctor Consultations
        sections.add(
            ExportSection(
                heading = if (isBn) "৫. মেডিকেল রিপোর্ট ও ডাক্তার ভিজিট" else "5. Serial Medical Reports & Consultations",
                lines = buildList {
                    add("${if (isBn) "মোট রিপোর্ট" else "Saved Reports"}: ${medicalReports.size} | ${if (isBn) "ডাক্তার ব্রিফ" else "Doctor Briefs"}: ${doctorBriefs.size} | ${if (isBn) "অ্যাপয়েন্টমেন্ট" else "Appointments"}: ${appointments.size}")
                    medicalReports.takeLast(8).forEach { rep ->
                        add("• #${rep.serialNumber} (${rep.reportDate}) ${rep.title} [${rep.category}]: ${rep.summary.take(180)}")
                    }
                }
            )
        )

        return ExportableHealthReport(
            suggestedFileName = "ShasthoSathi_Health_Dashboard_${fileStamp()}.pdf",
            title = if (isBn) "স্বাস্থ্যসাথী এআই — সম্পূর্ণ স্বাস্থ্য ডেটা রিপোর্ট" else "ShasthoSathi AI — Comprehensive Health Data Report",
            subtitle = if (isBn) "ব্যক্তিগত স্বাস্থ্য বিশ্লেষণ ও ডক্টর ভিজিট সারাংশ" else "Personal Health Analytics & Pre-Consultation Summary",
            generatedDate = nowFormatted(),
            disclaimer = if (isBn) {
                "সতর্কতা: এটি শুধুমাত্র ব্যক্তিগত তথ্য ও নির্দেশনার জন্য — কোনো রোগ নির্ণয় নয়। সর্বদা নিবন্ধিত ডাক্তারের পরামর্শ নিন।"
            } else {
                "Disclaimer: Personal tracking & guidance only — Not a medical diagnosis. Always consult a licensed physician."
            },
            sections = sections
        )
    }

    fun formatAsPlainText(report: ExportableHealthReport): String = buildString {
        appendLine("========================================")
        appendLine(report.title)
        appendLine(report.subtitle)
        appendLine("Generated: ${report.generatedDate}")
        appendLine("========================================")
        appendLine()
        report.sections.forEach { section ->
            appendLine(section.heading.uppercase(Locale.US))
            section.lines.forEach { line ->
                appendLine(line)
            }
            appendLine()
        }
        appendLine("----------------------------------------")
        appendLine(report.disclaimer)
    }

    suspend fun writePdfToUri(
        context: Context,
        uri: Uri,
        report: ExportableHealthReport
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val stream = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Could not open destination file for writing.")
            stream.use { out ->
                renderPdfDocument(report, out)
            }
        }
    }

    suspend fun generateShareablePdfFile(
        context: Context,
        report: ExportableHealthReport
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "reports").apply { mkdirs() }
            val outFile = File(dir, report.suggestedFileName)
            FileOutputStream(outFile).use { out ->
                renderPdfDocument(report, out)
            }
            outFile
        }
    }

    fun launchSharePdfIntent(context: Context, pdfFile: File, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF Health Report"))
    }

    fun launchShareTextIntent(context: Context, report: ExportableHealthReport) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, report.title)
            putExtra(Intent.EXTRA_TEXT, formatAsPlainText(report))
        }
        context.startActivity(Intent.createChooser(intent, "Share Health Report Summary"))
    }

    private fun renderPdfDocument(report: ExportableHealthReport, outputStream: OutputStream) {
        val pageWidth = 595 // A4 points (72 DPI)
        val pageHeight = 842
        val marginX = 40f
        val contentWidth = (pageWidth - (marginX * 2)).toInt()
        val footerTop = pageHeight - 54f

        val pdf = PdfDocument()
        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas

        val tealColor = Color.rgb(15, 118, 110)
        val lightTealBg = Color.rgb(240, 253, 250)
        val darkText = Color.rgb(15, 23, 42)
        val mutedText = Color.rgb(71, 85, 105)
        val borderColor = Color.rgb(226, 232, 240)

        val headerBgPaint = Paint().apply { color = tealColor }
        val sectionBgPaint = Paint().apply { color = lightTealBg }
        val dividerPaint = Paint().apply {
            color = borderColor
            strokeWidth = 1f
        }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 17f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(204, 251, 241)
            textSize = 10.5f
        }
        val sectionHeadingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tealColor
            textSize = 12.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkText
            textSize = 10.5f
        }
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedText
            textSize = 8.5f
        }

        fun drawFooter(c: android.graphics.Canvas, pNum: Int) {
            c.drawLine(marginX, footerTop - 8f, pageWidth - marginX, footerTop - 8f, dividerPaint)
            val footerLayout = StaticLayout.Builder
                .obtain(report.disclaimer, 0, report.disclaimer.length, footerPaint, contentWidth - 50)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            c.save()
            c.translate(marginX, footerTop)
            footerLayout.draw(c)
            c.restore()
            c.drawText("Page $pNum", pageWidth - marginX - 38f, footerTop + 12f, footerPaint)
        }

        fun startNextPage(): Float {
            drawFooter(canvas, pageNum)
            pdf.finishPage(page)
            pageNum += 1
            page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
            canvas = page.canvas
            return 44f
        }

        // Draw Teal Branded Header Banner on Page 1
        canvas.drawRoundRect(RectF(marginX, 32f, pageWidth - marginX, 114f), 12f, 12f, headerBgPaint)
        val titleLayout = StaticLayout.Builder
            .obtain(report.title, 0, report.title.length, titlePaint, contentWidth - 28)
            .build()
        canvas.save()
        canvas.translate(marginX + 14f, 44f)
        titleLayout.draw(canvas)
        canvas.restore()
        canvas.drawText(report.subtitle, marginX + 14f, 92f, subPaint)
        canvas.drawText("Generated: ${report.generatedDate}", marginX + 14f, 106f, subPaint)

        var cursorY = 132f

        report.sections.forEach { section ->
            if (cursorY > footerTop - 70f) {
                cursorY = startNextPage()
            }
            // Section Heading Bar
            canvas.drawRoundRect(
                RectF(marginX, cursorY, pageWidth - marginX, cursorY + 24f),
                6f,
                6f,
                sectionBgPaint
            )
            canvas.drawText(section.heading, marginX + 10f, cursorY + 16f, sectionHeadingPaint)
            cursorY += 32f

            section.lines.forEach { line ->
                val safeLine = line.trim()
                if (safeLine.isNotEmpty()) {
                    val textLayout = StaticLayout.Builder
                        .obtain(safeLine, 0, safeLine.length, bodyPaint, contentWidth - 12)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(2.5f, 1.0f)
                        .build()
                    if (cursorY + textLayout.height > footerTop - 16f) {
                        cursorY = startNextPage()
                    }
                    canvas.save()
                    canvas.translate(marginX + 6f, cursorY)
                    textLayout.draw(canvas)
                    canvas.restore()
                    cursorY += textLayout.height + 6f
                }
            }
            cursorY += 10f
        }

        drawFooter(canvas, pageNum)
        pdf.finishPage(page)
        try {
            pdf.writeTo(outputStream)
        } finally {
            pdf.close()
        }
    }
}
