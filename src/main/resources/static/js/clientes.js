(function () {
  const toast = document.getElementById('copyToast');
  if (!toast) return;

  let toastTimer;
  function showToast(msg) {
    toast.textContent = msg + ' ✓';
    toast.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => toast.classList.remove('show'), 1800);
  }

  document.addEventListener('click', async function (e) {
    if (e.target.tagName === 'INPUT') return;
    if (e.target.closest('.wa-link')) return;
    const el = e.target.closest('.copyable');
    if (!el) return;
    if (el.closest('tr[data-no-molestar]')) return;
    const text = el.getAttribute('data-copy') || el.textContent.trim();
    try {
      await navigator.clipboard.writeText(text);
    } catch (_) {
      const ta = document.createElement('textarea');
      ta.value = text;
      ta.style.cssText = 'position:fixed;left:-9999px';
      document.body.appendChild(ta);
      ta.select();
      document.execCommand('copy');
      document.body.removeChild(ta);
    }
    showToast('Copiado: ' + text);
  });

  const selectAllEmailsCb  = document.getElementById('selectAllEmails');
  const selectAllEmailText = document.getElementById('selectAllEmailText');
  const emailsCount        = document.getElementById('emailsCount');
  const copyEmailsBtn      = document.getElementById('copyEmailsBtn');
  const clearEmailsBtn     = document.getElementById('clearEmailsBtn');

  if (!selectAllEmailsCb || !selectAllEmailText || !emailsCount || !copyEmailsBtn || !clearEmailsBtn) {
    return;
  }

  function getEmailItems() {
    return Array.from(document.querySelectorAll('.email-item'));
  }

  function getSelectableEmailItems() {
    return getEmailItems().filter(cb =>
      cb.closest('tr[data-excluido]') === null &&
      cb.closest('tr[data-no-molestar]') === null
    );
  }

  function getSelectedEmails() {
    const set = new Set();
    getEmailItems().filter(cb => cb.checked).forEach(cb => {
      const v = (cb.getAttribute('data-email') || '').trim().toLowerCase();
      if (v) set.add(v);
    });
    return Array.from(set).sort();
  }

  function syncSelectAllEmails() {
    const all     = getSelectableEmailItems();
    const checked = getEmailItems().filter(cb => cb.checked);

    if (!all.length || !checked.length) {
      selectAllEmailsCb.checked = false;
      selectAllEmailsCb.indeterminate = false;
      selectAllEmailText.textContent = 'Seleccionar todos los emails';
    } else if (checked.length === all.length) {
      selectAllEmailsCb.checked = true;
      selectAllEmailsCb.indeterminate = false;
      selectAllEmailText.textContent = 'Deseleccionar todos';
    } else {
      selectAllEmailsCb.checked = false;
      selectAllEmailsCb.indeterminate = true;
      selectAllEmailText.textContent = `Seleccionar todos (${checked.length}/${all.length})`;
    }
  }

  function renderEmails() {
    const list = getSelectedEmails();
    emailsCount.textContent      = String(list.length);
    copyEmailsBtn.disabled       = list.length === 0;
    clearEmailsBtn.style.display = list.length === 0 ? 'none' : 'inline-flex';
    syncSelectAllEmails();
    return list;
  }

  function clearEmailSelection() {
    getEmailItems().forEach(cb => cb.checked = false);
    renderEmails();
  }

  selectAllEmailsCb.addEventListener('change', () => {
    getSelectableEmailItems().forEach(cb => cb.checked = selectAllEmailsCb.checked);
    renderEmails();
  });

  document.addEventListener('change', ev => {
    if (ev.target && ev.target.classList && ev.target.classList.contains('email-item')) {
      renderEmails();
    }
  });

  copyEmailsBtn.addEventListener('click', async () => {
    const list = renderEmails();
    if (!list.length) return;
    const text = list.join('; ');
    try {
      await navigator.clipboard.writeText(text);
    } catch (_) {
      const ta = document.createElement('textarea');
      ta.value = text;
      ta.setAttribute('readonly', '');
      ta.style.cssText = 'position:fixed;left:-9999px';
      document.body.appendChild(ta);
      ta.select();
      document.execCommand('copy');
      document.body.removeChild(ta);
    }
    showToast(list.length + ' email(s) copiados');
    clearEmailSelection();
  });

  clearEmailsBtn.addEventListener('click', clearEmailSelection);

  renderEmails();
})();
