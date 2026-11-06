// ============ AUTOCOMPLETE SEARCH ============

let searchTimeout = null;
let selectedPersona = null;

const searchInput = document.getElementById('searchInput');
const autocompleteList = document.getElementById('autocompleteList');
const personaCard = document.getElementById('personaCard');
const submitBtn = document.getElementById('submitBtn');
const personaIdInput = document.getElementById('personaId');

if (searchInput) {
    searchInput.addEventListener('input', function () {
        const query = this.value.trim();
        clearTimeout(searchTimeout);

        if (query.length < 2) {
            hideAutocomplete();
            return;
        }

        searchTimeout = setTimeout(() => buscarPersonas(query), 250);
    });

    searchInput.addEventListener('keydown', function (e) {
        const items = autocompleteList.querySelectorAll('.autocomplete-item');
        let activeIndex = Array.from(items).findIndex(i => i.classList.contains('active'));

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            activeIndex = Math.min(activeIndex + 1, items.length - 1);
            items.forEach((item, i) => item.classList.toggle('active', i === activeIndex));
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            activeIndex = Math.max(activeIndex - 1, 0);
            items.forEach((item, i) => item.classList.toggle('active', i === activeIndex));
        } else if (e.key === 'Enter') {
            e.preventDefault();
            if (activeIndex >= 0) items[activeIndex].click();
        } else if (e.key === 'Escape') {
            hideAutocomplete();
        }
    });
}

async function buscarPersonas(query) {
    try {
        const response = await fetch(`/api/personas/buscar?q=${encodeURIComponent(query)}`);
        const personas = await response.json();
        mostrarAutocomplete(personas);
    } catch (err) {
        console.error('Error buscando personas:', err);
    }
}

function mostrarAutocomplete(personas) {
    if (!autocompleteList) return;

    if (personas.length === 0) {
        autocompleteList.innerHTML = `
            <div class="autocomplete-item" style="cursor:default; color:#8b6013; padding: 1rem;">
                <i class="bi bi-search me-2" style="color:var(--dorado-oscuro); font-size: 1.1rem;"></i>
                <span>No se encontraron hermanos/as con ese nombre en el padrón</span>
            </div>`;
        autocompleteList.style.display = 'block';
        return;
    }

    const header = `
        <div style="padding: 7px 14px; background: #fffcf4; border-bottom: 1px solid #f2ead8; font-size: 0.78rem; font-weight: 700; color: var(--dorado-oscuro); letter-spacing: 0.4px;">
            <i class="bi bi-people-fill me-1"></i> ${personas.length} ${personas.length === 1 ? 'hermano/a encontrado' : 'hermanos/as encontrados'} (haz clic para seleccionar)
        </div>`;

    const items = personas.map(p => {
        const ini = ((p.nombres ? p.nombres.charAt(0) : '') + (p.apellidos ? p.apellidos.charAt(0) : '')).toUpperCase();
        return `
        <div class="autocomplete-item" onclick="seleccionarPersona(${JSON.stringify(p).replace(/"/g, '&quot;')})">
            <div class="person-avatar">${ini}</div>
            <div class="person-info">
                <strong>${p.nombreCompleto}</strong>
                <small><i class="bi bi-people"></i> Grupo ${p.numeroGrupo} &bull; ${p.rolDisplay} &bull; <i class="bi bi-buildings"></i> ${p.iglesiaDisplay || 'Canto Grande'}</small>
            </div>
        </div>
        `;
    }).join('');

    autocompleteList.innerHTML = header + items;
    autocompleteList.style.display = 'block';
}

function seleccionarPersona(persona) {
    selectedPersona = persona;
    searchInput.value = persona.nombreCompleto;
    hideAutocomplete();
    mostrarPersonaCard(persona);

    if (personaIdInput) personaIdInput.value = persona.id;

    // Sincronizar automáticamente la iglesia de procedencia en el Paso 1
    if (persona.iglesia) {
        const selectIglesia = document.getElementById('selectIglesia');
        if (selectIglesia) {
            selectIglesia.value = persona.iglesia;
        }
    }

    if (submitBtn) submitBtn.disabled = false;
}

function mostrarPersonaCard(persona) {
    if (!personaCard) return;

    const iniciales = ((persona.nombres ? persona.nombres.charAt(0) : '') + (persona.apellidos ? persona.apellidos.charAt(0) : '')).toUpperCase();

    // Iconos por rol
    const rolIcons = {
        'LIDER_CAPITAN': '👑',
        'LIDER_ALTERNO': '⭐',
        'DIRECTIVO': '🏛️',
        'MIEMBRO': '🙋',
        'ASPIRANTE': '🌱'
    };

    const icon = rolIcons[persona.rol] || '👤';

    personaCard.innerHTML = `
        <div class="persona-avatar-large">${iniciales}</div>
        <div class="persona-name">${persona.nombreCompleto}</div>
        <div class="d-flex justify-content-center gap-2 flex-wrap">
            <span class="badge" style="background:linear-gradient(135deg, #5C3A00, #3E2700); color:#FFD700; border-radius:8px; padding:0.4rem 0.75rem; font-weight:700;">
                <i class="bi bi-buildings"></i> ${persona.iglesiaDisplay || 'Iglesia Canto Grande'}
            </span>
            <span class="badge-rol badge-${persona.rol}">${icon} ${persona.rolDisplay}</span>
            <span class="badge-grupo"><i class="bi bi-people-fill"></i> Grupo ${persona.numeroGrupo}</span>
        </div>
    `;
    personaCard.style.display = 'block';
}

function hideAutocomplete() {
    if (autocompleteList) autocompleteList.style.display = 'none';
}

// Close autocomplete clicking outside
document.addEventListener('click', function (e) {
    if (!e.target.closest('.search-container')) {
        hideAutocomplete();
    }
});

// ============ MODALIDAD SELECTION ============

function selectModalidad(value) {
    document.querySelectorAll('.modalidad-option').forEach(el => el.classList.remove('selected'));
    const selected = document.querySelector(`.modalidad-option[data-value="${value}"]`);
    if (selected) {
        selected.classList.add('selected');
        selected.querySelector('input[type="radio"]').checked = true;
    }
}

// ============ MEDIO DE PAGO SELECTION ============

function selectMedioPago(value) {
    document.querySelectorAll('.pago-option').forEach(el => el.classList.remove('selected'));
    const selected = document.querySelector(`.pago-option[data-value="${value}"]`);
    if (selected) {
        selected.classList.add('selected');
        selected.querySelector('input[type="radio"]').checked = true;
    }
}

// ============ CONFIRM DELETE ============

function confirmarEliminar(nombre) {
    return confirm(`¿Estás seguro de eliminar el registro de ${nombre}?`);
}

document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.btn-eliminar').forEach(btn => {
        btn.addEventListener('click', function (e) {
            const nombre = this.getAttribute('data-nombre') || 'este asistente';
            if (!confirmarEliminar(nombre)) {
                e.preventDefault();
            }
        });
    });
});

// ============ STATS ANIMATION ============

function animateNumber(element, target) {
    let current = 0;
    const step = Math.ceil(target / 20);
    const timer = setInterval(() => {
        current = Math.min(current + step, target);
        element.textContent = current;
        if (current >= target) clearInterval(timer);
    }, 50);
}

document.querySelectorAll('.stat-number[data-value]').forEach(el => {
    animateNumber(el, parseInt(el.dataset.value));
});
