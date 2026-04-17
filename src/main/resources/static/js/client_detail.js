(function () {
  const csrfToken  = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';

  /* ══════════════════════════════
     TABS CLIENTE
  ══════════════════════════════ */
  const tabButtons = document.querySelectorAll('.client-tab[data-target]');
  const tabPanels  = document.querySelectorAll('.client-tab-panel');

  function activateFromHash() {
    const hash  = window.location.hash || '#interacciones';
    const id    = hash.replace('#', 'tab-');
    const panel = document.getElementById(id);
    if (!panel) return;
    tabButtons.forEach(b => b.classList.remove('active'));
    tabPanels.forEach(p => p.classList.remove('active'));
    const btn = document.querySelector(`.client-tab[data-target="#${id}"]`);
    if (btn) btn.classList.add('active');
    panel.classList.add('active');
  }

  tabButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetSelector = btn.getAttribute('data-target');
      if (!targetSelector) return;
      tabButtons.forEach(b => b.classList.remove('active'));
      tabPanels.forEach(p => p.classList.remove('active'));
      btn.classList.add('active');
      const panel = document.querySelector(targetSelector);
      if (panel) panel.classList.add('active');
      const hash = targetSelector.replace('#tab-', '#');
      if (history.replaceState) {
        history.replaceState(null, '', hash);
      } else {
        location.hash = hash;
      }
    });
  });

  window.addEventListener('hashchange', activateFromHash);
  activateFromHash();

  /* ══════════════════════════════
     COMBOBOX CATALOGO
  ══════════════════════════════ */
  const comboInput    = document.getElementById('niComboInput');
  const dropdown      = document.getElementById('niComboDropdown');
  const chip          = document.getElementById('niComboChip');
  const chipLabel     = document.getElementById('niComboChipLabel');
  const comboClearBtn = document.getElementById('niComboClearBtn');
  const wrapper       = document.getElementById('niComboWrapper');

  let currentHighlight = -1;
  let visibleOptions   = [];

  const allItems = (window.CATALOG || [])
    .filter(p => !p.sold)
    .map(p => ({
      code        : p.propertyCode  || '',
      type        : p.propertyType  || '',
      address     : p.address       || '',
      municipality: p.municipality  || '',
      preVendido  : p.preVendido    || false,
      label       : [p.propertyCode, p.propertyType, p.municipality].filter(Boolean).join(' · ')
    }));

  function escHtml(str) {
    return (str||'').replace(/&/g,'&amp;')
                    .replace(/</g,'&lt;')
                    .replace(/>/g,'&gt;')
                    .replace(/"/g,'&quot;');
  }

  function renderDropdown(q) {
    if (!dropdown) return;
    dropdown.innerHTML = '';
    currentHighlight   = -1;
    const lq = (q || '').trim().toLowerCase();
    visibleOptions = lq === ''
      ? allItems
      : allItems.filter(item =>
          item.code.toLowerCase().includes(lq)         ||
          item.type.toLowerCase().includes(lq)         ||
          item.municipality.toLowerCase().includes(lq) ||
          item.address.toLowerCase().includes(lq)
        );
    if (visibleOptions.length === 0) {
      dropdown.innerHTML = '<div class="combo-empty">Sin resultados</div>';
      dropdown.classList.add('open');
      return;
    }
    visibleOptions.forEach((item, idx) => {
      const div = document.createElement('div');
      div.className   = 'combo-option';
      div.dataset.idx = String(idx);
      const preTag = item.preVendido
        ? `<span style="margin-left:6px;font-size:10px;color:#a05800;background:#fff4e6;border:1px solid #e07a18;border-radius:999px;padding:1px 6px;font-weight:700;">⏳ Pre vendido</span>`
        : '';
      div.innerHTML = `
        <div class="opt-code">${escHtml(item.code)}${preTag}</div>
        <div class="opt-sub">${escHtml([item.type, item.municipality, item.address].filter(Boolean).join(' · '))}</div>
      `;
      div.addEventListener('mousedown', e => {
        e.preventDefault();
        selectItem(item);
      });
      dropdown.appendChild(div);
    });
    dropdown.classList.add('open');
  }

  function selectItem(item) {
    const codeInput  = document.getElementById('niPropertyCode');
    const typeInput  = document.getElementById('niPropertyType');
    const addrInput  = document.getElementById('niAddress');
    const munInput   = document.getElementById('niMunicipality');
    if (codeInput) codeInput.value = item.code;
    if (typeInput) typeInput.value = item.type;
    if (addrInput) addrInput.value = item.address;
    if (munInput)  munInput.value  = item.municipality;
    if (comboInput) {
      comboInput.value    = item.label;
      comboInput.readOnly = true;
    }
    if (chip && chipLabel) {
      chipLabel.textContent = item.label;
      chip.style.display    = 'inline-flex';
    }
    closeDropdown();
  }

  function clearSelection() {
    if (comboInput) {
      comboInput.value    = '';
      comboInput.readOnly = false;
    }
    if (chip) chip.style.display = 'none';
    ['niPropertyCode','niPropertyType','niAddress','niMunicipality'].forEach(id => {
      const el = document.getElementById(id);
      if (el) el.value = '';
    });
  }

  function openDropdown(q) { renderDropdown(q); }
  function closeDropdown() {
    if (!dropdown) return;
    dropdown.classList.remove('open');
    currentHighlight = -1;
  }

  function setHighlight(idx) {
    if (!dropdown) return;
    const opts = dropdown.querySelectorAll('.combo-option');
    opts.forEach(o => o.classList.remove('highlighted'));
    if (idx >= 0 && idx < opts.length) {
      opts[idx].classList.add('highlighted');
      opts[idx].scrollIntoView({ block:'nearest' });
      currentHighlight = idx;
    }
  }

  if (comboInput) {
    comboInput.addEventListener('click', () => {
      comboInput.readOnly = false;
      comboInput.value    = '';
      openDropdown('');
    });
    comboInput.addEventListener('input', () => openDropdown(comboInput.value));
    comboInput.addEventListener('keydown', e => {
      const opts = dropdown ? dropdown.querySelectorAll('.combo-option') : [];
      if (e.key === 'ArrowDown') {
        e.preventDefault();
        setHighlight(Math.min(currentHighlight + 1, opts.length - 1));
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        setHighlight(Math.max(currentHighlight - 1, 0));
      } else if (e.key === 'Enter') {
        e.preventDefault();
        if (currentHighlight >= 0 && visibleOptions[currentHighlight]) {
          selectItem(visibleOptions[currentHighlight]);
        }
      } else if (e.key === 'Escape') {
        closeDropdown();
      }
    });
    comboInput.addEventListener('blur', () => setTimeout(closeDropdown, 150));
  }

  if (comboClearBtn) {
    comboClearBtn.addEventListener('click', clearSelection);
  }

  document.addEventListener('click', e => {
    if (wrapper && !wrapper.contains(e.target)) {
      closeDropdown();
    }
  });

  /* ══════════════════════════════
     PANELES PRE VENDA / COMPRADOR
  ══════════════════════════════ */
  const chkPreVenda   = document.getElementById('chkPreVenda');
  const panelPreVenda = document.getElementById('panelPreVenda');
  if (chkPreVenda && panelPreVenda) {
    chkPreVenda.addEventListener('change', () => {
      panelPreVenda.style.display = chkPreVenda.checked ? 'block' : 'none';
    });
  }

  const chkComprador   = document.getElementById('chkCompradorFinal');
  const panelComprador = document.getElementById('panelInmuebleComprado');
  if (chkComprador && panelComprador) {
    chkComprador.addEventListener('change', () => {
      panelComprador.style.display = chkComprador.checked ? 'block' : 'none';
    });
  }

  /* ══════════════════════════════
     NDA TOGGLE
  ══════════════════════════════ */
  document.querySelectorAll('.nda-toggle').forEach(cb => {
    cb.addEventListener('change', async function () {
      const clientId      = cb.getAttribute('data-client-id');
      const interactionId = cb.getAttribute('data-interaction-id');
      const checked       = cb.checked;
      try {
        const res = await fetch(`/clientes/${clientId}/interacciones/${interactionId}/nda`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            [csrfHeader]: csrfToken
          },
          body: 'ndaRequested=' + encodeURIComponent(String(checked))
        });
        if (!res.ok) throw new Error('HTTP ' + res.status);
      } catch (e) {
        cb.checked = !checked;
        alert('No se pudo actualizar NDA.');
      }
    });
  });

  /* ══════════════════════════════
     TICKET SAVE
  ══════════════════════════════ */
  document.querySelectorAll('.ticket-save').forEach(btn => {
    const cell     = btn.closest('td');
    const feedback = cell?.querySelector('.ticket-feedback');
    if (feedback) {
      feedback.style.display = 'none';
    }
    btn.addEventListener('click', async function () {
      const clientId      = btn.getAttribute('data-client-id');
      const interactionId = btn.getAttribute('data-interaction-id');
      const input         = cell?.querySelector('.ticket-input');
      if (!input) return;
      try {
        const res = await fetch(`/clientes/${clientId}/interacciones/${interactionId}/ticket`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            [csrfHeader]: csrfToken
          },
          body: 'ticketCode=' + encodeURIComponent(input.value)
        });
        if (!res.ok) throw new Error('HTTP ' + res.status);
        if (feedback) {
          feedback.style.display = 'inline';
          setTimeout(() => (feedback.style.display = 'none'), 2000);
        }
      } catch (e) {
        alert('No se pudo guardar el ticket.');
      }
    });
  });

  /* ══════════════════════════════
     STATUS SELECT
  ══════════════════════════════ */
  const statusClassMap = {
    'GRIS_SIN_CONTACTO'     : 'gris',
    'AZUL_VISITA_PROGRAMADA': 'azul-claro',
    'AZUL_VISITA_REALIZADA' : 'azul-oscuro',
    'NARANJA_QUIERE_VISITA' : 'naranja',
    'ROSA_DESCARTA'         : 'rosa',
    'VERDE_PENSANDO'        : 'verde',
    'AMARILLO_OFERTA'       : 'amarillo'
  };

  document.querySelectorAll('.status-select').forEach(sel => {
    const cell = sel.closest('td');
    const dot  = cell?.querySelector('.status-dot');
    function applyDotClass(value) {
      if (!dot) return;
      dot.className = 'dot status-dot';
      const cls = statusClassMap[value];
      if (cls) dot.classList.add(cls);
      dot.setAttribute('data-status', value);
    }
    if (dot) {
      applyDotClass(dot.getAttribute('data-status') || sel.value);
    }
    sel.addEventListener('change', async function () {
      const clientId      = sel.getAttribute('data-client-id');
      const interactionId = sel.getAttribute('data-interaction-id');
      try {
        const res = await fetch(`/clientes/${clientId}/interacciones/${interactionId}/status`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            [csrfHeader]: csrfToken
          },
          body: 'status=' + encodeURIComponent(sel.value)
        });
        if (!res.ok) throw new Error('HTTP ' + res.status);
        applyDotClass(sel.value);
      } catch (e) {
        alert('No se pudo actualizar el estado.');
      }
    });
  });

  /* ══════════════════════════════
     MODAL COMENTARIOS
  ══════════════════════════════ */
  const commentEditor   = document.getElementById('commentEditor');
  const commentBackdrop = document.getElementById('commentModalBackdrop');
  const btnCancel       = document.getElementById('btnCancelComment');
  const btnSave         = document.getElementById('btnSaveComment');
  const btnHighlight    = document.getElementById('btnHighlight');
  const btnClearMark    = document.getElementById('btnClearMark');
  const btnClearAll     = document.getElementById('btnClearAll');

  let _commentTargetSpan = null;
  let _commentClientId   = null;
  let _commentIid        = null;

  function sanitizeHtml(html) {
    const div = document.createElement('div');
    div.innerHTML = html;
    const walker = document.createTreeWalker(div, NodeFilter.SHOW_ELEMENT);
    const toUnwrap = [];
    let node = walker.nextNode();
    while (node) {
      const tag = node.tagName.toLowerCase();
      if (tag !== 'mark' && tag !== 'br') toUnwrap.push(node);
      node = walker.nextNode();
    }
    toUnwrap.forEach(el => el.replaceWith(...el.childNodes));
    div.querySelectorAll('mark').forEach(m => {
      while (m.attributes.length > 0) m.removeAttribute(m.attributes[0].name);
    });
    return div.innerHTML;
  }

  function openCommentModal(span) {
    if (!commentEditor || !commentBackdrop) return;
    _commentTargetSpan = span;
    _commentClientId   = span.getAttribute('data-cid');
    _commentIid        = span.getAttribute('data-iid');
    const rawHtml      = span.innerHTML.trim();
    const isPlaceholder = span.classList.contains('comment-empty');
    commentEditor.innerHTML = isPlaceholder ? '' : rawHtml;
    commentBackdrop.classList.add('open');
    setTimeout(() => {
      commentEditor.focus();
      const range = document.createRange();
      range.selectNodeContents(commentEditor);
      range.collapse(false);
      const sel = window.getSelection();
      if (!sel) return;
      sel.removeAllRanges();
      sel.addRange(range);
    }, 80);
  }

  window.openCommentModal = openCommentModal;

  function closeCommentModal() {
    if (!commentBackdrop) return;
    commentBackdrop.classList.remove('open');
    _commentTargetSpan = null;
  }

  if (btnCancel && commentBackdrop) {
    btnCancel.addEventListener('click', closeCommentModal);
    commentBackdrop.addEventListener('click', e => {
      if (e.target === commentBackdrop) closeCommentModal();
    });
  }

  function applyHighlight() {
    if (!commentEditor) return;
    const sel = window.getSelection();
    if (!sel || sel.isCollapsed) return;
    const range = sel.getRangeAt(0);
    const ancestor = range.commonAncestorContainer;
    const markParent = ancestor.nodeType === 3
      ? ancestor.parentElement?.closest('mark')
      : ancestor.closest?.('mark');
    if (markParent) {
      markParent.replaceWith(...markParent.childNodes);
      sel.removeAllRanges();
      return;
    }
    const mark = document.createElement('mark');
    try {
      range.surroundContents(mark);
    } catch (_) {
      const fragment = range.extractContents();
      mark.appendChild(fragment);
      range.insertNode(mark);
    }
    sel.removeAllRanges();
  }

  if (commentEditor) {
    commentEditor.addEventListener('keydown', function(e) {
      if ((e.ctrlKey || e.altKey) && e.key === 'Enter') {
        e.preventDefault();
        document.execCommand('insertLineBreak');
      }
      if (e.ctrlKey && e.key === 'h') {
        e.preventDefault();
        applyHighlight();
      }
    });
  }

  if (btnHighlight && commentEditor) {
    btnHighlight.addEventListener('click', () => {
      commentEditor.focus();
      applyHighlight();
    });
  }

  if (btnClearMark && commentEditor) {
    btnClearMark.addEventListener('click', () => {
      commentEditor.focus();
      const sel = window.getSelection();
      if (!sel || sel.isCollapsed) return;
      const range = sel.getRangeAt(0);
      const fragment = range.extractContents();
      fragment.querySelectorAll('mark').forEach(m => m.replaceWith(...m.childNodes));
      range.insertNode(fragment);
      sel.removeAllRanges();
    });
  }

  if (btnClearAll && commentEditor) {
    btnClearAll.addEventListener('click', () => {
      commentEditor.innerHTML = '';
      commentEditor.focus();
    });
  }

  if (btnSave && commentEditor) {
    btnSave.addEventListener('click', async function() {
      if (!_commentIid || !_commentClientId || !_commentTargetSpan) return;
      const html = sanitizeHtml(commentEditor.innerHTML);
      try {
        const res = await fetch(`/clientes/${_commentClientId}/interacciones/${_commentIid}/comments`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            [csrfHeader]: csrfToken
          },
          body: 'comments=' + encodeURIComponent(html)
        });
        if (!res.ok) throw new Error('HTTP ' + res.status);
        if (!html || html.trim() === '' || html.trim() === '<br>') {
          _commentTargetSpan.innerHTML = '— <span>(editar)</span>';
          _commentTargetSpan.classList.add('comment-empty');
          _commentTargetSpan.classList.remove('comment-filled');
        } else {
          _commentTargetSpan.innerHTML = html;
          _commentTargetSpan.classList.remove('comment-empty');
          _commentTargetSpan.classList.add('comment-filled');
        }
        closeCommentModal();
      } catch(err) {
        alert('Error al guardar el comentario.');
        console.error(err);
      }
    });
  }
})();
