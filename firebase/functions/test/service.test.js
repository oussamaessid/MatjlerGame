const {test} = require("node:test");
const assert = require("node:assert/strict");
const {createService, periods} = require("../service");
function setup() {
  const store = new Map(); let time = Date.parse("2026-10-14T12:00:00Z"), queue = Promise.resolve();
  const db = {doc: path => ({path}), runTransaction: fn => {
    const work = queue.then(async () => {
      const writes = [];
      const result = await fn({get: async ref => ({exists: store.has(ref.path), data: () => store.get(ref.path)}),
        set: (ref, data) => writes.push(() => store.set(ref.path, data)),
        update: (ref, data) => writes.push(() => store.set(ref.path, {...store.get(ref.path), ...data}))});
      writes.forEach(write => write()); return result;
    }); queue = work.catch(() => {}); return work;
  }};
  return {store, service: createService(db, async () => ({number: 10, solution: "1+2"}), () => time),
    tick: ms => {time += ms;}, request: (data, uid = "player-a") => ({auth: {uid}, data})};
}
test("Tunis day and month change at 23:00 UTC", () => {
 assert.deepEqual(periods(Date.parse("2026-10-31T23:00:00Z")), {day: "2026-11-01", month: "2026-11"});
});
test("anonymous callers and invalid modes are rejected", async () => {
 const {service, request} = setup();
 await assert.rejects(service.beginAttempt({data: {}}), {code: "unauthenticated"});
 await assert.rejects(service.beginAttempt(request({mode: "FAKE"})), {code: "invalid-argument"});
});
test("start and concurrent score retries only aggregate once", async () => {
 const {service, request, tick, store} = setup();
 const data = {mode: "EASY", day: "2026-10-14", level: 10};
 const first = await service.beginAttempt(request(data)); tick(5000);
 assert.equal((await service.beginAttempt(request(data))).id, first.id);
 const finish = request({id: first.id, won: true, answer: "1+2"});
 await Promise.all([service.finishAttempt(finish), service.finishAttempt(finish)]);
 assert.equal(store.get("dailyBoards/2026-10-14/modes/EASY/players/player-a").correctCount, 1);
 assert.equal(store.get("monthlyBoards/2026-10/modes/EASY/players/player-a").totalMs, 5000);
});
test("other player and wrong answers cannot write scores", async () => {
 const {service, request, store} = setup();
 const {id} = await service.beginAttempt(request({mode: "EASY", day: "2026-10-14", level: 10}));
 await assert.rejects(service.finishAttempt(request({id, won: true, answer: "1+2"}, "other")), {code: "permission-denied"});
 await assert.rejects(service.finishAttempt(request({id, won: true, answer: "wrong"})), {code: "invalid-argument"});
 assert.equal(store.size, 1);
});
test("loss is immutable and does not enter boards", async () => {
 const {service, request, store} = setup();
 const {id} = await service.beginAttempt(request({mode: "EASY", day: "2026-10-14", level: 10}));
 await service.finishAttempt(request({id, won: false}));
 assert.equal((await service.finishAttempt(request({id, won: true, answer: "1+2"}))).counted, false);
 assert.equal(store.size, 1);
});
test("a next-day finish stays in the start period", async () => {
 const {service, request, tick, store} = setup();
 const {id} = await service.beginAttempt(request({mode: "EASY", day: "2026-10-14", level: 10}));
 tick(86400000);
 await service.finishAttempt(request({id, won: true, answer: "1+2"}));
 assert.ok(store.has("dailyBoards/2026-10-14/modes/EASY/players/player-a"));
 assert.equal(store.has("dailyBoards/2026-10-15/modes/EASY/players/player-a"), false);
});
test("profiles validate inputs and createOnly preserves edits", async () => {
 const {service, request} = setup();
 await assert.rejects(service.saveProfile(request({name: "x", avatar: 9})), {code: "invalid-argument"});
 await service.saveProfile(request({name: "Alice", avatar: 2}));
 assert.equal((await service.saveProfile(request({name: "Guest", avatar: 0, createOnly: true}))).name, "Alice");
});

test("two players appear globally with score and speed ordering, updated profile and pagination", async () => {
 const rows = [
   {uid: "a", correctCount: 1, totalMs: 100},
   {uid: "b", correctCount: 2, totalMs: 500},
   {uid: "c", correctCount: 2, totalMs: 200}
 ];
 const profiles = {a: {name: "Alice", avatar: 1}, b: {name: "Bob", avatar: 2}, c: {name: "Chloe modifiée", avatar: 3}};
 const order = []; let after = null;
 const query = {orderBy(field, direction) {order.push([field, direction]); return this;},
   startAfter(...cursor) {after = cursor; return this;}, limit(n) {assert.equal(n, 30); return this;},
   async get() {
     let result = [...rows].sort((a,b) => b.correctCount-a.correctCount || a.totalMs-b.totalMs || a.uid.localeCompare(b.uid));
     if (after) result = result.slice(result.findIndex(r => r.uid === after[2]) + 1);
     return {docs: result.map(row => ({data: () => row}))};
   }};
 const service = createService({doc: path => ({path}), collection: path => {
   assert.equal(path, "dailyBoards/2026-10-14/modes/EASY/players"); return query;
 }, getAll: async (...refs) => refs.map(ref => ({exists: true, data: () => profiles[ref.path.split("/").at(-1)]}))},
   null, () => Date.parse("2026-10-14T12:00:00Z"));
 const response = await service.leaderboard({auth: {uid: "a"}, data: {monthly: false, mode: "EASY"}});
 assert.deepEqual(order, [["correctCount","desc"],["totalMs","asc"],["uid","asc"]]);
 assert.deepEqual(response.entries.map(r => r.uid), ["c", "b", "a"]);
 assert.equal(response.entries[0].name, "Chloe modifiée");
 const page = await service.leaderboard({auth: {uid: "a"}, data: {monthly: false, mode: "EASY", cursor: {...rows[2], mode: "EASY", period: "2026-10-14"}}});
 assert.deepEqual(page.entries.map(r => r.uid), ["b", "a"]);
});
test("expired attempts cannot add a score", async () => {
 const {service, request, tick, store} = setup();
 const {id} = await service.beginAttempt(request({mode: "EASY", day: "2026-10-14", level: 10}));
 tick(49 * 3600000);
 await assert.rejects(service.finishAttempt(request({id, won: true, answer: "1+2"})), {code: "deadline-exceeded"});
 assert.equal(store.size, 1);
});

test("wins and concurrent retries stay separate in every difficulty and period", async () => {
 const {service, request, tick, store} = setup();
 for (const [mode, duration] of [["EASY",1000],["MEDIUM",5000],["HARD",9000]]) {
  const {id} = await service.beginAttempt(request({mode, day: "2026-10-14", level: 10}));
  tick(duration);
  await Promise.all([service.finishAttempt(request({id, won: true, answer: "1+2"})),
                    service.finishAttempt(request({id, won: true, answer: "1+2"}))]);
  for (const prefix of ["dailyBoards/2026-10-14", "monthlyBoards/2026-10"]) {
   assert.deepEqual(store.get(`${prefix}/modes/${mode}/players/player-a`), {uid:"player-a",correctCount:1,totalMs:duration});
  }
 }
 assert.equal([...store.keys()].filter(key => key.includes("Boards/")).length, 6);
});
test("all six leaderboard selections query the requested period and difficulty", async () => {
 const paths = [];
 const query = {orderBy() {return this;}, limit() {return this;}, async get() {return {docs:[]};}};
 const service = createService({collection(path) {paths.push(path); return query;}}, null,
   () => Date.parse("2026-10-14T12:00:00Z"));
 for (const monthly of [false,true]) for (const mode of ["EASY","MEDIUM","HARD"]) {
  const result = await service.leaderboard({auth:{uid:"a"}, data:{monthly,mode}});
  assert.equal(paths.at(-1), `${monthly ? "monthlyBoards/2026-10" : "dailyBoards/2026-10-14"}/modes/${mode}/players`);
  assert.equal(result.mode, mode);
 }
 await assert.rejects(service.leaderboard({auth:{uid:"a"}, data:{monthly:false,mode:"INVALID"}}), {code:"invalid-argument"});
 await assert.rejects(service.leaderboard({auth:{uid:"a"}, data:{monthly:false}}), {code:"invalid-argument"});
});
test("a cursor cannot cross periods or difficulty", async () => {
 const query = {orderBy() {return this;}};
 const service = createService({collection() {return query;}}, null,
   () => Date.parse("2026-10-14T12:00:00Z"));
 const cursor = {correctCount:1,totalMs:1000,uid:"a",mode:"EASY",period:"2026-10-14"};
 await assert.rejects(service.leaderboard({auth:{uid:"a"},data:{monthly:false,mode:"HARD",cursor}}),{code:"invalid-argument"});
 await assert.rejects(service.leaderboard({auth:{uid:"a"},data:{monthly:true,mode:"EASY",cursor}}),{code:"invalid-argument"});
});
test("monthly totals combine days of the same mode without mixing difficulties", async () => {
 const {service, request, tick, store} = setup();
 const first = await service.beginAttempt(request({mode:"EASY",day:"2026-10-14",level:10}));
 tick(1000);
 await service.finishAttempt(request({id:first.id,won:true,answer:"1+2"}));
 tick(86400000);
 const second = await service.beginAttempt(request({mode:"EASY",day:"2026-10-15",level:10}));
 tick(2000);
 await service.finishAttempt(request({id:second.id,won:true,answer:"1+2"}));
 const hard = await service.beginAttempt(request({mode:"HARD",day:"2026-10-15",level:10}));
 tick(8000);
 await service.finishAttempt(request({id:hard.id,won:true,answer:"1+2"}));
 assert.deepEqual(store.get("monthlyBoards/2026-10/modes/EASY/players/player-a"),{uid:"player-a",correctCount:2,totalMs:3000});
 assert.equal(store.get("dailyBoards/2026-10-14/modes/EASY/players/player-a").totalMs,1000);
 assert.equal(store.get("dailyBoards/2026-10-15/modes/EASY/players/player-a").totalMs,2000);
 assert.deepEqual(store.get("monthlyBoards/2026-10/modes/HARD/players/player-a"),{uid:"player-a",correctCount:1,totalMs:8000});
});

test("profile accepts all Ludo avatar IDs and preserves legacy IDs", async () => {
 const {service, request} = setup();
 for (const avatar of [0,7,8,55]) {
  const profile = await service.saveProfile(request({name:"Joueur Ludo",avatar}));
  assert.equal(profile.avatar,avatar);
 }
 for (const avatar of [-1,56,1.5,"8"]) {
  await assert.rejects(service.saveProfile(request({name:"Joueur Ludo",avatar})),{code:"invalid-argument"});
 }
});
