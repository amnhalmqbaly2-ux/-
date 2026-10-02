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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user: cannot read profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("profiles").get());
});

test("Authenticated user: can create and read own child profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("profiles").doc("prof_1").set({
      id: "prof_1",
      userId: ALICE_UID,
      name: "سالم",
      age: 8,
      avatarId: 1,
      stars: 15,
      badges: "falaj_guardian",
      completedStoryIds: "story_1",
      preTestScore: 3,
      preTestTotal: 5,
      postTestScore: -1,
      postTestTotal: 5,
      postTestUnlockedByTeacher: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("profiles").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: cannot access another user's profiles or assessments", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("profiles").doc("prof_bob").set({
      id: "prof_bob",
      userId: BOB_UID,
      name: "مريم",
      age: 7,
      avatarId: 2,
      stars: 10,
      badges: "",
      completedStoryIds: "",
      preTestScore: 2,
      preTestTotal: 5,
      postTestScore: -1,
      postTestTotal: 5,
      postTestUnlockedByTeacher: false,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("profiles").doc("prof_bob").get()
  );
});

test("Shadow update test: rejects ghost field on profile update", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("profiles").doc("prof_1");
  await assertSucceeds(
    docRef.set({
      id: "prof_1",
      userId: ALICE_UID,
      name: "سالم",
      age: 8,
      avatarId: 1,
      stars: 15,
      badges: "",
      completedStoryIds: "",
      preTestScore: 3,
      preTestTotal: 5,
      postTestScore: -1,
      postTestTotal: 5,
      postTestUnlockedByTeacher: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    docRef.update({
      stars: 20,
      isAdminGhostField: true,
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can update child profile stars and badges", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("profiles").doc("prof_stars_test");
  await assertSucceeds(
    docRef.set({
      id: "prof_stars_test",
      userId: ALICE_UID,
      name: "سالم",
      age: 8,
      avatarId: 1,
      stars: 10,
      badges: "badge_first_story_read",
      completedStoryIds: "story_1",
      preTestScore: 4,
      preTestTotal: 5,
      postTestScore: -1,
      postTestTotal: 5,
      postTestUnlockedByTeacher: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    docRef.update({
      stars: 25,
      badges: "badge_first_story_read,badge_points_5",
      completedStoryIds: "story_1,story_2",
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can create assessment record and track test progress", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("assessments").doc("assess_1").set({
      id: "assess_1",
      userId: ALICE_UID,
      profileId: "prof_stars_test",
      storyId: "story_1",
      storyTitle: "سِرُّ الفَلَجِ العَجِيبِ",
      assessmentType: "STORY_PRACTICE",
      score: 5,
      totalQuestions: 5,
      literalScore: 1,
      vocabularyScore: 1,
      sequencingScore: 1,
      inferenceScore: 1,
      mainIdeaScore: 1,
      createdAt: now,
    })
  );
});

test("Authenticated user: can create and save custom stories", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("custom_stories").doc("custom_story_1").set({
      id: "custom_story_1",
      userId: ALICE_UID,
      title: "مُغَامَرَةُ الجَبَلِ الأَخْضَرِ",
      villageName: "نِزْوَى",
      moral: "التَّعَاوُنُ وَالصَّبْرُ",
      paragraphsText: "فِي قَرْيَةٍ جَمِيلَةٍ...",
      vocabularyText: "المِدْرَاجُ: سُلَّمٌ صَخْرِيٌّ",
      questionsText: "مَا هُوَ اسْمُ القَرْيَةِ؟",
      createdAt: now,
    })
  );
});

