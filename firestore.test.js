const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("reminders").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Unauthorized",
      age: 30,
      language: "EN",
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("UserProfile: owner create/update succeeds; cross-user and shadow fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Rahim Uddin",
      age: 42,
      language: "EN",
      healthConditions: ["Diabetes", "High BP"],
      dietaryPreference: "Regular",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Partial update allowed
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      language: "BN",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user read/write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Shadow field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt mutation rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("MedicineReminder: create, toggle, mark status, list, and delete succeed for owner; fail for others", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  const remRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("reminders")
    .doc("rem_1");

  await assertSucceeds(
    remRef.set({
      userId: ALICE_UID,
      medicineName: "Napa 500mg",
      dosage: "1 Tablet",
      timeSlot: "MORNING",
      scheduledTime: "08:30 AM",
      mealTiming: "After Meal",
      durationDays: 5,
      sourceType: "AUTO_RX",
      active: true,
      todayStatus: "PENDING",
      lastActionDate: "2026-11-19",
      notes: "For fever",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Toggle active
  await assertSucceeds(
    remRef.update({
      active: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Mark TAKEN
  await assertSucceeds(
    remRef.update({
      todayStatus: "TAKEN",
      lastActionDate: "2026-11-19",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // List query matching Kotlin repository
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reminders").get()
  );

  // Cross-user list/get denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("reminders").get()
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reminders")
      .doc("rem_future")
      .set({
        userId: ALICE_UID,
        medicineName: "Seclo 20mg",
        dosage: "1 Capsule",
        timeSlot: "MORNING",
        scheduledTime: "08:00 AM",
        mealTiming: "Before Meal",
        durationDays: 14,
        sourceType: "MANUAL",
        active: true,
        todayStatus: "PENDING",
        lastActionDate: "2026-11-19",
        notes: "",
        createdAt: new Date(Date.now() + 60000),
        updatedAt: new Date(Date.now() + 60000),
      })
  );

  // Extra @DocumentId `id` field rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reminders")
      .doc("rem_bad_id")
      .set({
        id: "rem_bad_id",
        userId: ALICE_UID,
        medicineName: "Fexo 120mg",
        dosage: "1 Tab",
        timeSlot: "NIGHT",
        scheduledTime: "09:30 PM",
        mealTiming: "After Meal",
        durationDays: 7,
        sourceType: "MANUAL",
        active: true,
        todayStatus: "PENDING",
        lastActionDate: "2026-11-19",
        notes: "",
        createdAt: past,
        updatedAt: past,
      })
  );
});

test("AdherenceLog, DoctorBrief, DietPlan, and MoodEntry owner create/list succeed and cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 4000);

  // AdherenceLog
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("adherence_logs")
      .doc("log_1")
      .set({
        userId: ALICE_UID,
        reminderId: "rem_1",
        medicineName: "Napa 500mg",
        action: "TAKEN",
        dateKey: "2026-11-19",
        createdAt: past,
      })
  );

  // DoctorBrief
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("doctor_briefs")
      .doc("brief_1")
      .set({
        userId: ALICE_UID,
        language: "EN",
        chiefSymptoms: "Fever and mild cough for 2 days",
        severityAndContext: "Severity 6/10, no allergies, took Napa 500mg",
        questionsForDoctor: ["Should I get a CBC test?"],
        redFlagWarning: "Seek immediate care if breathing becomes difficult.",
        urgentCareAdvised: false,
        createdAt: past,
      })
  );

  // DietPlan
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("diet_plans")
      .doc("plan_1")
      .set({
        userId: ALICE_UID,
        language: "BN",
        age: 42,
        conditions: ["Diabetes"],
        preference: "Regular",
        breakfastTitle: "লাল আটার রুটি ও ডাল",
        breakfastPortion: "২টি রুটি, ১ বাটি পাতলা ডাল ও ১টি সিদ্ধ ডিম",
        breakfastRationale: "রক্তের শর্করা স্থিতিশীল রাখে",
        lunchTitle: "পরিমিত ভাত ও মাছের ঝোল",
        lunchPortion: "১ কাপ ভাত, ১ পিস রুই মাছ ও শাকসবজি",
        lunchRationale: "সুষম আমিষ ও আঁশ সরবরাহ করে",
        snackTitle: "পেয়ারা ও ভাজা ছোলা",
        snackPortion: "১টি মাঝারি পেয়ারা ও মুঠো ছোলা",
        snackRationale: "স্বাস্থ্যকর বিকেলের নাস্তা",
        dinnerTitle: "রুটি ও সবজি ভাজি",
        dinnerPortion: "২টি পাতলা রুটি ও কম তেলে সবজি",
        dinnerRationale: "রাতে সহজপাচ্য খাবার",
        foodsToPrioritize: ["শাকসবজি", "ডাল", "মাছ"],
        foodsToLimit: ["চিনিযুক্ত পানীয়", "ভাজাপোড়া"],
        hydrationTip: "দিনে ৮-১০ গ্লাস নিরাপদ পানি পান করুন।",
        createdAt: past,
      })
  );

  // MoodEntry
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("mood_entries")
      .doc("mood_1")
      .set({
        userId: ALICE_UID,
        language: "EN",
        selectedMood: "CALM",
        journalText: "Feeling much better after taking my evening walk.",
        detectedMoodTag: "Peaceful & Hopeful",
        moodScore: 4,
        aiReflection: "It sounds wonderful that your walk helped bring calm to your evening.",
        microAction: "Try 2 minutes of 4-7-8 breathing before sleep.",
        supportNote: "",
        dateLabel: "Nov 19",
        createdAt: past,
      })
  );

  // Owner lists succeed
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("doctor_briefs").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("diet_plans").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("mood_entries").get()
  );

  // Cross-user lists fail
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("doctor_briefs").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("mood_entries").get()
  );
});

test("UserProfile location/emergencyNumber, Reminder ringtone, MedicalReport, and DoctorAppointment rules enforce ownership & schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 4000);

  // UserProfile with location & emergencyNumber
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Asaduzzaman",
      age: 34,
      location: "Dhanmondi, Dhaka",
      emergencyNumber: "+8801711000111",
      language: "EN",
      healthConditions: ["General Wellness"],
      dietaryPreference: "Regular",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Reminder with custom ringtone URI & name
  const remRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("reminders")
    .doc("rem_ringtone_1");
  await assertSucceeds(
    remRef.set({
      userId: ALICE_UID,
      medicineName: "Napa Extra",
      dosage: "1 Tablet",
      timeSlot: "NIGHT",
      scheduledTime: "10:15 PM",
      mealTiming: "After Meal",
      durationDays: 5,
      sourceType: "MANUAL",
      active: true,
      todayStatus: "PENDING",
      lastActionDate: "2026-11-19",
      notes: "With warm water",
      ringtoneUri: "content://settings/system/alarm_alert",
      ringtoneName: "Default Alarm",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    remRef.update({
      medicineName: "Napa Extra 500mg",
      dosage: "1 Tablet",
      timeSlot: "NIGHT",
      scheduledTime: "10:30 PM",
      mealTiming: "After Meal",
      durationDays: 7,
      notes: "After dinner",
      ringtoneUri: "content://settings/system/ringtone",
      ringtoneName: "Gentle Bell",
      active: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // MedicalReport create & list
  const reportRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("medical_reports")
    .doc("rep_1");
  await assertSucceeds(
    reportRef.set({
      userId: ALICE_UID,
      serialNumber: 1,
      title: "Complete Blood Count (CBC)",
      category: "LAB_TEST",
      doctorOrLab: "Popular Diagnostic Centre, Dhaka",
      reportDate: "2026-11-19",
      summary: "Hemoglobin 13.8 g/dL, WBC normal, Platelets 240,000",
      details: "Routine blood panel within normal ranges.",
      createdAt: past,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("medical_reports").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("medical_reports").get()
  );

  // DoctorAppointment paid booking create, cancel update, cross-user deny
  const apptRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("doctor_appointments")
    .doc("appt_1");
  await assertSucceeds(
    apptRef.set({
      userId: ALICE_UID,
      doctorId: "doc_medicine_01",
      doctorName: "Dr. Farhana Rahman, FCPS",
      specialty: "Internal Medicine",
      hospital: "Square Hospital, Dhaka",
      appointmentDate: "Nov 20, 2026",
      timeSlot: "07:00 PM",
      consultationMode: "VIDEO",
      feeBdt: 800,
      paymentMethod: "BKASH",
      paymentStatus: "PAID",
      bookingStatus: "CONFIRMED",
      bookingRef: "SS-2026-8821",
      reasonForVisit: "Persistent mild fever and fatigue",
      attachedReportTitle: "Complete Blood Count (CBC)",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    apptRef.update({
      bookingStatus: "CANCELLED",
      paymentStatus: "REFUNDED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("doctor_appointments").get()
  );
});

test("HealthMetricsRecord and MedicalReport attachments enforce ownership & schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 4000);

  const metricRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("health_metrics")
    .doc("2026-11-20");

  await assertSucceeds(
    metricRef.set({
      userId: ALICE_UID,
      dateKey: "2026-11-20",
      heightCm: 170.0,
      weightKg: 68.5,
      bmiValue: 23.7,
      bmiCategory: "Normal",
      waterIntakeMl: 1750,
      waterGoalMl: 2500,
      lifeConditionTitle: "Balanced & Improving",
      lifeConditionScore: 82,
      lifeConditionSummary: "Your BMI is in the healthy range and hydration is progressing well.",
      lifeConditionTips: ["Drink 3 more glasses of water before evening", "Take a 20-minute walk"],
      language: "EN",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    metricRef.update({
      waterIntakeMl: 2250,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("health_metrics").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("health_metrics").get()
  );

  // MedicalReport with attachmentType AUDIO / FILE / IMAGE
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("medical_reports")
      .doc("rep_audio_1")
      .set({
        userId: ALICE_UID,
        serialNumber: 2,
        title: "Voice Symptom Note",
        category: "DOCTOR_NOTE",
        doctorOrLab: "Google Drive Upload",
        reportDate: "2026-11-20",
        summary: "Mild throat discomfort and cough recorded for doctor review",
        details: "Transcribed audio note attached via Symptom Organizer.",
        attachmentType: "AUDIO",
        attachmentName: "symptom_voice.m4a",
        createdAt: past,
      })
  );
});


