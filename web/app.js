(() => {
  "use strict";

  const el = {
    loginScreen: document.getElementById("login-screen"),
    appScreen: document.getElementById("app-screen"),
    loginUsername: document.getElementById("login-username"),
    loginPassword: document.getElementById("login-password"),
    loginBtn: document.getElementById("login-btn"),
    logoutBtn: document.getElementById("logout-btn"),
    usersBtn: document.getElementById("users-btn"),
    who: document.getElementById("who"),
    searchInput: document.getElementById("search-input"),
    regionFilter: document.getElementById("region-filter"),
    districtFilter: document.getElementById("district-filter"),
    mahallaFilter: document.getElementById("mahalla-filter"),
    treeFilter: document.getElementById("tree-filter"),
    yearFilter: document.getElementById("year-filter"),
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
    el.usersBtn.hidden = state.session.role !== "admin";
  }

  function groupByHousehold(rows) {
    const map = new Map();
    for (const r of rows) {
      const key = `${r.mahallaId}_${r.fio}_${r.phone}`;
      if (!map.has(key)) map.set(key, []);
      map.get(key).push(r);
    }
    return [...map.values()];
  }

  function renderRows() {
    el.countLabel.textContent = `${state.rows.length} ta yozuv ko'rsatilmoqda`;
    el.recordsBody.innerHTML = "";
    const canEdit = state.session?.role === "admin";
    const groups = groupByHousehold(state.rows);
    for (const group of groups) {
      const span = group.length;
      group.forEach((r, idx) => {
        const tr = document.createElement("tr");
        const householdCells = idx === 0 ? `
          <td rowspan="${span}">${escapeHtml(r.region)}</td>
          <td rowspan="${span}">${escapeHtml(r.district)}</td>
          <td rowspan="${span}">${escapeHtml(r.mahalla)}</td>
          <td rowspan="${span}">${escapeHtml(r.fio)}</td>
          <td rowspan="${span}">${escapeHtml(r.phone)}</td>
          <td rowspan="${span}">${escapeHtml(r.area)}</td>
        ` : "";
        tr.innerHTML = `
          ${householdCells}
          <td>${escapeHtml(r.tree)}</td>
          <td>${escapeHtml(r.variety)}</td>
          <td>${escapeHtml(r.count)}</td>
          <td>${escapeHtml(r.submittedBy)}</td>
          <td class="row-actions">
            ${canEdit ? `<button data-edit="${r.id}">✎</button><button data-del="${r.id}" class="del">🗑</button>` : ""}
          </td>
        `;
        el.recordsBody.appendChild(tr);
      });
    }
    el.recordsBody.querySelectorAll("[data-edit]").forEach((btn) =>
      btn.addEventListener("click", () => openEditModal(btn.dataset.edit))
    );
    el.recordsBody.querySelectorAll("[data-del]").forEach((btn) =>
      btn.addEventListener("click", () => confirmDelete(btn.dataset.del))
    );
  }

  function fillSelect(select, placeholder, values) {
    const current = select.value;
    select.innerHTML = `<option value="">${placeholder}</option>` +
      values.map((v) => `<option value="${escapeHtml(v)}">${escapeHtml(v)}</option>`).join("");
    if (values.includes(current)) select.value = current;
  }

  async function loadFacets() {
    try {
      const params = new URLSearchParams();
      if (el.regionFilter.value) params.set("region", el.regionFilter.value);
      if (el.districtFilter.value) params.set("district", el.districtFilter.value);
      if (el.mahallaFilter.value) params.set("mahalla", el.mahallaFilter.value);
      const res = await api(`/api/facets?${params.toString()}`);
      const data = await res.json();
      fillSelect(el.mahallaFilter, "Barcha MFY", data.mahallas || []);
      fillSelect(el.treeFilter, "Barcha ko'chat turlari", data.trees || []);
      fillSelect(el.yearFilter, "Barcha yillar", data.years || []);
      if (state.session?.role === "admin") {
        el.regionFilter.hidden = false;
        el.districtFilter.hidden = false;
        fillSelect(el.regionFilter, "Barcha viloyatlar", data.regions || []);
        fillSelect(el.districtFilter, "Barcha tumanlar", data.districts || []);
      }
    } catch (err) {
      showToast("error", err.message);
    }
  }

  function currentFilterParams() {
    const params = new URLSearchParams();
    if (el.searchInput.value.trim()) params.set("q", el.searchInput.value.trim());
    if (el.treeFilter.value) params.set("tree", el.treeFilter.value);
    if (el.regionFilter.value) params.set("region", el.regionFilter.value);
    if (el.districtFilter.value) params.set("district", el.districtFilter.value);
    if (el.mahallaFilter.value) params.set("mahalla", el.mahallaFilter.value);
    if (el.yearFilter.value) params.set("planting", el.yearFilter.value);
    return params;
  }

  async function loadRecords() {
    try {
      const res = await api(`/api/records?${currentFilterParams().toString()}`);
      const data = await res.json();
      state.rows = data.rows;
      renderRows();
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

  async function openUsersModal() {
    el.modalRoot.innerHTML = `
      <div class="modal-backdrop">
        <div class="modal-card modal-card-wide">
          <h2>Login va parollarni boshqarish</h2>
          <input id="users-search" type="text" placeholder="Login, viloyat yoki tuman bo'yicha qidirish..." />
          <div class="users-table-wrap">
            <table id="users-table">
              <thead>
                <tr><th>Login</th><th>Viloyat</th><th>Tuman</th><th>Yangi parol</th><th></th></tr>
              </thead>
              <tbody id="users-body"></tbody>
            </table>
          </div>
          <div class="modal-actions">
            <button class="btn-secondary" id="users-close">Yopish</button>
          </div>
        </div>
      </div>
    `;
    document.getElementById("users-close").addEventListener("click", closeModal);

    let allUsers = [];
    try {
      const res = await api("/api/admin/users");
      allUsers = await res.json();
    } catch (err) {
      showToast("error", err.message);
      return;
    }

    function renderUsers(users) {
      const body = document.getElementById("users-body");
      if (!body) return;
      body.innerHTML = users.map((u) => `
        <tr data-username="${escapeHtml(u.username)}">
          <td>${escapeHtml(u.username)}</td>
          <td>${escapeHtml(u.region || "")}</td>
          <td>${escapeHtml(u.district || "")}</td>
          <td><input type="password" class="users-pw-input" placeholder="kamida 6 belgi" /></td>
          <td><button class="btn-primary users-save-btn">Saqlash</button></td>
        </tr>
      `).join("");
      body.querySelectorAll(".users-save-btn").forEach((btn) => {
        btn.addEventListener("click", async () => {
          const tr = btn.closest("tr");
          const username = tr.dataset.username;
          const pwInput = tr.querySelector(".users-pw-input");
          const password = pwInput.value;
          if (password.length < 6) {
            showToast("error", "Parol kamida 6 belgidan iborat bo'lishi kerak.");
            return;
          }
          try {
            await api(`/api/admin/users/${encodeURIComponent(username)}/password`, {
              method: "PUT",
              body: JSON.stringify({ password }),
            });
            showToast("success", `${username} paroli yangilandi.`);
            pwInput.value = "";
          } catch (err) {
            showToast("error", err.message);
          }
        });
      });
    }

    renderUsers(allUsers);

    document.getElementById("users-search").addEventListener("input", (e) => {
      const q = e.target.value.trim().toLowerCase();
      const filtered = !q ? allUsers : allUsers.filter((u) =>
        [u.username, u.region, u.district].some((v) => (v || "").toLowerCase().includes(q))
      );
      renderUsers(filtered);
    });
  }

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
      const res = await api(`/api/export.xlsx?${currentFilterParams().toString()}`);
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
    loadFacets();
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
  el.usersBtn.addEventListener("click", openUsersModal);
  el.searchInput.addEventListener("input", () => {
    clearTimeout(state.searchTimer);
    state.searchTimer = setTimeout(loadRecords, 300);
  });
  el.treeFilter.addEventListener("change", loadRecords);
  el.yearFilter.addEventListener("change", loadRecords);
  el.mahallaFilter.addEventListener("change", () => { loadFacets(); loadRecords(); });
  el.regionFilter.addEventListener("change", () => {
    el.districtFilter.value = "";
    el.mahallaFilter.value = "";
    loadFacets();
    loadRecords();
  });
  el.districtFilter.addEventListener("change", () => {
    el.mahallaFilter.value = "";
    loadFacets();
    loadRecords();
  });
  el.exportBtn.addEventListener("click", doExport);

  if (state.token && state.session) showApp();
})();
