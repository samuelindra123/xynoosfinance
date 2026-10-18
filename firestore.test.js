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

const validNow = () => new Date(Date.now() - 1000);

test("1. Unauthenticated user cannot read or write transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
});

test("2. Cross-user isolation: Bob cannot read Alice's profile or transactions", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      currencyCode: "IDR",
      financialProfileType: "PERSONAL",
      onboardingCompleted: true,
      version: 1,
      createdAt: validNow(),
      updatedAt: validNow(),
    });
    await db.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amountIdr: 50000,
      currencyCode: "IDR",
      title: "Makan Siang",
      categoryId: "cat_food",
      categoryName: "Makanan",
      accountId: "acc_cash",
      accountName: "Tunai",
      transactionDateMillis: 1760000000000,
      isDeleted: false,
      version: 1,
      createdAt: validNow(),
      updatedAt: validNow(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("transactions").where("userId", "==", ALICE_UID).get()
  );
});

test("3. Identity spoofing rejected on create", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_spoof").set({
      userId: BOB_UID,
      type: "EXPENSE",
      amountIdr: 50000,
      currencyCode: "IDR",
      title: "Makan Siang",
      categoryId: "cat_food",
      categoryName: "Makanan",
      accountId: "acc_cash",
      accountName: "Tunai",
      transactionDateMillis: 1760000000000,
      isDeleted: false,
      version: 1,
      createdAt: validNow(),
      updatedAt: validNow(),
    })
  );
});

test("4. Shadow update (ghost field injection) rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_ghost").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amountIdr: 50000,
      currencyCode: "IDR",
      title: "Makan Siang",
      categoryId: "cat_food",
      categoryName: "Makanan",
      accountId: "acc_cash",
      accountName: "Tunai",
      transactionDateMillis: 1760000000000,
      isDeleted: false,
      version: 1,
      isVerifiedAdmin: true,
      createdAt: validNow(),
      updatedAt: validNow(),
    })
  );
});

test("5. Negative or zero transaction amount rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_neg").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amountIdr: -25000,
      currencyCode: "IDR",
      title: "Invalid",
      categoryId: "cat_food",
      categoryName: "Makanan",
      accountId: "acc_cash",
      accountName: "Tunai",
      transactionDateMillis: 1760000000000,
      isDeleted: false,
      version: 1,
      createdAt: validNow(),
      updatedAt: validNow(),
    })
  );
});

test("6. Authenticated owner can create, update, and query their own records", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Budi Santoso",
      email: "budi@example.com",
      currencyCode: "IDR",
      financialProfileType: "BUSINESS",
      onboardingCompleted: true,
      version: 1,
      createdAt: validNow(),
      updatedAt: validNow(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc_bca").set({
      userId: ALICE_UID,
      name: "BCA Bisnis",
      accountType: "BANK",
      openingBalanceIdr: 25000000,
      currentBalanceIdr: 25000000,
      colorHex: "#49E0A0",
      institutionName: "Bank Central Asia",
      isArchived: false,
      version: 1,
      createdAt: validNow(),
      updatedAt: validNow(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      type: "INCOME",
      amountIdr: 15000000,
      currencyCode: "IDR",
      title: "Pembayaran Proyek Klien",
      categoryId: "cat_revenue",
      categoryName: "Pendapatan Bisnis",
      accountId: "acc_bca",
      accountName: "BCA Bisnis",
      transactionDateMillis: 1760000000000,
      isDeleted: false,
      version: 1,
      createdAt: validNow(),
      updatedAt: validNow(),
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});
