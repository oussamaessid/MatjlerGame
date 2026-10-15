"use strict";
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {createService} = require("./service");
initializeApp();
// This is the game's existing published puzzle source, not a new invented endpoint.
const SOURCE = "https://raw.githubusercontent.com/oussamaessid/MatjlerData/refs/heads/main/MatjlerData.json";
async function puzzleFor(day, mode) {
  const response = await fetch(SOURCE, {signal: AbortSignal.timeout(15000)});
  if (!response.ok) throw new HttpsError("unavailable", "Énigmes indisponibles.");
  const groups = await response.json();
  const questions = groups.find(g => g.mode === mode)?.questions;
  if (!questions?.length) throw new HttpsError("unavailable", "Mode indisponible.");
  const index = Math.max(0, Math.floor((Date.parse(day + "T00:00:00Z") - Date.parse("2026-01-01T00:00:00Z")) / 86400000)) % questions.length;
  const question = questions[index];
  return {number: question.level, solution: question.solution};
}
const service = createService(getFirestore(), puzzleFor, Date.now, (code, message) => {throw new HttpsError(code, message);});
for (const name of ["saveProfile", "beginAttempt", "finishAttempt", "leaderboard"]) {
  exports[name] = onCall({region: "europe-west1", maxInstances: 10, timeoutSeconds: 30}, service[name]);
}
