(() => {
  "use strict";

  const el = {
    loginScreen: document.getElementById("login-screen"),
    appScreen: document.getElementById("app-screen"),
    loginUsername: document.getElementById("login-username"),
    loginPassword: document.getElementById("login-password"),
    loginBtn: document.getElementById("login-btn"),
    logoutBtn: document.getElementById("logout-btn"),
    who: document.getElementById("who"),
    searchInput: document.getElementById("search-input"),
    treeFilter: document.getElementById("tree-filter"),
    exportBtn: document.getElementById("export-btn"),
    countLabel: document.getElementById("count-label"),
    recordsBody: document.getElementById("records-body"),
    toastArea: document.getElementById("toast-area"),
    modalRoot: document.getElementById("modal-root"),
  };

  function readStoredSession() {
    try {
      return {
        token: sessionStorage.getItem("kochatzor_token") || "",
        session: JSON.parse(sessionStorage.getItem("kochatzor_session") || "null"),
      };
    } catch {
      return { token: "", session: null };
    }
  }
  function saveStoredSession(token, session) {
    try {
      sessionStorage.setItem("kochatzor_token", token);
      sessionStorage.setItem("kochatzor_session", JSON.stringify(session));
    } catch {
      // brauzer xotirasi bloklangan bo'lishi mumkin (masalan maxfiy rejim) —
      // sessiya shu sahifa yangilanmaguncha xotirada saqlanadi, login davom etadi.
    }
  }

  const stored = readStoredSession();
  const state = {
    token: stored.token,
    session: stored.session,
    rows: [],
    searchTimer: null,
  };

  function showToast(type, text) {
    const div = document.createElement("div");
    div.className = `toast ${type}`;
    div.textContent = text;
    el.toastArea.appendChild(div);
    setTimeout(() => div.remove(), 3500);
  }

  async function api(path, options = {}) {
    const res = await fetch(path, {
      ...options,
      headers: {
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        Authorization: `Bearer ${state.token}`,
        ...(options.headers || {}),
      },
    });
    if (res.status === 401) {
      logout();
      throw new Error("Sessiya tugadi, qayta kiring.");
    }
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      throw new Error(data.detail || "Xatolik yuz berdi.");
    }
    return res;
  }

  function escapeHtml(v) {
    return String(v == null ? "" : v).replace(/[&<>"']/g, (c) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
    }[c]));
  }

  function renderWho() {
    if (!state.session) return;
    const scope = state.session.role === "admin" ? "Barcha hududlar" : `${state.session.region} — ${state.session.district}`;
    el.who.textContent = `${state.session.role === "admin" ? "Administrator" : "Xodim"} · ${scope}`;
  }

  function renderRows() {
    el.countLabel.textContent = `${state.rows.length} ta yozuv ko'rsatilmoqda`;
    el.recordsBody.innerHTML = "";
    const canEdit = state.session?.role === "admin";
    for (const r of state.rows) {
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td>${escapeHtml(r.region)}</td>
        <td>${escapeHtml(r.district)}</td>
        <td>${escapeHtml(r.mahalla)}</td>
        <td>${escapeHtml(r.fio)}</td>
        <td>${escapeHtml(r.phone)}</td>
        <td>${escapeHtml(r.area)}</td>
        <td>${escapeHtml(r.tree)}</td>
        <td>${escapeHtml(r.variety)}</td>
        <td>${escapeHtml(r.count)}</td>
        <td>${r.latitude || r.longitude ? `<a href="https://maps.google.com/?q=${r.latitude},${r.longitude}" target="_blank" rel="noopener">🗺️ xarita</a>` : "—"}</td>
        <td>${escapeHtml(r.submittedBy)}</td>
        <td class="row-actions">
          ${canEdit ? `<button data-edit="${r.id}">✎</button><button data-del="${r.id}" class="del">🗑</button>` : ""}
        </td>
      `;
      el.recordsBody.appendChild(tr);
    }
    el.recordsBody.querySelectorAll("[data-edit]").forEach((btn) =>
      btn.addEventListener("click", () => openEditModal(btn.dataset.edit))
    );
    el.recordsBody.querySelectorAll("[data-del]").forEach((btn) =>
      btn.addEventListener("click", () => confirmDelete(btn.dataset.del))
    );
  }

  function updateTreeFilterOptions() {
    const current = el.treeFilter.value;
    const trees = [...new Set(state.rows.map((r) => r.tree).filter(Boolean))].sort();
    el.treeFilter.innerHTML = '<option value="">Barcha ko\'chat turlari</option>' +
      trees.map((t) => `<option value="${escapeHtml(t)}">${escapeHtml(t)}</option>`).join("");
    if (trees.includes(current)) el.treeFilter.value = current;
  }

  async function loadRecords() {
    const params = new URLSearchParams();
    if (el.searchInput.value.trim()) params.set("q", el.searchInput.value.trim());
    if (el.treeFilter.value) params.set("tree", el.treeFilter.value);
    try {
      const res = await api(`/api/records?${params.toString()}`);
      const data = await res.json();
      state.rows = data.rows;
      renderRows();
      updateTreeFilterOptions();
    } catch (err) {
      showToast("error", err.message);
    }
  }

  function openEditModal(id) {
    const row = state.rows.find((r) => r.id === id);
    if (!row) return;
    el.modalRoot.innerHTML = `
      <div class="modal-backdrop">
        <div class="modal-card">
          <h2>Yozuvni tahrirlash</h2>
          <label>FIO</label><input id="edit-fio" value="${escapeHtml(row.fio)}" />
          <label>Telefon</label><input id="edit-phone" value="${escapeHtml(row.phone)}" />
          <label>Maydon (ga)</label><input id="edit-area" type="number" step="0.01" value="${row.area}" />
          <label>Ko'chat turi</label><input id="edit-tree" value="${escapeHtml(row.tree)}" />
          <label>Nav</label><input id="edit-variety" value="${escapeHtml(row.variety)}" />
          <label>Soni</label><input id="edit-count" type="number" value="${row.count}" />
          <label>Kenglik (latitude)</label><input id="edit-lat" type="number" step="0.000001" value="${row.latitude || ""}" />
          <label>Uzunlik (longitude)</label><input id="edit-lng" type="number" step="0.000001" value="${row.longitude || ""}" />
          <div class="modal-actions">
            <button class="btn-secondary" id="edit-cancel">Bekor qilish</button>
            <button class="btn-primary" id="edit-save">Saqlash</button>
          </div>
        </div>
      </div>
    `;
    document.getElementById("edit-cancel").addEventListener("click", closeModal);
    document.getElementById("edit-save").addEventListener("click", async () => {
      try {
        await api(`/api/records/${encodeURIComponent(id)}`, {
          method: "PUT",
          body: JSON.stringify({
            fio: document.getElementById("edit-fio").value,
            phone: document.getElementById("edit-phone").value,
            area: Number(document.getElementById("edit-area").value),
            tree: document.getElementById("edit-tree").value,
            variety: document.getElementById("edit-variety").value,
            count: Number(document.getElementById("edit-count").value),
            latitude: Number(document.getElementById("edit-lat").value) || 0,
            longitude: Number(document.getElementById("edit-lng").value) || 0,
          }),
        });
        closeModal();
        showToast("success", "Saqlandi.");
        loadRecords();
      } catch (err) {
        showToast("error", err.message);
      }
    });
  }
  function closeModal() { el.modalRoot.innerHTML = ""; }

  async function confirmDelete(id) {
    if (!window.confirm("Bu yozuvni o'chirishni tasdiqlaysizmi?")) return;
    try {
      await api(`/api/records/${encodeURIComponent(id)}`, { method: "DELETE" });
      showToast("success", "O'chirildi.");
      loadRecords();
    } catch (err) {
      showToast("error", err.message);
    }
  }

  async function doExport() {
    try {
      const res = await api("/api/export.xlsx");
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "kochatzor.xlsx";
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      showToast("error", err.message);
    }
  }

  function showApp() {
    el.loginScreen.hidden = true;
    el.appScreen.hidden = false;
    renderWho();
    loadRecords();
  }

  function logout() {
    state.token = "";
    state.session = null;
    try {
      sessionStorage.removeItem("kochatzor_token");
      sessionStorage.removeItem("kochatzor_session");
    } catch {
      // xotira bloklangan bo'lsa ham chiqishning o'zi davom etadi
    }
    el.appScreen.hidden = true;
    el.loginScreen.hidden = false;
    el.loginPassword.value = "";
  }

  el.loginBtn.addEventListener("click", async () => {
    const username = el.loginUsername.value.trim();
    const password = el.loginPassword.value.trim();
    if (!username || !password) return;
    try {
      const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      });
      if (!res.ok) throw new Error("Login yoki parol noto'g'ri.");
      const data = await res.json();
      state.token = data.token;
      state.session = data;
      saveStoredSession(state.token, data);
      showApp();
    } catch (err) {
      showToast("error", err.message);
    }
  });
  el.loginPassword.addEventListener("keydown", (e) => { if (e.key === "Enter") el.loginBtn.click(); });
  el.logoutBtn.addEventListener("click", logout);
  el.searchInput.addEventListener("input", () => {
    clearTimeout(state.searchTimer);
    state.searchTimer = setTimeout(loadRecords, 300);
  });
  el.treeFilter.addEventListener("change", loadRecords);
  el.exportBtn.addEventListener("click", doExport);

  if (state.token && state.session) showApp();
})();
