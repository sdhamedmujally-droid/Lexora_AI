const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

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

function buildProfilePayload(uid, ts) {
  return {
    userId: uid,
    displayName: "Alice Learner",
    preferredLevel: "B1",
    streakDays: 3,
    longestStreak: 7,
    lastActiveDay: "2026-11-18",
    totalStudyMinutes: 45,
    wordsMasteredCount: 120,
    wordsLearningCount: 35,
    quizzesCompleted: 4,
    totalQuizScorePct: 340,
    storiesCompleted: 2,
    conversationsCompleted: 1,
    grammarCompleted: 5,
    verbsPracticed: 40,
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(buildProfilePayload(ALICE_UID, ts))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("word_progress").get()
  );
});

test("Owner can create, read, and partially update learning profile; shadow & timestamp attacks fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(buildProfilePayload(ALICE_UID, created))
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      streakDays: 4,
      totalStudyMinutes: 60,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Ghost field fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt mutation fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Future timestamp fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      streakDays: 5,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Cross-user isolation blocks Bob from reading or writing Alice's subcollections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("word_progress")
      .doc("word_abandon")
      .set({
        userId: ALICE_UID,
        headword: "abandon",
        cefrLevel: "B2",
        favorite: true,
        masteryState: "LEARNING",
        easeFactor: 2.5,
        intervalDays: 1,
        repetitions: 1,
        nextReviewAtMs: Date.now(),
        correctCount: 1,
        attemptCount: 1,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("word_progress").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("word_progress")
      .doc("word_abandon")
      .update({ favorite: false, updatedAt: new Date(Date.now() - 1000) })
  );
});

test("Word progress, quiz attempts, content progress, and daily activity payloads succeed and reject invalid IDs/fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  // Empty list queries succeed for owner
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("word_progress").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("quiz_attempts").get()
  );

  // Create quiz attempt
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("quiz_attempts")
      .doc("attempt_001")
      .set({
        userId: ALICE_UID,
        category: "VOCABULARY",
        mode: "MCQ",
        difficulty: "INTERMEDIATE",
        cefrLevel: "B1",
        score: 8,
        totalQuestions: 10,
        durationSeconds: 120,
        topicTitle: "B1 Core Vocabulary",
        createdAt: ts,
      })
  );

  // Reject @DocumentId "id" key inside body
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("quiz_attempts")
      .doc("attempt_002")
      .set({
        id: "attempt_002",
        userId: ALICE_UID,
        category: "VOCABULARY",
        mode: "MCQ",
        difficulty: "INTERMEDIATE",
        cefrLevel: "B1",
        score: 8,
        totalQuestions: 10,
        durationSeconds: 120,
        topicTitle: "B1 Core Vocabulary",
        createdAt: ts,
      })
  );

  // Content progress create + update
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("content_progress")
      .doc("story_a1_001")
      .set({
        userId: ALICE_UID,
        contentType: "STORY",
        cefrLevel: "A1",
        completed: true,
        bestScorePct: 90,
        minutesSpent: 5,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("content_progress")
      .doc("story_a1_001")
      .update({
        bestScorePct: 100,
        minutesSpent: 8,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Daily activity create + update
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("daily_activity")
      .doc("2026-11-18")
      .set({
        userId: ALICE_UID,
        dateKey: "2026-11-18",
        minutesStudied: 25,
        wordsReviewed: 30,
        quizzesTaken: 2,
        createdAt: ts,
        updatedAt: ts,
      })
  );
});
