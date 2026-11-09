const api = "/api/v1";
const authKey = "behaviorguard-auth";
let riskData = [];
let caseData = [];
let toastTimer;

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
}

function showAuth(message = "") {
  document.querySelector("#auth-overlay").hidden = false;
  document.querySelector("#auth-error").textContent = message;
  requestAnimationFrame(() => document.querySelector('#auth-form [name="username"]').focus());
}

function syncNavigation() {
  const activeHash = ["#cases", "#policies"].includes(window.location.hash) ? window.location.hash : "#risk";
  document.querySelectorAll(".topbar nav a").forEach((link) => {
    const active = link.hash === activeHash;
    link.classList.toggle("nav-active", active);
    if (active) link.setAttribute("aria-current", "page");
    else link.removeAttribute("aria-current");
  });
}

async function request(path, options = {}) {
  const headers = new Headers(options.headers || {});
  headers.set("Content-Type", "application/json");
  const credentials = sessionStorage.getItem(authKey);
  if (credentials) headers.set("Authorization", `Basic ${credentials}`);
  const response = await fetch(`${api}${path}`, { ...options, headers });
  if (response.status === 401) {
    sessionStorage.removeItem(authKey);
    showAuth();
    throw new Error("AUTH_REQUIRED");
  }
  if (!response.ok) throw new Error(response.status === 403 ? "Your account is not permitted to perform this action." : `Request failed (${response.status})`);
  return response.status === 204 ? null : response.json();
}

function toast(message) {
  const element = document.querySelector("#toast");
  element.textContent = message;
  element.classList.add("visible");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => element.classList.remove("visible"), 2800);
}

function tierClass(score) {
  if (score >= 85) return "critical";
  if (score >= 70) return "high";
  if (score >= 30) return "guarded";
  return "low";
}

function initials(name) {
  return name.split(/\s+/).slice(0, 2).map((part) => part[0]).join("").toUpperCase();
}

function renderRisk() {
  const rows = document.querySelector("#user-list");
  rows.innerHTML = riskData.map((user) => {
    const level = tierClass(user.score);
    return `<button class="user-row" data-user="${escapeHtml(user.userId)}" aria-label="Inspect ${escapeHtml(user.displayName)}, score ${user.score}">
      <span class="person"><span class="person-avatar">${escapeHtml(initials(user.displayName))}</span><span><span class="person-name">${escapeHtml(user.displayName)}</span><span class="person-id">${escapeHtml(user.userId)}</span></span></span>
      <span class="department">${escapeHtml(user.department)}</span>
      <span class="score-wrap"><span class="score-track"><span class="score-fill ${level}" style="width:${user.score}%"></span></span><span class="score-number">${user.score}</span></span>
      <span class="risk-level ${level}">${escapeHtml(user.tier)}</span>
    </button>`;
  }).join("");
  document.querySelector("#metric-users").textContent = riskData.length;
  document.querySelector("#metric-high-risk").textContent = riskData.filter((user) => user.score >= 70).length;
  document.querySelector("#metric-cases").textContent = caseData.filter((item) => item.status === "OPEN").length;
  document.querySelector("#updated-label").textContent = `UPDATED ${new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`;
}

function showProfile(userId) {
  const user = riskData.find((candidate) => candidate.userId === userId);
  if (!user) return;
  document.querySelectorAll(".user-row").forEach((row) => row.classList.toggle("selected", row.dataset.user === userId));
  document.querySelector("#profile-title").textContent = user.displayName;
  const body = document.querySelector("#profile-body");
  body.innerHTML = `<div class="profile-overview"><div class="score-ring" style="--score:${user.score}"><strong>${user.score}</strong></div><div><div class="profile-name">${escapeHtml(user.displayName)}</div><div class="profile-meta">${escapeHtml(user.department)} · ${escapeHtml(user.tier)} risk · ${escapeHtml(user.userId)}</div></div></div>
    <div class="baseline"><span class="baseline-mark">⌁</span><span>${escapeHtml(user.baseline)}</span></div>
    <div class="factor-heading"><span>ACTIVE RISK FACTORS</span><span>${user.factors.length} FACTORS</span></div>
    <div class="factor-list">${user.factors.length ? user.factors.map((factor) => `<div class="factor-row"><div><div class="factor-name">${escapeHtml(factor.label)}</div><div class="factor-explanation">${escapeHtml(factor.explanation)}</div><div class="factor-evidence">EVIDENCE ${factor.evidenceEventIds.map(escapeHtml).join(" · ")}</div></div><span class="factor-points">+${factor.points}</span></div>`).join("") : '<div class="profile-empty">No active anomaly factors. This user is within their current baseline.</div>'}</div>`;
}

function renderCases() {
  const list = document.querySelector("#case-list");
  document.querySelector("#case-count").textContent = `${caseData.length} OPEN`;
  document.querySelector("#case-nav-count").textContent = caseData.length;
  list.innerHTML = caseData.length ? caseData.map((item) => {
    const user = riskData.find((candidate) => candidate.userId === item.userId);
    return `<div class="case-row"><span class="case-title">${escapeHtml(item.title)}</span><span class="case-user">${escapeHtml(user?.displayName || item.userId)}</span><span class="case-severity">${escapeHtml(item.severity)}</span><span class="case-time">${item.evidenceEventIds.length} evidence events · ${escapeHtml(item.status)}</span></div>`;
  }).join("") : '<div class="empty-case">No cases to review. Trigger the demo scenario to see evidence linked to a user risk change.</div>';
  document.querySelector("#metric-cases").textContent = caseData.filter((item) => item.status === "OPEN").length;
}

async function refresh() {
  try {
    [riskData, caseData] = await Promise.all([request("/users/risk"), request("/cases")]);
    renderRisk();
    renderCases();
  } catch (error) {
    if (error.message !== "AUTH_REQUIRED") toast(`Could not load profiles: ${error.message}`);
  }
}

document.querySelector("#auth-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const username = form.elements.username.value.trim();
  const password = form.elements.password.value;
  sessionStorage.setItem(authKey, btoa(`${username}:${password}`));
  try {
    await request("/users/risk");
    form.reset();
    document.querySelector("#auth-overlay").hidden = true;
    await refresh();
  } catch (error) {
    sessionStorage.removeItem(authKey);
    showAuth(error.message === "AUTH_REQUIRED"
      ? "Sign-in failed. Check the username and password."
      : "Could not reach the workspace. Check the connection and try again.");
  }
});

document.querySelector("#sign-out").addEventListener("click", () => {
  sessionStorage.removeItem(authKey);
  riskData = [];
  caseData = [];
  renderRisk();
  renderCases();
  showAuth();
});

document.querySelector("#user-list").addEventListener("click", (event) => {
  const row = event.target.closest(".user-row");
  if (row) showProfile(row.dataset.user);
});

document.querySelector("#close-profile").addEventListener("click", () => {
  document.querySelector("#profile-title").textContent = "Select an identity";
  document.querySelector("#profile-body").innerHTML = '<div class="profile-empty">Select a user to review their behavior baseline and risk factors.</div>';
  document.querySelectorAll(".user-row").forEach((row) => row.classList.remove("selected"));
});

document.querySelector("#run-scenario").addEventListener("click", async (event) => {
  const button = event.currentTarget;
  button.disabled = true;
  try {
    await request("/demo/account-takeover", { method: "POST" });
    await refresh();
    showProfile("usr-104");
    toast("Synthetic case opened. Each score contribution links to evidence.");
  } catch (error) {
    toast(error.message);
  } finally {
    button.disabled = false;
  }
});

window.addEventListener("hashchange", syncNavigation);
syncNavigation();
if (sessionStorage.getItem(authKey)) refresh();
else showAuth();