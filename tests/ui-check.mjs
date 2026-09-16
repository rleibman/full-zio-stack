// Checks a generated full-stack app in a real browser engine: drives headless Chrome over the DevTools protocol
// (no npm packages needed; requires Node 22+ and google-chrome), creates a ModelObject through the dialog, and checks
// it shows up in the table with no console errors.
//
// Usage: node tests/ui-check.mjs [base URL, default http://localhost:8080] [screenshot directory]
// The server must be running and serving a client build.
import { spawn } from "node:child_process";
import { mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";

const baseUrl = process.argv[2] ?? "http://localhost:8080";
const screenshots = process.argv[3];
const port = 9333;
const chrome = spawn("google-chrome", [
  "--headless=new", "--disable-gpu", "--no-sandbox", `--remote-debugging-port=${port}`,
  `--user-data-dir=${mkdtempSync(path.join(tmpdir(), "ui-check-"))}`, "--window-size=1280,800", "about:blank",
], { stdio: "ignore" });

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
let target;
for (let i = 0; i < 50 && !target; i++) {
  await sleep(200);
  try { target = (await (await fetch(`http://127.0.0.1:${port}/json/list`)).json()).find((t) => t.type === "page"); } catch {}
}
if (!target) { console.error("Couldn't start headless Chrome"); chrome.kill(); process.exit(1); }
const ws = new WebSocket(target.webSocketDebuggerUrl);
await new Promise((r) => ws.addEventListener("open", r));

let nextId = 1;
const pending = new Map();
const consoleErrors = [];
ws.addEventListener("message", (event) => {
  const msg = JSON.parse(event.data);
  if (msg.id && pending.has(msg.id)) { pending.get(msg.id)(msg); pending.delete(msg.id); }
  if (msg.method === "Runtime.exceptionThrown") consoleErrors.push(msg.params.exceptionDetails.text);
  if (msg.method === "Runtime.consoleAPICalled" && msg.params.type === "error")
    consoleErrors.push(msg.params.args.map((a) => a.value ?? a.description).join(" "));
});
const send = (method, params = {}) =>
  new Promise((resolve) => { const id = nextId++; pending.set(id, resolve); ws.send(JSON.stringify({ id, method, params })); });
const evaluate = async (expression) =>
  (await send("Runtime.evaluate", { expression, awaitPromise: true, returnByValue: true })).result?.result?.value;
const screenshot = async (name) => {
  if (screenshots)
    writeFileSync(path.join(screenshots, name), Buffer.from((await send("Page.captureScreenshot", { format: "png" })).result.data, "base64"));
};
const clickButton = (text) =>
  evaluate(`(() => { const b = [...document.querySelectorAll('button, [role=tab]')].find(b => b.textContent.trim() === ${JSON.stringify(text)}); if (!b) return false; b.click(); return true; })()`);
// React-controlled inputs need the native value setter (input or textarea) plus an input event.
const fill = (label, value) =>
  evaluate(`(() => {
    const l = [...document.querySelectorAll('label')].find(l => l.textContent.replace('*', '').trim() === ${JSON.stringify(label)});
    const input = l && document.getElementById(l.htmlFor);
    if (!input) return false;
    Object.getOwnPropertyDescriptor(Object.getPrototypeOf(input), 'value').set.call(input, ${JSON.stringify(value)});
    input.dispatchEvent(new Event('input', { bubbles: true }));
    return true;
  })()`);
const tableText = () => evaluate(`[...document.querySelectorAll('tbody tr')].map(r => r.innerText.replace(/\\s+/g, ' ')).join(' | ')`);

await send("Page.enable");
await send("Runtime.enable");
await send("Page.navigate", { url: baseUrl });
await sleep(3000);

const name = `UI check ${Date.now()}`;
const results = {};
results.rendered = await evaluate(`!!document.querySelector('header') && !!document.querySelector('table')`);
await screenshot("ui-1-page.png");
results.clickedNew = await clickButton("New");
await sleep(800);
results.filledName = await fill("Name", name);
results.filledDescription = await fill("Description", "Created by tests/ui-check.mjs");
await screenshot("ui-2-dialog.png");
results.clickedSave = await clickButton("Save");
await sleep(2000);
results.tableAfter = await tableText();
results.dialogClosed = !(await evaluate(`!!document.querySelector('[role=dialog]')`));
await screenshot("ui-3-saved.png");
results.consoleErrors = consoleErrors;

const ok = results.rendered && results.clickedNew && results.filledName && results.clickedSave && results.dialogClosed &&
  (results.tableAfter ?? "").includes(name) && consoleErrors.length === 0;
console.log(JSON.stringify(results, null, 2));
console.log(ok ? "UI check passed" : "UI check FAILED");
ws.close();
chrome.kill();
process.exit(ok ? 0 : 1);
