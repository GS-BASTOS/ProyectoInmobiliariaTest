(function () {
  const csrfToken  = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';

  const INPUT_IDS = ['fDesde','fHasta','fCliente','fTelefono','fEmail','fInmueble'];

  /* Marcar visita como hecha */
  async function markDone(btn) {
    const id = btn.getAttribute('data-id');
    btn.disabled = true;
    btn.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> Guardando…';
    try {
      const res = await fetch('/visitas/' + id + '/realizada', {
        method: 'POST',
        headers: { [csrfHeader]: csrfToken }
      });
      if (!res.ok) throw new Error('HTTP ' + res.status);
      const row = btn.closest('tr');
      if (!row) return;
      row.style.transition = 'opacity .25s';
      row.style.opacity = '0';
      setTimeout(() => { row.remove(); applyFilters(); }, 260);
    } catch (e) {
      btn.disabled = false;
      btn.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> Hecha';
      alert('Error al actualizar la visita: ' + e.message);
    }
  }

  function getInput(id) {
    return document.getElementById(id);
  }

  function applyFilters() {
    const desde    = getInput('fDesde')?.value || '';
    const hasta    = getInput('fHasta')?.value || '';
    const cliente  = (getInput('fCliente')?.value || '').trim().toLowerCase();
    const telefono = (getInput('fTelefono')?.value || '').trim().toLowerCase();
    const email    = (getInput('fEmail')?.value || '').trim().toLowerCase();
    const inmueble = (getInput('fInmueble')?.value || '').trim().toLowerCase();

    const rows = document.querySelectorAll('#visitsBody tr');
    let visible = 0;

    rows.forEach(row => {
      const fecha       = row.getAttribute('data-fecha') || '';
      const clienteTxt  = (row.getAttribute('data-cliente') || '').toLowerCase();
      const inmuebleTxt = (row.getAttribute('data-inmueble') || '').toLowerCase();
      const phonesTxt   = (row.querySelector('.cell-phones')?.innerText || '').toLowerCase();
      const emailsTxt   = (row.querySelector('.cell-emails')?.innerText || '').toLowerCase();

      const ok =
        (!desde    || fecha >= desde) &&
        (!hasta    || fecha <= hasta) &&
        (!cliente  || clienteTxt.includes(cliente)) &&
        (!telefono || phonesTxt.includes(telefono)) &&
        (!email    || emailsTxt.includes(email)) &&
        (!inmueble || inmuebleTxt.includes(inmueble));

      row.style.display = ok ? '' : 'none';
      if (ok) visible++;
    });

    const counter = getInput('filterCount');
    if (counter) {
      const total = rows.length;
      counter.textContent = visible < total
        ? `${visible} de ${total} visitas`
        : `${total} visitas`;
    }

    const noRes = document.getElementById('noResults');
    if (noRes) {
      noRes.style.display = visible === 0 ? 'block' : 'none';
    }
  }

  function resetFilters() {
    INPUT_IDS.forEach(id => {
      const el = getInput(id);
      if (el) el.value = '';
    });
    applyFilters();
  }

  function highlightRows() {
    const hoy = new Date().toISOString().split('T')[0];
    document.querySelectorAll('#visitsBody tr').forEach(row => {
      const fecha = row.getAttribute('data-fecha') || '';
      if (fecha === hoy) row.classList.add('row-hoy');
      else if (fecha > hoy) row.classList.add('row-proxima');
    });
  }

  /* Modal edición */
  const modalEdit = document.getElementById('modalEdit');
  const editForm  = document.getElementById('editForm');
  const editDate  = document.getElementById('editDate');
  const editTime  = document.getElementById('editTime');
  const editNotes = document.getElementById('editNotes');

  async function openEditModal(btn) {
    const id = btn.getAttribute('data-id');
    try {
      const res = await fetch('/visitas/' + id);
      if (!res.ok) throw new Error('HTTP ' + res.status);
      const data = await res.json();
      if (editDate) editDate.value  = data.visitDate;
      if (editTime) editTime.value  = data.visitTime.substring(0, 5);
      if (editNotes) editNotes.value = data.notes || '';
      if (editForm) editForm.action = '/visitas/' + id + '/actualizar';
      modalEdit?.classList.add('open');
    } catch (e) {
      alert('Error al cargar la visita: ' + e.message);
    }
  }

  function closeEditModal() {
    modalEdit?.classList.remove('open');
  }

  modalEdit?.addEventListener('click', function (e) {
    if (e.target === modalEdit) closeEditModal();
  });

  /* Modal eliminar + estado cliente */
  const modalDelete   = document.getElementById('modalDelete');
  const deleteVisitId = document.getElementById('deleteVisitId');
  const deleteEstado  = document.getElementById('deleteEstado');

  function openDeleteModal(btn) {
    if (!deleteVisitId || !deleteEstado) return;
    deleteVisitId.value = btn.getAttribute('data-id');
    deleteEstado.value  = '';
    document.querySelectorAll('#modalDelete .status-pill').forEach(p => p.classList.remove('selected'));
    modalDelete?.classList.add('open');
  }

  function closeDeleteModal() {
    modalDelete?.classList.remove('open');
  }

  modalDelete?.addEventListener('click', function (e) {
    if (e.target === modalDelete) closeDeleteModal();
  });

  function selectEstado(btn) {
    if (!deleteEstado) return;
    const value = btn.getAttribute('data-value');
    deleteEstado.value = value;
    document.querySelectorAll('#modalDelete .status-pill').forEach(p => p.classList.remove('selected'));
    btn.classList.add('selected');
  }

  async function confirmDelete() {
    if (!deleteVisitId) return;
    const id     = deleteVisitId.value;
    const estado = deleteEstado?.value || '';
    if (!id) return;

    try {
      const params = estado ? ('?nuevoEstado=' + encodeURIComponent(estado)) : '';
      const res = await fetch('/visitas/' + id + '/eliminar' + params, {
        method: 'POST',
        headers: { [csrfHeader]: csrfToken }
      });
      if (!res.ok) throw new Error('HTTP ' + res.status);

      const row = document.querySelector(`#visitsBody tr[data-id="${id}"]`);
      if (row) {
        row.style.transition = 'opacity .25s';
        row.style.opacity = '0';
        setTimeout(() => { row.remove(); applyFilters(); }, 260);
      }
      closeDeleteModal();
    } catch (e) {
      alert('Error al eliminar la visita: ' + e.message);
    }
  }

  /* Eventos iniciales */
  INPUT_IDS.forEach(id => {
    const el = getInput(id);
    if (el) el.addEventListener('input', applyFilters);
  });

  highlightRows();
  applyFilters();

  /* Exponer API mínima en window para los manejadores inline del HTML */
  window.visitsScheduled = {
    markDone,
    resetFilters,
    openEditModal,
    closeEditModal,
    openDeleteModal,
    closeDeleteModal,
    selectEstado,
    confirmDelete
  };
})();
