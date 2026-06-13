/* ============================================================
   Hospital Emergency Queue Management System — app.js
   ============================================================ */

const API_BASE = 'http://localhost:8080/api';
let updatePriorityModal = null;
let currentTab = 'queue';
let autoRefreshTimer = null;

// Priority config
const PRIORITY_CONFIG = {
    1: { label: 'Critical',  color: 'badge-1', cardClass: 'priority-1-card', icon: 'bi-heartbeat',            text: '#d32f2f' },
    2: { label: 'Serious',   color: 'badge-2', cardClass: 'priority-2-card', icon: 'bi-exclamation-triangle', text: '#e65100' },
    3: { label: 'Moderate',  color: 'badge-3', cardClass: 'priority-3-card', icon: 'bi-activity',             text: '#f57f17' },
    4: { label: 'Minor',     color: 'badge-4', cardClass: 'priority-4-card', icon: 'bi-bandaid',              text: '#2e7d32' },
    5: { label: 'Normal',    color: 'badge-5', cardClass: 'priority-5-card', icon: 'bi-emoji-smile',          text: '#455a64' },
};

// ============================================================
// INIT
// ============================================================
document.addEventListener('DOMContentLoaded', () => {
    updateClock();
    setInterval(updateClock, 1000);

    updatePriorityModal = new bootstrap.Modal(document.getElementById('updatePriorityModal'));

    document.getElementById('patientForm').addEventListener('submit', handleFormSubmit);
    document.getElementById('patientSymptoms').addEventListener('input', updateTriagePreview);
    updateTriagePreview();

    refreshAll();
    // Auto-refresh queue every 15 seconds
    autoRefreshTimer = setInterval(() => { if (currentTab === 'queue') refreshQueue(); }, 15000);
});

function updateClock() {
    const now = new Date();
    document.getElementById('currentTime').textContent =
        now.toLocaleDateString('en-IN', { weekday:'short', day:'2-digit', month:'short' }) + '  ' +
        now.toLocaleTimeString('en-IN', { hour:'2-digit', minute:'2-digit', second:'2-digit' });
}

// ============================================================
// REFRESH ALL
// ============================================================
async function refreshAll() {
    await Promise.all([loadStats(), refreshQueue()]);
}

// ============================================================
// LOAD STATS
// ============================================================
async function loadStats() {
    try {
        const res = await fetch(`${API_BASE}/patients/stats`);
        const json = await res.json();
        if (!json.success) return;

        const s = json.data;
        animateNumber('statTotal',    s.totalInQueue);
        animateNumber('statCritical', s.criticalCount);
        animateNumber('statSerious',  s.seriousCount);
        animateNumber('statModerate', s.moderateCount);
        animateNumber('statTreated',  s.totalTreated);
        animateNumber('statAllTime',  s.totalPatients);

        // Next patient banner
        if (s.nextPatient) {
            showNextPatientBanner(s.nextPatient);
        } else {
            document.getElementById('nextPatientBanner').classList.add('d-none');
            document.getElementById('emptyQueueBanner').classList.remove('d-none');
        }
    } catch (err) {
        console.error('Stats load error:', err);
    }
}

function animateNumber(id, target) {
    const el = document.getElementById(id);
    const current = parseInt(el.textContent) || 0;
    if (current === target) return;
    const step = target > current ? 1 : -1;
    let val = current;
    const timer = setInterval(() => {
        val += step;
        el.textContent = val;
        if (val === target) clearInterval(timer);
    }, 40);
}

function showNextPatientBanner(patient) {
    document.getElementById('nextPatientBanner').classList.remove('d-none');
    document.getElementById('emptyQueueBanner').classList.add('d-none');
    document.getElementById('nextPatientName').textContent = patient.name;
    document.getElementById('nextPatientInfo').textContent =
        `Age ${patient.age} · ${patient.priorityDescription} (P${patient.priorityLevel}) · ${patient.symptoms}`;
}

// ============================================================
// LOAD QUEUE
// ============================================================
async function refreshQueue() {
    const priority = document.getElementById('priorityFilter').value;
    const search   = document.getElementById('searchInput').value.trim();

    let url;
    if (search) {
        url = `${API_BASE}/patients/search?name=${encodeURIComponent(search)}`;
    } else if (priority) {
        url = `${API_BASE}/patients/filter?priority=${priority}`;
    } else {
        url = `${API_BASE}/patients`;
    }

    try {
        const res  = await fetch(url);
        const json = await res.json();
        if (!json.success) return;

        const patients = json.data || [];
        const waiting  = patients.filter(p => p.status === 'WAITING' || p.status === undefined);
        renderQueue(waiting);
        document.getElementById('queueCountBadge').textContent = `${waiting.length} patient${waiting.length !== 1 ? 's' : ''}`;
    } catch (err) {
        console.error('Queue load error:', err);
        document.getElementById('queueContainer').innerHTML =
            `<div class="empty-state"><i class="bi bi-wifi-off"></i><p>Cannot connect to server.<br>Make sure the Spring Boot backend is running on port 8080.</p></div>`;
    }
}

function renderQueue(patients) {
    const container = document.getElementById('queueContainer');
    if (patients.length === 0) {
        container.innerHTML = `<div class="empty-state"><i class="bi bi-inbox"></i><p>No patients in the queue.</p></div>`;
        return;
    }
    container.innerHTML = patients.map((p, idx) => buildPatientCard(p, idx + 1, false)).join('');
}

// ============================================================
// LOAD HISTORY
// ============================================================
async function loadHistory() {
    try {
        const res  = await fetch(`${API_BASE}/history`);
        const json = await res.json();
        if (!json.success) return;

        const container = document.getElementById('historyContainer');
        const history   = json.data || [];
        if (history.length === 0) {
            container.innerHTML = `<div class="empty-state"><i class="bi bi-clock-history"></i><p>No treatment history yet.</p></div>`;
            return;
        }
        container.innerHTML = history.map((p, idx) => buildPatientCard(p, idx + 1, true)).join('');
    } catch (err) {
        console.error('History load error:', err);
    }
}

// ============================================================
// BUILD PATIENT CARD HTML
// ============================================================
function buildPatientCard(p, pos, isHistory) {
    const cfg = PRIORITY_CONFIG[p.priorityLevel] || PRIORITY_CONFIG[5];
    const posClass = pos === 1 && !isHistory ? 'pos-1' : '';
    const arrivalTime = formatDateTime(p.arrivalTime);
    const treatedTime = p.treatedTime ? formatDateTime(p.treatedTime) : null;

    const actionButtons = isHistory
        ? `<span class="badge bg-success"><i class="bi bi-check-circle me-1"></i>Treated</span>`
        : `
            <button class="btn btn-outline-primary btn-action" onclick="openUpdatePriority(${p.patientId}, '${escapeHtml(p.name)}', ${p.priorityLevel})" title="Update Priority">
                <i class="bi bi-arrow-repeat"></i> Update
            </button>
            <button class="btn btn-outline-danger btn-action" onclick="removePatient(${p.patientId}, '${escapeHtml(p.name)}')" title="Remove">
                <i class="bi bi-trash3"></i>
            </button>
        `;

    return `
    <div class="patient-card ${cfg.cardClass} ${isHistory ? 'treated' : ''}">
        <div class="queue-position ${posClass}">${isHistory ? '<i class="bi bi-check2"></i>' : pos}</div>
        <div class="flex-grow-1 overflow-hidden">
            <div class="d-flex align-items-center gap-2 flex-wrap">
                <span class="patient-name">${escapeHtml(p.name)}</span>
                <span class="priority-badge ${cfg.color}"><i class="bi ${cfg.icon} me-1"></i>P${p.priorityLevel} ${cfg.label}</span>
                ${pos === 1 && !isHistory ? '<span class="badge bg-danger blink-badge">NEXT</span>' : ''}
            </div>
            <div class="patient-meta mt-1">
                <i class="bi bi-person me-1"></i>Age ${p.age}
                ${p.contactNumber ? `<span class="ms-2"><i class="bi bi-telephone me-1"></i>${escapeHtml(p.contactNumber)}</span>` : ''}
                <span class="ms-2"><i class="bi bi-clock me-1"></i>Arrived: ${arrivalTime}</span>
                ${treatedTime ? `<span class="ms-2 text-success"><i class="bi bi-check-circle me-1"></i>Treated: ${treatedTime}</span>` : ''}
                ${p.queuePosition ? `<span class="ms-2 text-primary"><i class="bi bi-list-ol me-1"></i>Queue #${p.queuePosition}</span>` : ''}
            </div>
            <div class="patient-symptoms"><i class="bi bi-clipboard2-pulse me-1"></i>${escapeHtml(p.symptoms)}</div>
            ${p.triageReason ? `<div class="small text-secondary mt-1"><i class="bi bi-shield-plus me-1"></i>${escapeHtml(p.triageReason)}</div>` : ''}
        </div>
        <div class="patient-actions">${actionButtons}</div>
    </div>`;
}

// ============================================================
// FORM SUBMIT — Register Patient
// ============================================================
async function handleFormSubmit(e) {
    e.preventDefault();

    const name     = document.getElementById('patientName').value.trim();
    const age      = parseInt(document.getElementById('patientAge').value);
    const symptoms = document.getElementById('patientSymptoms').value.trim();
    const contact  = document.getElementById('patientContact').value.trim();

    let valid = true;

    // Name
    if (!name || name.length < 2) {
        document.getElementById('patientName').classList.add('is-invalid');
        valid = false;
    } else { document.getElementById('patientName').classList.remove('is-invalid'); }

    // Age
    if (isNaN(age) || age < 0 || age > 150) {
        document.getElementById('patientAge').classList.add('is-invalid');
        valid = false;
    } else { document.getElementById('patientAge').classList.remove('is-invalid'); }

    // Symptoms
    if (!symptoms) {
        document.getElementById('patientSymptoms').classList.add('is-invalid');
        valid = false;
    } else { document.getElementById('patientSymptoms').classList.remove('is-invalid'); }

    if (!valid) return;

    const triage = detectPriorityFromSymptoms(symptoms);
    const payload = { name, age, symptoms, contactNumber: contact || null, priorityLevel: triage.level };

    try {
        const res  = await fetch(`${API_BASE}/patients`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const json = await res.json();

        if (json.success) {
            showAlert('success', `<i class="bi bi-check-circle-fill me-2"></i>${json.message} <span class="text-muted">(Auto-triaged: P${triage.level} ${triage.label})</span>`);
            document.getElementById('patientForm').reset();
            updateTriagePreview();
            await refreshAll();
        } else {
            showAlert('danger', `<i class="bi bi-x-circle-fill me-2"></i>${json.message}`);
        }
    } catch (err) {
        showAlert('danger', '<i class="bi bi-wifi-off me-2"></i>Cannot connect to server. Ensure the Spring Boot backend is running.');
    }
}

// ============================================================
// CALL NEXT PATIENT
// ============================================================
async function callNextPatient() {
    try {
        const res  = await fetch(`${API_BASE}/patients/next/call`, { method: 'POST' });
        const json = await res.json();

        if (json.success && json.data) {
            showAlert('info', `<i class="bi bi-megaphone-fill me-2"></i>${json.message}`);
        } else {
            showAlert('warning', '<i class="bi bi-inbox me-2"></i>Queue is empty — no patients to call.');
        }
        await refreshAll();
    } catch (err) {
        showAlert('danger', '<i class="bi bi-wifi-off me-2"></i>Connection error.');
    }
}

// ============================================================
// REMOVE PATIENT
// ============================================================
async function removePatient(id, name) {
    if (!confirm(`Remove "${name}" from the queue?`)) return;

    try {
        const res  = await fetch(`${API_BASE}/patients/${id}`, { method: 'DELETE' });
        const json = await res.json();

        if (json.success) {
            showAlert('warning', `<i class="bi bi-trash3-fill me-2"></i>${json.message}`);
            await refreshAll();
        } else {
            showAlert('danger', `<i class="bi bi-x-circle me-2"></i>${json.message}`);
        }
    } catch (err) {
        showAlert('danger', '<i class="bi bi-wifi-off me-2"></i>Connection error.');
    }
}

// ============================================================
// UPDATE PRIORITY MODAL
// ============================================================
function openUpdatePriority(id, name, currentPriority) {
    document.getElementById('modalPatientId').value   = id;
    document.getElementById('modalPatientName').textContent = name;
    document.getElementById('modalNewPriority').value = '';

    // Reset modal priority options
    document.querySelectorAll('#updatePriorityModal .priority-option').forEach(el => {
        el.classList.remove('active');
        if (parseInt(el.dataset.value) === currentPriority) el.classList.add('active');
    });

    updatePriorityModal.show();
}

function selectModalPriority(el) {
    document.querySelectorAll('#updatePriorityModal .priority-option').forEach(o => o.classList.remove('active'));
    el.classList.add('active');
    document.getElementById('modalNewPriority').value = el.dataset.value;
}

async function confirmUpdatePriority() {
    const id          = document.getElementById('modalPatientId').value;
    const newPriority = parseInt(document.getElementById('modalNewPriority').value);

    if (!newPriority) { alert('Please select a priority level.'); return; }

    try {
        const res  = await fetch(`${API_BASE}/patients/${id}/priority`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ priorityLevel: newPriority })
        });
        const json = await res.json();

        updatePriorityModal.hide();
        if (json.success) {
            showAlert('success', `<i class="bi bi-check-circle-fill me-2"></i>${json.message}`);
            await refreshAll();
        } else {
            showAlert('danger', `<i class="bi bi-x-circle me-2"></i>${json.message}`);
        }
    } catch (err) {
        showAlert('danger', '<i class="bi bi-wifi-off me-2"></i>Connection error.');
    }
}

// ============================================================
// SEARCH & FILTER
// ============================================================
let searchDebounce = null;
function searchPatients() {
    clearTimeout(searchDebounce);
    searchDebounce = setTimeout(() => {
        document.getElementById('priorityFilter').value = '';
        refreshQueue();
    }, 350);
}

function filterByPriority() {
    document.getElementById('searchInput').value = '';
    refreshQueue();
}

function clearSearch() {
    document.getElementById('searchInput').value = '';
    document.getElementById('priorityFilter').value = '';
    refreshQueue();
}

// ============================================================
// TAB SWITCHING
// ============================================================
function switchTab(tab) {
    currentTab = tab;
    ['queue','history'].forEach(t => {
        document.getElementById(`tab-${t}`).classList.toggle('active', t === tab);
        document.getElementById(`panel-${t}`).classList.toggle('d-none', t !== tab);
    });
    if (tab === 'history') loadHistory();
    else refreshQueue();
}

// ============================================================
// AUTOMATIC TRIAGE PREVIEW
// ============================================================
const TRIAGE_KEYWORDS = [
    { level: 1, label: 'Critical', keywords: ['heart attack', 'chest pain', 'stroke', 'unconscious', 'severe bleeding', 'cardiac arrest'] },
    { level: 2, label: 'Serious', keywords: ['high fever', 'breathing difficulty', 'major fracture', 'severe burns'] },
    { level: 3, label: 'Moderate', keywords: ['moderate fever', 'vomiting', 'dehydration', 'migraine'] },
    { level: 4, label: 'Minor', keywords: ['minor fracture', 'sprain', 'mild injury', 'cough'] },
    { level: 5, label: 'Normal', keywords: ['routine checkup', 'general consultation', 'follow-up visit', 'follow up visit'] }
];

function detectPriorityFromSymptoms(symptoms) {
    const text = (symptoms || '').toLowerCase();
    for (const rule of TRIAGE_KEYWORDS) {
        const matches = rule.keywords.filter(k => text.includes(k));
        if (matches.length) {
            return { level: rule.level, label: rule.label, reason: `Matched keywords: ${matches.join(', ')}` };
        }
    }
    return { level: 5, label: 'Normal', reason: 'No specific emergency keywords matched; assigned P5 by default.' };
}

function updateTriagePreview() {
    const text = document.getElementById('patientSymptoms').value.trim();
    const triage = detectPriorityFromSymptoms(text);
    const preview = document.getElementById('triagePreview');
    const reason = document.getElementById('triageReasonText');

    preview.querySelector('.badge').className = `badge bg-${triage.level === 1 ? 'danger' : triage.level === 2 ? 'warning text-dark' : triage.level === 3 ? 'info text-dark' : triage.level === 4 ? 'success' : 'secondary'}`;
    preview.querySelector('.badge').textContent = `Detected Priority: P${triage.level} ${triage.label}`;
    reason.textContent = triage.reason;
}

// ============================================================
// ALERTS
// ============================================================
function showAlert(type, html) {
    const area  = document.getElementById('alertArea');
    const alert = document.createElement('div');
    alert.className = `alert alert-${type} alert-dismissible fade show`;
    alert.innerHTML = `${html}<button type="button" class="btn-close" data-bs-dismiss="alert"></button>`;
    area.appendChild(alert);
    setTimeout(() => { if (alert.parentNode) alert.remove(); }, 5000);
}

// ============================================================
// HELPERS
// ============================================================
function formatDateTime(dtStr) {
    if (!dtStr) return '—';
    const d = new Date(dtStr);
    return d.toLocaleDateString('en-IN', { day:'2-digit', month:'short' }) + ' ' +
           d.toLocaleTimeString('en-IN', { hour:'2-digit', minute:'2-digit' });
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
