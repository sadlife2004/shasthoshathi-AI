package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DoctorAppointment
import com.example.data.model.MedicalReport
import com.example.data.model.ProfessionalDoctor
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.ui.components.SafetyDisclaimerBanner
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralHeart
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDeep
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val VerifiedSpecialistDirectory = listOf(
    ProfessionalDoctor(
        id = "doc_medicine_01",
        nameEn = "Dr. Mahmudul Hasan, FCPS",
        nameBn = "ডা. মাহমুদুল হাসান, এফসিপিএস",
        qualification = "MBBS, FCPS (Medicine)",
        bmdcRegNo = "BMDC A-54821",
        specialtyEn = "Internal Medicine",
        specialtyBn = "মেডিসিন বিশেষজ্ঞ",
        hospitalEn = "Square Hospitals, Dhaka",
        hospitalBn = "স্কয়ার হাসপাতাল, ঢাকা",
        experienceYears = 14,
        rating = 4.9,
        feeBdt = 900,
        availableSlots = listOf("05:30 PM", "07:00 PM", "08:30 PM", "09:30 PM"),
        languages = "Bangla, English"
    ),
    ProfessionalDoctor(
        id = "doc_cardio_02",
        nameEn = "Dr. Farzana Akter, MD",
        nameBn = "ডা. ফারজানা আক্তার, এমডি",
        qualification = "MBBS, MD (Cardiology)",
        bmdcRegNo = "BMDC A-49210",
        specialtyEn = "Cardiology",
        specialtyBn = "হৃদরোগ বিশেষজ্ঞ",
        hospitalEn = "Evercare Hospital, Dhaka",
        hospitalBn = "এভারকেয়ার হাসপাতাল, ঢাকা",
        experienceYears = 12,
        rating = 4.9,
        feeBdt = 1200,
        availableSlots = listOf("04:00 PM", "06:00 PM", "08:00 PM"),
        languages = "Bangla, English"
    ),
    ProfessionalDoctor(
        id = "doc_diabetes_03",
        nameEn = "Dr. Tanvir Ahmed, DEM",
        nameBn = "ডা. তানভীর আহমেদ, ডিইএম",
        qualification = "MBBS, DEM (BIRDEM)",
        bmdcRegNo = "BMDC A-61345",
        specialtyEn = "Diabetes & Endocrine",
        specialtyBn = "ডায়াবেটিস ও হরমোন",
        hospitalEn = "BIRDEM General Hospital",
        hospitalBn = "বারডেম জেনারেল হাসপাতাল",
        experienceYears = 10,
        rating = 4.8,
        feeBdt = 800,
        availableSlots = listOf("10:30 AM", "04:30 PM", "07:30 PM", "09:00 PM"),
        languages = "Bangla, English"
    ),
    ProfessionalDoctor(
        id = "doc_gyne_04",
        nameEn = "Dr. Nusrat Jahan, FCPS",
        nameBn = "ডা. নুসরাত জাহান, এফসিপিএস",
        qualification = "MBBS, FCPS (Obs & Gynae)",
        bmdcRegNo = "BMDC A-58902",
        specialtyEn = "Gynecology",
        specialtyBn = "গাইনী ও প্রসূতি",
        hospitalEn = "United Hospital, Dhaka",
        hospitalBn = "ইউনাইটেড হাসপাতাল, ঢাকা",
        experienceYears = 11,
        rating = 4.9,
        feeBdt = 1000,
        availableSlots = listOf("11:00 AM", "05:00 PM", "08:00 PM"),
        languages = "Bangla, English"
    ),
    ProfessionalDoctor(
        id = "doc_peds_05",
        nameEn = "Dr. Kamrul Islam, MD",
        nameBn = "ডা. কামরুল ইসলাম, এমডি",
        qualification = "MBBS, MD (Pediatrics)",
        bmdcRegNo = "BMDC A-63119",
        specialtyEn = "Pediatrics",
        specialtyBn = "শিশু বিশেষজ্ঞ",
        hospitalEn = "Dhaka Shishu Hospital",
        hospitalBn = "ঢাকা শিশু হাসপাতাল",
        experienceYears = 9,
        rating = 4.8,
        feeBdt = 750,
        availableSlots = listOf("03:30 PM", "06:30 PM", "08:30 PM"),
        languages = "Bangla, English"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorsScreen(
    language: AppLanguage,
    appointments: List<DoctorAppointment>,
    reports: List<MedicalReport>,
    onBack: () -> Unit,
    onBookPaidAppointment: (
        doctor: ProfessionalDoctor,
        appointmentDate: String,
        timeSlot: String,
        consultationMode: String,
        paymentMethod: String,
        reasonForVisit: String,
        attachedReportTitle: String,
        onBooked: () -> Unit
    ) -> Unit,
    onCancelAppointment: (DoctorAppointment) -> Unit
) {
    BackHandler(onBack = onBack)

    val strings = stringsFor(language)
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedSpecialty by remember { mutableStateOf("ALL") }
    var bookingDoctor by remember { mutableStateOf<ProfessionalDoctor?>(null) }

    val specialties = remember(language) {
        listOf("ALL" to strings.filterAll) + VerifiedSpecialistDirectory.map {
            it.specialtyEn to (if (language == AppLanguage.BN) it.specialtyBn else it.specialtyEn)
        }.distinctBy { it.first }
    }

    val filteredDoctors = remember(selectedSpecialty) {
        if (selectedSpecialty == "ALL") VerifiedSpecialistDirectory
        else VerifiedSpecialistDirectory.filter { it.specialtyEn == selectedSpecialty }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("professional_doctors_screen")
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("doctors_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Home",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strings.proDoctorsTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = AmberContainer
                    ) {
                        Text(
                            text = strings.paidBadgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AmberOnContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = strings.proDoctorsSub,
                    style = MaterialTheme.typography.bodySmall,
                    color = TealDeep
                )
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(strings.findDoctorsTab) },
                modifier = Modifier.testTag("doctors_find_tab")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("${strings.myAppointmentsTab} (${appointments.size})") },
                modifier = Modifier.testTag("doctors_bookings_tab")
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { SafetyDisclaimerBanner(language = language) }

            if (selectedTab == 0) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(specialties) { (key, label) ->
                            FilterChip(
                                selected = selectedSpecialty == key,
                                onClick = { selectedSpecialty = key },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealDeep,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                items(filteredDoctors, key = { it.id }) { doc ->
                    SpecialistDoctorCard(
                        doctor = doc,
                        language = language,
                        onBookClick = { bookingDoctor = doc }
                    )
                }
            } else {
                if (appointments.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalHospital,
                                    contentDescription = null,
                                    tint = TealDeep,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = strings.noAppointmentsYet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(appointments, key = { it.id }) { appt ->
                        AppointmentBookingCard(
                            appointment = appt,
                            language = language,
                            onCancel = { onCancelAppointment(appt) }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    bookingDoctor?.let { doc ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { bookingDoctor = null },
            sheetState = sheetState
        ) {
            PaidBookingCheckoutSheet(
                doctor = doc,
                reports = reports,
                language = language,
                onConfirmPay = { date, slot, mode, payment, reason, attachedTitle ->
                    onBookPaidAppointment(doc, date, slot, mode, payment, reason, attachedTitle) {
                        bookingDoctor = null
                        selectedTab = 1
                    }
                }
            )
        }
    }
}

@Composable
private fun SpecialistDoctorCard(
    doctor: ProfessionalDoctor,
    language: AppLanguage,
    onBookClick: () -> Unit
) {
    val strings = stringsFor(language)
    val isBn = language == AppLanguage.BN
    val name = if (isBn) doctor.nameBn else doctor.nameEn
    val specialty = if (isBn) doctor.specialtyBn else doctor.specialtyEn
    val hospital = if (isBn) doctor.hospitalBn else doctor.hospitalEn

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("doctor_card_${doctor.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(TealContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalHospital,
                        contentDescription = null,
                        tint = TealDeep,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "BMDC Verified",
                            tint = TealDeep,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "$specialty • ${doctor.qualification}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TealDeep
                    )
                    Text(
                        text = "$hospital • ${doctor.bmdcRegNo}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(shape = RoundedCornerShape(8.dp), color = AmberContainer) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(14.dp))
                            Text(
                                text = "${doctor.rating}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AmberOnContainer
                            )
                        }
                    }
                    Text(
                        text = if (isBn) "${doctor.experienceYears} বছর অভিজ্ঞতা" else "${doctor.experienceYears} yrs exp",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(shape = RoundedCornerShape(10.dp), color = EmeraldContainer) {
                    Text(
                        text = "৳${doctor.feeBdt}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldSuccess,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onBookClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("book_doctor_${doctor.id}"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
            ) {
                Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("${strings.bookAppointmentBtn} (৳${doctor.feeBdt})", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun PaidBookingCheckoutSheet(
    doctor: ProfessionalDoctor,
    reports: List<MedicalReport>,
    language: AppLanguage,
    onConfirmPay: (
        date: String,
        slot: String,
        mode: String,
        paymentMethod: String,
        reason: String,
        attachedReportTitle: String
    ) -> Unit
) {
    val strings = stringsFor(language)
    val isBn = language == AppLanguage.BN
    val dateOptions = remember {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        (0..4).map { offset ->
            val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            fmt.format(c.time)
        }
    }

    var selectedDate by remember { mutableStateOf(dateOptions.first()) }
    var selectedSlot by remember { mutableStateOf(doctor.availableSlots.first()) }
    var selectedMode by remember { mutableStateOf("VIDEO") }
    var selectedPayment by remember { mutableStateOf("BKASH") }
    var selectedReportTitle by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = strings.checkoutTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = TealContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, TealDeep.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBn) doctor.nameBn else doctor.nameEn,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TealDeep
                    )
                    Text(
                        text = if (isBn) doctor.specialtyBn else doctor.specialtyEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "৳${doctor.feeBdt}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TealDeep
                )
            }
        }

        // Date
        Text(strings.selectDateLabel, style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dateOptions) { date ->
                FilterChip(
                    selected = selectedDate == date,
                    onClick = { selectedDate = date },
                    label = { Text(date, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TealDeep,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Time Slot
        Text(strings.selectTimeSlotLabel, style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(doctor.availableSlots) { slot ->
                FilterChip(
                    selected = selectedSlot == slot,
                    onClick = { selectedSlot = slot },
                    label = { Text(slot, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TealDeep,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Consultation Mode
        Text(strings.consultationModeLabel, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Triple("VIDEO", if (isBn) "ভিডিও কল" else "Video", Icons.Filled.Videocam),
                Triple("AUDIO", if (isBn) "অডিও কল" else "Audio", Icons.Filled.Call),
                Triple("CHAT", if (isBn) "চ্যাট" else "Chat", Icons.Filled.Chat)
            ).forEach { (modeKey, label, icon) ->
                FilterChip(
                    selected = selectedMode == modeKey,
                    onClick = { selectedMode = modeKey },
                    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TealDeep,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )
            }
        }

        // Optional Medical Report Attachment from Serial Vault
        if (reports.isNotEmpty()) {
            Text(strings.attachReportLabel, style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedReportTitle.isBlank(),
                        onClick = { selectedReportTitle = "" },
                        label = { Text(strings.noReportAttached, style = MaterialTheme.typography.labelSmall) }
                    )
                }
                items(reports, key = { it.id }) { rep ->
                    val label = "#${rep.serialNumber} ${rep.title}"
                    FilterChip(
                        selected = selectedReportTitle == label,
                        onClick = { selectedReportTitle = label },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text(strings.reasonForVisitLabel) },
            minLines = 2,
            maxLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("appointment_reason_input")
        )

        // Payment Method
        Text(strings.paymentMethodLabel, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("BKASH" to "bKash", "NAGAD" to "Nagad", "CARD" to "Visa / Card").forEach { (methodKey, label) ->
                FilterChip(
                    selected = selectedPayment == methodKey,
                    onClick = { selectedPayment = methodKey },
                    leadingIcon = { Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberAccent,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )
            }
        }

        Button(
            onClick = {
                onConfirmPay(
                    selectedDate,
                    selectedSlot,
                    selectedMode,
                    selectedPayment,
                    reason,
                    selectedReportTitle
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("confirm_paid_appointment_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("${strings.confirmAndPayBtn} • ৳${doctor.feeBdt}", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AppointmentBookingCard(
    appointment: DoctorAppointment,
    language: AppLanguage,
    onCancel: () -> Unit
) {
    val strings = stringsFor(language)
    val isCancelled = appointment.bookingStatus == "CANCELLED"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("appointment_card_${appointment.bookingRef}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCancelled) CoralContainer else EmeraldContainer
                ) {
                    Text(
                        text = "${appointment.bookingStatus} • ${appointment.paymentStatus}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isCancelled) CoralHeart else EmeraldSuccess,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = appointment.bookingRef,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TealDeep
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = appointment.doctorName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${appointment.specialty} • ${appointment.hospital}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = "${appointment.appointmentDate} • ${appointment.timeSlot} • ${appointment.consultationMode} • ৳${appointment.feeBdt} (${appointment.paymentMethod})",
                style = MaterialTheme.typography.labelMedium,
                color = TealDeep
            )

            if (appointment.attachedReportTitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Report: ${appointment.attachedReportTitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (appointment.reasonForVisit.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = appointment.reasonForVisit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isCancelled) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CoralHeart.copy(alpha = 0.5f)),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(strings.cancelRefundBtn, color = CoralHeart, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
