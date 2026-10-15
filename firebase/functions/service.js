"use strict";
const AVATAR_COUNT = 56;
const DEFAULT_AVATAR = 8;
const MODES = new Set(["EASY", "MEDIUM", "HARD"]);
function periods(ms) {
  const parts = Object.fromEntries(new Intl.DateTimeFormat("en", {
    timeZone: "Africa/Tunis", year: "numeric", month: "2-digit", day: "2-digit"
  }).formatToParts(new Date(ms)).map(p => [p.type, p.value]));
  const day = `${parts.year}-${parts.month}-${parts.day}`;
  return {day, month: day.slice(0, 7)};
}
function createService(db, puzzleFor, now = Date.now, fail = (code, message) => {
  throw Object.assign(new Error(message), {code});
}) {
  const uidOf = req => { if (!req.auth?.uid) fail("unauthenticated", "Connexion requise."); return req.auth.uid; };
  const modeOf = data => { if (!MODES.has(data?.mode)) fail("invalid-argument", "Mode invalide."); return data.mode; };
  const profileRef = uid => db.doc(`publicPlayers/${encodeURIComponent(uid)}`);
  return {
    async saveProfile(req) {
      const uid = uidOf(req), data = req.data || {};
      const name = typeof data.name === "string" ? data.name.trim() : "";
      if (name.length < 2 || name.length > 24 || /[\x00-\x1f\x7f-\x9f]/.test(name) ||
          !Number.isInteger(data.avatar) || data.avatar < 0 || data.avatar >= AVATAR_COUNT) fail("invalid-argument", "Profil invalide.");
      return db.runTransaction(async tx => {
        const ref = profileRef(uid), existing = await tx.get(ref);
        if (data.createOnly === true && existing.exists) return existing.data();
        const profile = {uid, name, avatar: data.avatar};
        tx.set(ref, profile);
        return profile;
      });
    },
    async beginAttempt(req) {
      const uid = uidOf(req), mode = modeOf(req.data), stamp = now(), {day, month} = periods(stamp);
      if (req.data.day !== day) fail("failed-precondition", "Le jour a changé. Revenez au menu.");
      const level = await puzzleFor(day, mode);
      if (req.data.level !== level.number) fail("failed-precondition", "Énigme différente. Rechargez le jeu.");
      const id = `${encodeURIComponent(uid)}_${day}_${mode}`, ref = db.doc(`attempts/${id}`);
      return db.runTransaction(async tx => {
        const snapshot = await tx.get(ref);
        if (!snapshot.exists) tx.set(ref, {uid, day, month, mode, startedAt: stamp,
          solution: level.solution, finished: false});
        return {id, day, finished: snapshot.exists && snapshot.data().finished === true,
          won: snapshot.exists && snapshot.data().won === true};
      });
    },
    async finishAttempt(req) {
      const uid = uidOf(req), data = req.data || {};
      if (typeof data.id !== "string" || data.id.length > 220 || data.id.includes("/") || typeof data.won !== "boolean")
        fail("invalid-argument", "Tentative invalide.");
      const ref = db.doc(`attempts/${data.id}`), stamp = now();
      return db.runTransaction(async tx => {
        const snap = await tx.get(ref);
        if (!snap.exists || snap.data().uid !== uid) fail("permission-denied", "Tentative introuvable.");
        const attempt = snap.data();
        if (attempt.finished) return {counted: attempt.won, durationMs: attempt.durationMs};
        if (data.won && (typeof data.answer !== "string" || data.answer !== attempt.solution))
          fail("invalid-argument", "Réponse incorrecte.");
        // Accept next-day retries, but do not rank attempts older than 48 hours.
        if (stamp - attempt.startedAt > 48 * 60 * 60 * 1000) fail("deadline-exceeded", "Tentative expirée.");
        const durationMs = Math.max(1, stamp - attempt.startedAt);
        const daily = db.doc(`dailyBoards/${attempt.day}/modes/${attempt.mode}/players/${encodeURIComponent(uid)}`);
        const monthly = db.doc(`monthlyBoards/${attempt.month}/modes/${attempt.mode}/players/${encodeURIComponent(uid)}`);
        const boards = data.won ? await Promise.all([tx.get(daily), tx.get(monthly)]) : [];
        tx.update(ref, {finished: true, won: data.won, durationMs, finishedAt: stamp});
        boards.forEach((board, i) => {
          const old = board.exists ? board.data() : {correctCount: 0, totalMs: 0};
          tx.set(i === 0 ? daily : monthly, {uid, correctCount: old.correctCount + 1, totalMs: old.totalMs + durationMs});
        });
        return {counted: data.won, durationMs};
      });
    },
    async leaderboard(req) {
      uidOf(req);
      const data = req.data || {}, {day, month} = periods(now());
      if (typeof data.monthly !== "boolean") fail("invalid-argument", "Période invalide.");
      const mode = modeOf(data);
      const period = data.monthly ? month : day;
      let query = db.collection(`${data.monthly ? "monthlyBoards" : "dailyBoards"}/${period}/modes/${mode}/players`)
        .orderBy("correctCount", "desc").orderBy("totalMs", "asc").orderBy("uid", "asc");
      if (data.cursor != null) {
        const c = data.cursor;
        if (c.mode !== mode || c.period !== period) fail("invalid-argument", "Curseur d’une autre période ou difficulté.");
        if (!Number.isSafeInteger(c.correctCount) || c.correctCount < 1 || !Number.isSafeInteger(c.totalMs) || c.totalMs < 1 ||
          typeof c.uid !== "string" || c.uid.length > 128) fail("invalid-argument", "Curseur invalide.");
        query = query.startAfter(c.correctCount, c.totalMs, c.uid);
      }
      const result = await query.limit(30).get();
      const profiles = result.docs.length ? await db.getAll(...result.docs.map(d => profileRef(d.data().uid))) : [];
      const entries = result.docs.map((doc, i) => {
        const row = doc.data(), profile = profiles[i].exists ? profiles[i].data() : {name: "Joueur", avatar: DEFAULT_AVATAR};
        return {...row, name: profile.name, avatar: profile.avatar};
      });
      return {period, mode, zone: "Africa/Tunis", entries,
        cursor: result.docs.length === 30 ? {...result.docs.at(-1).data(), mode, period} : null};
    }
  };
}
module.exports = {createService, periods};
