// Reference demo shell with server-backed navigation; no simulated business data.
const uiSidebar = document.querySelector<HTMLElement>("#sidebar");
const uiMenu = document.querySelector<HTMLButtonElement>("[data-menu]");
const uiMain = document.querySelector<HTMLElement>("main");
const currentPage = uiMain?.dataset.currentPage;
const activeLink = document.querySelector<HTMLAnchorElement>(`.navitem[data-page="${currentPage}"]`);
activeLink?.classList.add("active");
activeLink?.setAttribute("aria-current", "page");
const uiLabel = document.querySelector<HTMLElement>("[data-page-label]");
if (uiLabel) uiLabel.textContent = activeLink?.textContent?.trim() ?? uiMain?.querySelector("h1")?.textContent ?? "Không gian làm việc";
const clockElement = document.querySelector<HTMLElement>("[data-clock]");
if (clockElement) {
  const now = new Date();
  clockElement.textContent = `${now.toLocaleDateString("vi-VN", {day:"2-digit",month:"2-digit",year:"numeric"})} · ${now.toLocaleTimeString("vi-VN", {hour:"2-digit", minute:"2-digit"})}`;
  clockElement.setAttribute("datetime", now.toISOString());
}
const closeUiMenu = (): void => {
  uiSidebar?.classList.remove("open"); document.body.classList.remove("navigation-open");
  uiMenu?.setAttribute("aria-expanded", "false");
};
uiMenu?.addEventListener("click", () => {
  const open = uiSidebar?.classList.toggle("open") ?? false;
  document.body.classList.toggle("navigation-open", open);
  uiMenu.setAttribute("aria-expanded", String(open));
});
document.querySelector("[data-close-menu]")?.addEventListener("click", closeUiMenu);
window.matchMedia("(min-width: 601px)").addEventListener("change", closeUiMenu);
document.addEventListener("keydown", (event) => {
  if (event.key === "Escape" && uiSidebar?.classList.contains("open")) { closeUiMenu(); uiMenu?.focus(); }
});
const showFuture = (title: string): void => {
  document.querySelector<HTMLDialogElement>("#quick-navigation")?.close();
  const dialog = document.querySelector<HTMLDialogElement>("#future-feature");
  const heading = dialog?.querySelector("h2");
  if (heading) heading.textContent = title;
  dialog?.showModal();
};
document.addEventListener("click", (event) => {
  if (!(event.target instanceof Element)) return;
  const future = event.target.closest<HTMLElement>("[data-future]");
  if (future) { event.preventDefault(); showFuture(future.dataset.future ?? "Tính năng đang phát triển"); return; }
  const opener = event.target.closest<HTMLElement>("[data-open-dialog]");
  if (opener) document.querySelector<HTMLDialogElement>(`#${opener.dataset.openDialog}`)?.showModal();
  if (event.target.closest("[data-close-dialog]")) event.target.closest("dialog")?.close();
});
document.querySelectorAll<HTMLDialogElement>('dialog[data-open="true"]').forEach(dialog => dialog.showModal());
const quickDialog = document.querySelector<HTMLDialogElement>("#quick-navigation");
const quickSearch = document.querySelector<HTMLInputElement>("[data-quick-search]");
const quickResults = document.querySelector<HTMLElement>("[data-quick-results]");
const normalizeUiText = (value: string): string => value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/gi,"d").toLowerCase();
const populateQuick = (): void => {
  quickResults?.replaceChildren();
  document.querySelectorAll<HTMLElement>(".sidebar .navgroup .navitem").forEach(item => {
    if (!normalizeUiText(item.textContent ?? "").includes(normalizeUiText(quickSearch?.value ?? ""))) return;
    const copy = item.cloneNode(true) as HTMLElement;
    const label = copy.querySelector("span");
    const symbol = copy.querySelector("svg");
    copy.className = "command-item";
    copy.removeAttribute("aria-current");
    if (symbol) { const holder = document.createElement("span"); holder.className = "command-icon"; symbol.before(holder); holder.append(symbol); }
    const hint = document.createElement("small"); hint.textContent = item.hasAttribute("aria-current") ? "Màn hình hiện tại" : "Đi tới chức năng"; label?.append(hint);
    copy.insertAdjacentHTML("beforeend", '<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><path d="m9 5 7 7-7 7"/></svg>');
    quickResults?.append(copy);
  });
  if (quickResults && !quickResults.childElementCount) { const empty = document.createElement("p"); empty.className = "command-empty"; empty.textContent = "Không tìm thấy chức năng phù hợp."; quickResults.append(empty); }
};
const openQuick = (): void => { if (quickSearch) quickSearch.value = ""; populateQuick(); quickDialog?.showModal(); quickSearch?.focus(); };
document.querySelector("[data-quick-nav]")?.addEventListener("click", openQuick);
quickSearch?.addEventListener("input", populateQuick);
quickSearch?.addEventListener("keydown", event => { if (event.key === "ArrowDown") { event.preventDefault(); quickResults?.querySelector<HTMLElement>(".command-item")?.focus(); } });
quickResults?.addEventListener("keydown", event => {
  if (event.key !== "ArrowDown" && event.key !== "ArrowUp") return;
  event.preventDefault(); const nodes = Array.from(quickResults.querySelectorAll<HTMLElement>(".command-item"));
  const current = nodes.indexOf(document.activeElement as HTMLElement); const next = current + (event.key === "ArrowDown" ? 1 : -1);
  if (next < 0) quickSearch?.focus(); else nodes[Math.min(next,nodes.length - 1)]?.focus();
});
document.addEventListener("keydown", (event) => {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k" && quickDialog) { event.preventDefault(); openQuick(); }
  if (event.key === "F2" && document.querySelector("#scan")) { event.preventDefault(); document.querySelector<HTMLInputElement>("#scan")?.focus(); }
});
document.querySelectorAll<HTMLTableElement>(".table-card table").forEach(table => {
  const labels = Array.from(table.querySelectorAll("thead th"), cell => cell.textContent?.trim() ?? "");
  table.querySelectorAll("tbody tr").forEach(row => Array.from(row.children).forEach((cell, index) => {
    if (cell instanceof HTMLElement && !cell.hasAttribute("colspan")) cell.dataset.label = labels[index] ?? "";
  }));
});

// Confirm in an app dialog, retaining the original POST and CSRF fields.
const logoutModal = document.querySelector<HTMLDialogElement>("#logout-confirmation");
let confirmedLogout = false;
let pendingLogout: HTMLFormElement | null = null;
document.querySelectorAll<HTMLFormElement>(".app-logout").forEach((form) => {
  form.addEventListener("submit", (event) => {
    if (confirmedLogout) return;
    event.preventDefault();
    pendingLogout = form;
    logoutModal?.showModal();
  });
});
document.querySelector("[data-cancel-logout]")?.addEventListener("click", () => logoutModal?.close());
document.querySelector("[data-confirm-logout]")?.addEventListener("click", () => {
  if (!pendingLogout) return;
  confirmedLogout = true;
  logoutModal?.close();
  pendingLogout.requestSubmit();
});
logoutModal?.addEventListener("close", () => {
  if (!confirmedLogout) pendingLogout?.querySelector<HTMLButtonElement>("button")?.focus();
});

// Shared by login and account creation; the password value never leaves the input.
document.querySelectorAll<HTMLInputElement>('input[type="password"]').forEach((input) => {
  const wrapper = document.createElement("div");
  wrapper.className = "password-control";
  input.before(wrapper);
  wrapper.append(input);
  const toggle = document.createElement("button");
  toggle.type = "button";
  toggle.className = "password-toggle";
  toggle.setAttribute("aria-label", "Hiện mật khẩu");
  toggle.setAttribute("aria-pressed", "false");
  if (input.id) toggle.setAttribute("aria-controls", input.id);
  toggle.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/><path class="eye-slash" d="m3 3 18 18"/></svg>';
  toggle.addEventListener("click", () => {
    const show = input.type === "password";
    input.type = show ? "text" : "password";
    toggle.setAttribute("aria-label", show ? "Ẩn mật khẩu" : "Hiện mật khẩu");
    toggle.setAttribute("aria-pressed", String(show));
  });
  wrapper.append(toggle);
});
window.addEventListener("pageshow", () => {
  confirmedLogout = false; pendingLogout = null; logoutModal?.close();
  document.querySelectorAll<HTMLButtonElement>(".password-toggle").forEach((toggle) => {
    const input = toggle.parentElement?.querySelector("input");
    if (input) input.type = "password";
    toggle.setAttribute("aria-label", "Hiện mật khẩu");
    toggle.setAttribute("aria-pressed", "false");
  });
});
const filterRows = (): void => {
  const query = normalizeUiText(document.querySelector<HTMLInputElement>('[data-filter-search]')?.value ?? '');
  const selected = document.querySelector<HTMLSelectElement>('[data-filter-select]')?.value ?? '';
  document.querySelectorAll<HTMLElement>('[data-filter-rows] > tr[data-filter-value]').forEach(row => {
    row.hidden = Boolean((selected && row.dataset.filterValue !== selected) || !normalizeUiText(row.textContent ?? '').includes(query));
  });
};
document.querySelector('[data-filter-search]')?.addEventListener('input', filterRows);
document.querySelector('[data-filter-select]')?.addEventListener('change', filterRows);

// Reuse the reference illustrations only for matching products without an image.
function decorateProductImages(scope: ParentNode = document): void {
scope.querySelectorAll<HTMLImageElement>('img[src$="product-placeholder.svg"]').forEach(image => {
  const name = normalizeUiText(image.alt);
  const artwork = name.includes('vinamilk') ? 'milk' : name.includes('cosy') ? 'box'
    : name.includes('aquafina') ? 'water' : name.includes('hao hao') ? 'noodles' : name.includes('simply') ? 'oil' : null;
  if (artwork) image.src = image.src.replace('product-placeholder.svg', `reference/${artwork}.svg`);
});
}
decorateProductImages();

(()=>{
const cashForm = document.querySelector<HTMLFormElement>("#cash-form");
const cashInput = document.querySelector<HTMLInputElement>("#cashReceived");
// Reference tender shortcuts only fill the existing input; the server confirms payment.
if (cashForm && cashInput) {
  const amountDue = BigInt((cashForm.dataset.total ?? "0").split(".")[0]);
  const shortcuts = document.createElement("div");
  shortcuts.className = "quickcash span2";
  const suggestions = new Set([10000n, 50000n, 100000n].map(unit => ((amountDue + unit - 1n) / unit) * unit));
  suggestions.add(200000n);
  suggestions.add(500000n);
  Array.from(suggestions).sort((a, b) => a < b ? -1 : a > b ? 1 : 0).forEach(amount => {
    const button = document.createElement("button");
    button.type = "button"; button.textContent = amount.toLocaleString("vi-VN");
    const unit = document.createElement("span"); unit.className = "currency-unit"; unit.textContent = "VNĐ"; button.append(unit);
    button.addEventListener("click", () => { cashInput.value = amount.toString(); cashInput.dispatchEvent(new Event("input", {bubbles:true})); });
    shortcuts.append(button);
  });
  cashInput.closest(".field")?.after(shortcuts);
}

})();
