/* AuraSpend — enhanced client-side application
   NOTE: Authentication here is local/demo authentication. Do not use it for
   real accounts or sensitive financial data without a backend/auth provider.
*/
const STORAGE = {
  users: "auraspend_users_v2",
  session: "auraspend_session_v2",
  expensesPrefix: "auraspend_expenses_v2_",
  settingsPrefix: "auraspend_settings_v2_"
};

const CURRENCIES = {
  USD: { symbol: "$", name: "US Dollar" },
  INR: { symbol: "₹", name: "Indian Rupee" },
  EUR: { symbol: "€", name: "Euro" },
  GBP: { symbol: "£", name: "British Pound" },
  AED: { symbol: "د.إ", name: "UAE Dirham" },
  CAD: { symbol: "C$", name: "Canadian Dollar" },
  AUD: { symbol: "A$", name: "Australian Dollar" },
  JPY: { symbol: "¥", name: "Japanese Yen" },
  SGD: { symbol: "S$", name: "Singapore Dollar" }
};

const CATEGORIES = {
  food: { label: "Food", emoji: "🍔" },
  transport: { label: "Transport", emoji: "🚗" },
  rent: { label: "Rent", emoji: "🏠" },
  fun: { label: "Fun", emoji: "🎮" },
  utilities: { label: "Utilities", emoji: "💡" },
  others: { label: "Others", emoji: "✦" }
};

let users = [];
let currentUser = null;
let expenses = [];
let settings = { currency: "USD", budget: 2000, theme: "dark" };
let activeView = "dashboard";
let activeFilter = "all";
let reportPeriod = "weekly";
let selectedCategory = "";
let editSelectedCategory = "";

const $ = (id) => document.getElementById(id);
const $$ = (selector) => [...document.querySelectorAll(selector)];

document.addEventListener("DOMContentLoaded", init);

// Defensive fallback for the auth tabs.
document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll(".auth-tab").forEach(tab => {
    tab.setAttribute("type", "button");
  });
});

function init() {
  loadUsers();
  populateCurrencySelects();
  bindAuth();
  bindApp();
  const session = localStorage.getItem(STORAGE.session);
  if (session) {
    currentUser = users.find(u => u.email === session) || null;
  }
  if (currentUser) enterApp();
  else showAuth();
}

function loadUsers() {
  try { users = JSON.parse(localStorage.getItem(STORAGE.users)) || []; }
  catch { users = []; }
}

function saveUsers() {
  localStorage.setItem(STORAGE.users, JSON.stringify(users));
}

function userKey() {
  return currentUser ? encodeURIComponent(currentUser.email.toLowerCase()) : "";
}

function loadUserData() {
  try {
    expenses = JSON.parse(localStorage.getItem(STORAGE.expensesPrefix + userKey())) || [];
    settings = { ...{ currency: currentUser.currency || "USD", budget: 2000, theme: "dark" },
      ...(JSON.parse(localStorage.getItem(STORAGE.settingsPrefix + userKey())) || {}) };
  } catch {
    expenses = [];
    settings = { currency: currentUser.currency || "USD", budget: 2000, theme: "dark" };
  }
}

function saveExpenses() {
  if (!currentUser) throw new Error("No active user session.");
  localStorage.setItem(
    STORAGE.expensesPrefix + userKey(),
    JSON.stringify(expenses)
  );
}

function saveSettings() {
  localStorage.setItem(STORAGE.settingsPrefix + userKey(), JSON.stringify(settings));
}

function bindAuth() {
  // Event delegation makes the auth tabs reliable even if the DOM is
  // re-rendered or the page is opened in a browser with stricter handling.
  document.addEventListener("click", (event) => {
    const tab = event.target.closest(".auth-tab");
    if (!tab) return;
    event.preventDefault();
    switchAuth(tab.dataset.authTab);
  });

  $("loginForm").addEventListener("submit", login);
  $("signupForm").addEventListener("submit", signup);
}

function switchAuth(tab) {
  const loginTab = document.querySelector('[data-auth-tab="login"]');
  const signupTab = document.querySelector('[data-auth-tab="signup"]');

  loginTab?.classList.toggle("active", tab === "login");
  signupTab?.classList.toggle("active", tab === "signup");

  const showLogin = tab === "login";
  const showSignup = tab === "signup";

  $("loginForm").classList.toggle("hidden", !showLogin);
  $("signupForm").classList.toggle("hidden", !showSignup);
  $("loginForm").hidden = !showLogin;
  $("signupForm").hidden = !showSignup;

  // Clear old validation/input state when changing forms.
  if (tab === "signup") {
    $("signupName")?.focus();
  } else {
    $("loginEmail")?.focus();
  }
}

function login(e) {
  e.preventDefault();
  const email = $("loginEmail").value.trim().toLowerCase();
  const password = $("loginPassword").value;
  const user = users.find(u => u.email === email && u.password === password);
  if (!user) return showToast("Invalid email or password");
  currentUser = user;
  localStorage.setItem(STORAGE.session, currentUser.email);
  enterApp();
}

function signup(e) {
  e.preventDefault();
  const name = $("signupName").value.trim();
  const email = $("signupEmail").value.trim().toLowerCase();
  const password = $("signupPassword").value;
  const currency = $("signupCurrency").value;
  if (users.some(u => u.email === email)) return showToast("An account with this email already exists");
  if (password.length < 6) return showToast("Password must be at least 6 characters");
  currentUser = { id: crypto.randomUUID ? crypto.randomUUID() : String(Date.now()), name, email, password, currency };
  users.push(currentUser);
  saveUsers();
  localStorage.setItem(STORAGE.session, currentUser.email);
  expenses = [];
  settings = { currency, budget: 2000, theme: "dark" };
  saveExpenses();
  saveSettings();
  enterApp();
  showToast("Account created successfully");
}

function showAuth() {
  $("authView").classList.remove("hidden");
  $("appView").classList.add("hidden");
}

function enterApp() {
  loadUserData();
  applyTheme();
  $("authView").classList.add("hidden");
  $("appView").classList.remove("hidden");
  $("settingsName").textContent = currentUser.name;
  $("settingsEmail").textContent = currentUser.email;
  $("profileInitial").textContent = currentUser.name.charAt(0).toUpperCase();
  $("settingsAvatar").textContent = currentUser.name.charAt(0).toUpperCase();
  $("greeting").textContent = `Welcome back, ${currentUser.name.split(" ")[0]}.`;
  $("currencySelect").value = settings.currency;
  $("budgetInput").value = settings.budget;
  updateCurrencyLabels();
  setToday();
  buildCategoryGrids();
  renderApp();
}

function logout() {
  localStorage.removeItem(STORAGE.session);
  currentUser = null;
  expenses = [];
  showAuth();
  switchAuth("login");
  $("loginForm").reset();
  showToast("Logged out");
}

function bindApp() {
  document.addEventListener("click", (event) => {
    const btn = event.target.closest(".nav-item[data-view]");
    if (!btn) return;
    event.preventDefault();
    switchView(btn.dataset.view);
  });

  const addExpenseButton = $("addExpenseBtn");
  if (addExpenseButton) {
    addExpenseButton.addEventListener("click", (event) => {
      event.preventDefault();
      event.stopPropagation();
      openAddExpense();
    });
  }
  $("profileBtn").addEventListener("click", () => switchView("settings"));
  $("quickReportBtn").addEventListener("click", () => switchView("reports"));
  $("expenseForm").addEventListener("submit", addExpense);
  const refreshAddButton = () => {
    const button = $("addExpenseSubmit");
    if (!button) return;
    const amount = Number($("amount").value);
    const date = $("date").value;
    const ready = Number.isFinite(amount) && amount > 0 && !!date && !!selectedCategory;
    button.disabled = !ready;
    button.setAttribute("aria-disabled", ready ? "false" : "true");
  };
  ["amount", "date", "description"].forEach(id => {
    const field = $(id);
    if (field) field.addEventListener("input", refreshAddButton);
    if (field) field.addEventListener("change", refreshAddButton);
  });
  window.AuraRefreshAddButton = refreshAddButton;
  $("editExpenseForm").addEventListener("submit", updateExpense);
  $("deleteExpenseBtn").addEventListener("click", deleteCurrentExpense);
  $("downloadReportBtn").addEventListener("click", () => generatePDF(reportPeriod));
  $("settingsPdfBtn").addEventListener("click", () => generatePDF("all"));
  $("exportJsonBtn").addEventListener("click", exportJSON);
  $("importJsonBtn").addEventListener("click", () => $("importJsonInput").click());
  $("importJsonInput").addEventListener("change", importJSON);
  $("currencySelect").addEventListener("change", changeCurrency);
  $("budgetInput").addEventListener("change", changeBudget);
  $("themeToggle").addEventListener("click", toggleTheme);
  $("historySearchBtn").addEventListener("click", () => $("historySearchWrap").classList.toggle("hidden"));
  $("historySearch").addEventListener("input", renderHistory);
  $("clearFilterBtn").addEventListener("click", () => { activeFilter = "all"; renderDashboard(); });
  $$("[data-close-modal]").forEach(el => el.addEventListener("click", () => closeModal(el.dataset.closeModal)));
  $("logoutBtn").addEventListener("click", logout);
  $$(".report-tab").forEach(btn => btn.addEventListener("click", () => {
    reportPeriod = btn.dataset.reportPeriod;
    $$(".report-tab").forEach(b => b.classList.toggle("active", b === btn));
    renderReports();
  }));
}

window.AuraOpenAddExpense = function () {
  openAddExpense();
};

function openAddExpense() {
  const form = $("expenseForm");
  if (form) form.reset();

  selectedCategory = "";
  window.AuraSelectedCategory = "";
  $("formError").textContent = "";
  $("formError").classList.add("hidden");

  setToday();
  selectCategory("addCategoryGrid", "");
  updateCurrencyLabels();
  openModal("addModal");
  if (typeof window.AuraRefreshAddButton === "function") window.AuraRefreshAddButton();

  // Focus after the sheet animation starts.
  setTimeout(() => $("amount")?.focus(), 80);
}

function switchView(view) {
  const map = { dashboard: "dashboardView", history: "historyView", reports: "reportsView", settings: "settingsView" };
  if (!Object.prototype.hasOwnProperty.call(map, view)) return;
  activeView = view;
  Object.entries(map).forEach(([key, id]) => {
    const section = $(id);
    if (section) section.classList.toggle("hidden", key !== view);
  });
  $$(".nav-item[data-view]").forEach(btn => btn.classList.toggle("active", btn.dataset.view === view));
  if (view === "dashboard") renderDashboard();
  else if (view === "history") renderHistory();
  else if (view === "reports") renderReports();
}

function renderApp() {
  renderDashboard();
  renderHistory();
  renderReports();
}

function populateCurrencySelects() {
  const options = Object.entries(CURRENCIES).map(([code, c]) =>
    `<option value="${code}">${code} — ${c.name} (${c.symbol})</option>`).join("");
  $("signupCurrency").innerHTML = options;
  $("currencySelect").innerHTML = options;
}

function changeCurrency() {
  settings.currency = $("currencySelect").value;
  saveSettings();
  const user = users.find(u => u.email === currentUser.email);
  if (user) user.currency = settings.currency;
  saveUsers();
  updateCurrencyLabels();
  renderApp();
  showToast(`Currency changed to ${settings.currency}`);
}

function updateCurrencyLabels() {
  const s = CURRENCIES[settings.currency].symbol;
  $("amountCurrency").textContent = s;
  $("editAmountCurrency").textContent = s;
}

function changeBudget() {
  const value = Math.max(0, Number($("budgetInput").value) || 0);
  settings.budget = value;
  $("budgetInput").value = value;
  saveSettings();
  renderDashboard();
}

function toggleTheme() {
  settings.theme = settings.theme === "dark" ? "light" : "dark";
  saveSettings();
  applyTheme();
}

function applyTheme() {
  document.body.classList.toggle("light-theme", settings.theme === "light");
  $("themeToggle")?.classList.toggle("on", settings.theme === "light");
}

function setToday() {
  const today = new Date().toISOString().slice(0, 10);
  $("date").value = today;
}

function buildCategoryGrids() {
  ["addCategoryGrid", "editCategoryGrid"].forEach(id => {
    const grid = $(id);
    if (!grid) return;

    grid.innerHTML = Object.entries(CATEGORIES).map(([key, c]) =>
      `<button type="button" class="category-option" data-category="${key}" aria-pressed="false">
        <span>${c.emoji}</span><small>${c.label}</small>
      </button>`
    ).join("");

    // One delegated handler on the grid is more reliable on mobile/touch
    // than attaching handlers to elements that are repeatedly rebuilt.
    if (grid.dataset.categoryBound !== "true") {
      grid.dataset.categoryBound = "true";
      grid.addEventListener("click", (event) => {
        const btn = event.target.closest(".category-option");
        if (!btn || !grid.contains(btn)) return;
        event.preventDefault();
        event.stopPropagation();

        const category = btn.dataset.category;
        if (!Object.prototype.hasOwnProperty.call(CATEGORIES, category)) return;

        if (id === "addCategoryGrid") {
          selectedCategory = category;
          window.AuraSelectedCategory = category;
          if (typeof window.AuraRefreshAddButton === "function") window.AuraRefreshAddButton();
        } else {
          editSelectedCategory = category;
        }

        grid.querySelectorAll(".category-option").forEach(option => {
          const selected = option.dataset.category === category;
          option.classList.toggle("selected", selected);
          option.setAttribute("aria-pressed", selected ? "true" : "false");
        });

        const error = id === "addCategoryGrid" ? $("formError") : $("editFormError");
        if (error) error.textContent = "";
      });
    }
  });

  selectCategory("addCategoryGrid", selectedCategory);
  selectCategory("editCategoryGrid", editSelectedCategory);
}

function selectCategory(gridId, category) {
  $(`#${gridId}`).querySelectorAll(".category-option").forEach(b =>
    b.classList.toggle("selected", b.dataset.category === category));
}

function addExpense(e) {
  e.preventDefault();

  const amount = Number($("amount").value);
  const date = $("date").value;
  const description = $("description").value.trim();
  const category = selectedCategory || window.AuraSelectedCategory || "";
  selectedCategory = category;

  if (!Number.isFinite(amount) || amount <= 0 || !date) {
    return setFormError("formError", "Enter a valid amount and date.");
  }

  if (!category || !Object.prototype.hasOwnProperty.call(CATEGORIES, category)) {
    return setFormError("formError", "Please select a category.");
  }

  if (!currentUser) {
    return setFormError("formError", "Please log in before adding an expense.");
  }

  const expense = {
    id: makeId(),
    amount,
    category,
    date,
    description,
    createdAt: new Date().toISOString()
  };

  expenses.unshift(expense);

  try {
    saveExpenses();

    // Confirm that the record actually reached this account's storage key.
    const stored = JSON.parse(
      localStorage.getItem(STORAGE.expensesPrefix + userKey()) || "[]"
    );
    if (!stored.some(item => item && item.id === expense.id)) {
      throw new Error("Expense was not persisted.");
    }
  } catch (error) {
    // Roll back the in-memory change if browser storage is unavailable/full.
    expenses = expenses.filter(item => item.id !== expense.id);
    console.error("AuraSpend: failed to persist expense", error);
    return setFormError(
      "formError",
      "Could not save this expense. Browser storage may be unavailable or full."
    );
  }

  $("expenseForm").reset();
  setToday();
  selectedCategory = "";
  selectCategory("addCategoryGrid", "");
  closeModal("addModal");
  renderApp();
  showToast("Expense added successfully");
}

function updateExpense(e) {
  e.preventDefault();
  const id = $("editId").value;
  const item = expenses.find(x => x.id === id);
  if (!item) return;
  const amount = Number($("editAmount").value);
  if (!amount || amount <= 0 || !$("editDate").value) return setFormError("editFormError", "Enter a valid amount and date.");
  Object.assign(item, {
    amount,
    category: editSelectedCategory,
    date: $("editDate").value,
    description: $("editDescription").value.trim()
  });
  saveExpenses();
  closeModal("editModal");
  renderApp();
  showToast("Expense updated");
}

function deleteCurrentExpense() {
  const id = $("editId").value;
  const item = expenses.find(x => x.id === id);
  if (!item) return;
  if (!confirm("Delete this transaction?")) return;
  expenses = expenses.filter(x => x.id !== id);
  saveExpenses();
  closeModal("editModal");
  renderApp();
  showToast("Transaction deleted");
}

function openEdit(id) {
  const item = expenses.find(x => x.id === id);
  if (!item) return;
  $("editId").value = item.id;
  $("editAmount").value = item.amount;
  $("editDate").value = item.date;
  $("editDescription").value = item.description || "";
  editSelectedCategory = item.category;
  selectCategory("editCategoryGrid", editSelectedCategory);
  openModal("editModal");
}

function renderDashboard() {
  if (!currentUser) return;
  const monthItems = expenses.filter(isCurrentMonth);
  const total = monthItems.reduce((sum, e) => sum + Number(e.amount), 0);
  const budget = Number(settings.budget) || 0;
  const percent = budget ? Math.min(100, Math.round(total / budget * 100)) : 0;
  const days = new Date().getDate();
  const average = days ? total / days : 0;

  $("monthlyTotal").textContent = money(total);
  $("monthlyCompare").textContent = budget ? `${money(Math.max(0, budget - total))} remaining` : "No budget set";
  $("monthTransactions").textContent = monthItems.length;
  $("dailyAverage").textContent = money(average, 0);
  $("budgetPercent").textContent = `${percent}%`;
  $("ringProgress").style.strokeDashoffset = 264 - (264 * percent / 100);
  $("topCategory").textContent = topCategory(monthItems) || "—";

  const filterItems = activeFilter === "all" ? monthItems : monthItems.filter(e => e.category === activeFilter);
  const grouped = groupByCategory(filterItems);
  const max = Math.max(...Object.values(grouped), 1);
  $("categoryFilters").innerHTML = `<button class="chip ${activeFilter === "all" ? "active" : ""}" data-filter="all">All</button>` +
    Object.entries(CATEGORIES).map(([key, c]) =>
      `<button class="chip ${activeFilter === key ? "active" : ""}" data-filter="${key}">${c.emoji} ${c.label}</button>`).join("");
  $$(".chip").forEach(btn => btn.addEventListener("click", () => {
    activeFilter = btn.dataset.filter;
    renderDashboard();
  }));
  $("clearFilterBtn").classList.toggle("hidden", activeFilter === "all");

  const entries = Object.entries(grouped).sort((a,b) => b[1] - a[1]);
  $("categoryBreakdown").innerHTML = entries.map(([key, value]) => `
    <div class="breakdown-item">
      <div class="breakdown-icon">${CATEGORIES[key].emoji}</div>
      <div class="breakdown-main"><div><strong>${CATEGORIES[key].label}</strong><span>${money(value)}</span></div>
      <div class="progress"><span style="width:${Math.round(value / max * 100)}%"></span></div></div>
    </div>`).join("");
  $("emptyDashboard").classList.toggle("hidden", entries.length > 0);
}

function renderHistory() {
  const query = ($("historySearch")?.value || "").trim().toLowerCase();
  const filtered = expenses.filter(e => {
    const matches = !query || `${e.description} ${CATEGORIES[e.category]?.label}`.toLowerCase().includes(query);
    return matches;
  }).sort((a,b) => b.date.localeCompare(a.date));
  const groups = {};
  filtered.forEach(e => (groups[e.date] ||= []).push(e));
  $("historyList").innerHTML = Object.entries(groups).map(([date, items]) => `
    <div class="history-group">
      <div class="date-heading"><span>${formatDateHeading(date)}</span><strong>${money(items.reduce((s,e)=>s+Number(e.amount),0))}</strong></div>
      ${items.map(e => `<button class="history-card" type="button" data-edit-id="${e.id}">
        <div class="history-icon">${CATEGORIES[e.category]?.emoji || "✦"}</div>
        <div class="history-main"><strong>${escapeHtml(e.description || CATEGORIES[e.category]?.label || "Expense")}</strong><span>${CATEGORIES[e.category]?.label || e.category}</span></div>
        <strong class="history-amount">-${money(e.amount)}</strong>
      </button>`).join("")}
    </div>`).join("");
  $$("[data-edit-id]").forEach(btn => btn.addEventListener("click", () => openEdit(btn.dataset.editId)));
  $("emptyHistory").classList.toggle("hidden", filtered.length > 0);
}

function renderReports() {
  const items = getReportItems(reportPeriod);
  const total = items.reduce((s,e) => s + Number(e.amount), 0);
  $("reportPeriodLabel").textContent = reportLabel(reportPeriod);
  $("reportTotal").textContent = money(total);
  $("reportTransactionCount").textContent = `${items.length} transaction${items.length === 1 ? "" : "s"}`;

  const grouped = groupByCategory(items);
  const max = Math.max(...Object.values(grouped), 1);
  $("reportCategories").innerHTML = Object.entries(grouped).sort((a,b)=>b[1]-a[1]).map(([key,value]) => `
    <div class="breakdown-item">
      <div class="breakdown-icon">${CATEGORIES[key].emoji}</div>
      <div class="breakdown-main"><div><strong>${CATEGORIES[key].label}</strong><span>${money(value)}</span></div>
      <div class="progress"><span style="width:${Math.round(value/max*100)}%"></span></div></div>
    </div>`).join("") || `<div class="empty-inline">No transactions in this period.</div>`;

  $("reportTransactions").innerHTML = items.slice().sort((a,b)=>b.date.localeCompare(a.date)).map(e => `
    <div class="report-row"><span>${formatDateShort(e.date)} · ${CATEGORIES[e.category]?.label}</span><strong>${money(e.amount)}</strong></div>
  `).join("") || `<div class="empty-inline">Nothing to report.</div>`;
}

function getReportItems(period) {
  const now = new Date();
  if (period === "all") return [...expenses];
  return expenses.filter(e => {
    const d = parseDate(e.date);
    if (period === "weekly") {
      const start = startOfWeek(now);
      return d >= start && d <= now;
    }
    if (period === "monthly") return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth();
    return d.getFullYear() === now.getFullYear();
  });
}

function reportLabel(period) {
  return ({ weekly: "This week", monthly: "This month", annual: "This year", all: "All time" })[period];
}

function generatePDF(period) {
  // Generate the report locally without depending on an external PDF library/CDN.
  // This keeps PDF export working even when the device is offline.
  const items = getReportItems(period).slice().sort((a, b) => b.date.localeCompare(a.date));
  const total = items.reduce((sum, e) => sum + Number(e.amount || 0), 0);
  const currency = CURRENCIES[settings.currency] || CURRENCIES.USD;
  const title = `AuraSpend ${reportLabel(period)} Report`;
  const account = currentUser ? `${currentUser.name} (${currentUser.email})` : 'Local account';

  const ascii = (value) => String(value ?? '')
    .replace(/[₹€£¥]/g, settings.currency === 'INR' ? 'Rs.' : currency.symbol)
    .replace(/[^\x20-\x7E]/g, '')
    .replace(/[\\]/g, '\\\\')
    .replace(/\(/g, '\\(')
    .replace(/\)/g, '\\)');

  const lines = [
    'AuraSpend',
    title,
    `Account: ${account}`,
    `Generated: ${new Date().toLocaleString()}`,
    `Total: ${currency.symbol}${total.toFixed(2)}`,
    `Transactions: ${items.length}`,
    '',
    'Date                 Category              Description                         Amount',
    '----------------------------------------------------------------------------------------'
  ];

  items.forEach(e => {
    const date = formatDateShort(e.date).padEnd(20).slice(0, 20);
    const category = (CATEGORIES[e.category]?.label || e.category || 'Other').padEnd(22).slice(0, 22);
    const description = (e.description || '—').replace(/\s+/g, ' ').padEnd(36).slice(0, 36);
    const amount = `${currency.symbol}${Number(e.amount || 0).toFixed(2)}`;
    lines.push(`${date}${category}${description}${amount}`);
  });

  // Minimal valid PDF writer using built-in Helvetica. Non-ASCII characters are
  // stripped so the report remains valid without embedding a font file.
  const pageLines = [];
  for (let i = 0; i < lines.length; i += 45) pageLines.push(lines.slice(i, i + 45));
  if (!pageLines.length) pageLines.push([]);

  const objects = [];
  const addObject = body => { objects.push(body); return objects.length; };
  const pagesId = addObject('');
  const fontId = addObject('<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>');
  const pageIds = [];

  pageLines.forEach(page => {
    let stream = 'BT\n/F1 9 Tf\n42 800 Td\n12 TL\n';
    page.forEach((line, index) => {
      const safe = ascii(line);
      if (index === 0) stream += '/F1 18 Tf\n0 0 Td\n';
      else if (index === 1) stream += '/F1 12 Tf\n0 -22 Td\n';
      else stream += '/F1 9 Tf\n0 -14 Td\n';
      stream += `(${safe}) Tj\n`;
    });
    stream += 'ET';
    const streamId = addObject(`<< /Length ${stream.length} >>\nstream\n${stream}\nendstream`);
    const pageId = addObject(`<< /Type /Page /Parent ${pagesId} 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 ${fontId} 0 R >> >> /Contents ${streamId} 0 R >>`);
    pageIds.push(pageId);
  });

  objects[pagesId - 1] = `<< /Type /Pages /Kids [${pageIds.map(id => `${id} 0 R`).join(' ')}] /Count ${pageIds.length} >>`;
  const catalogId = addObject(`<< /Type /Catalog /Pages ${pagesId} 0 R >>`);

  let pdf = '%PDF-1.4\n%\xFF\xFF\xFF\xFF\n';
  const offsets = [0];
  objects.forEach((body, index) => {
    offsets[index + 1] = pdf.length;
    pdf += `${index + 1} 0 obj\n${body}\nendobj\n`;
  });
  const xref = pdf.length;
  pdf += `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`;
  for (let i = 1; i <= objects.length; i++) pdf += `${String(offsets[i]).padStart(10, '0')} 00000 n \n`;
  pdf += `trailer\n<< /Size ${objects.length + 1} /Root ${catalogId} 0 R >>\nstartxref\n${xref}\n%%EOF`;

  const blob = new Blob([pdf], { type: 'application/pdf' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `auraspend-${period === 'all' ? 'all-time' : period}-report-${new Date().toISOString().slice(0, 10)}.pdf`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
  showToast('PDF generated');
}

function exportJSON() {
  const payload = { app: "AuraSpend", version: 2, exportedAt: new Date().toISOString(), expenses, settings };
  downloadBlob(JSON.stringify(payload, null, 2), `auraspend-backup-${new Date().toISOString().slice(0,10)}.json`, "application/json");
  showToast("Backup exported");
}

function importJSON(e) {
  const file = e.target.files?.[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = () => {
    try {
      const payload = JSON.parse(reader.result);
      if (!Array.isArray(payload.expenses)) throw new Error();
      expenses = payload.expenses.filter(x => x && x.amount > 0 && x.date);
      if (payload.settings) settings = { ...settings, ...payload.settings };
      saveExpenses(); saveSettings(); enterApp();
      showToast("Backup imported");
    } catch { showToast("Invalid AuraSpend backup file"); }
    e.target.value = "";
  };
  reader.readAsText(file);
}

function downloadBlob(content, filename, type) {
  const blob = new Blob([content], { type });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url; a.download = filename; a.click();
  URL.revokeObjectURL(url);
}

function groupByCategory(items) {
  const result = {};
  items.forEach(e => result[e.category] = (result[e.category] || 0) + Number(e.amount));
  return result;
}

function topCategory(items) {
  const groups = groupByCategory(items);
  const top = Object.entries(groups).sort((a,b)=>b[1]-a[1])[0];
  return top ? CATEGORIES[top[0]]?.label : null;
}

function isCurrentMonth(e) {
  const d = parseDate(e.date), now = new Date();
  return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth();
}

function parseDate(str) {
  const [y,m,d] = str.split("-").map(Number);
  return new Date(y, m - 1, d);
}

function startOfWeek(date) {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const day = d.getDay();
  d.setDate(d.getDate() - day);
  d.setHours(0,0,0,0);
  return d;
}

function money(value, decimals = 2) {
  const c = CURRENCIES[settings.currency] || CURRENCIES.USD;
  return `${c.symbol}${Number(value || 0).toLocaleString(undefined, { minimumFractionDigits: decimals, maximumFractionDigits: decimals })}`;
}

function formatDateHeading(date) {
  return parseDate(date).toLocaleDateString(undefined, { weekday: "long", month: "short", day: "numeric" });
}

function formatDateShort(date) {
  return parseDate(date).toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
}

function makeId() {
  return crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function openModal(id) {
  $(id).classList.remove("hidden");
  document.body.classList.add("modal-open");
}

function closeModal(id) {
  $(id).classList.add("hidden");
  document.body.classList.remove("modal-open");
  setFormError(id === "addModal" ? "formError" : "editFormError", "");
}

function setFormError(id, message) {
  $(id).textContent = message;
  $(id).classList.toggle("hidden", !message);
}

function showToast(message) {
  const toast = $("toast");
  toast.textContent = message;
  toast.classList.add("show");
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => toast.classList.remove("show"), 2600);
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, ch => ({ "&":"&amp;", "<":"&lt;", ">":"&gt;", '"':"&quot;", "'":"&#039;" }[ch]));
}
