(function(){
  let idCounter = 0;
  function ensureControls(section) {
    const btn = section.querySelector('.collapse-toggle');
    const content = section.querySelector('.collapse-content');
    if (!btn || !content) return;
    if (!content.id) {
      content.id = `collapse-section-${++idCounter}`;
    }
    btn.setAttribute('aria-controls', content.id);
  }

  function updateButtonLabel(btn, expanded) {
    const labelSpan = btn.querySelector('span');
    if (labelSpan) labelSpan.textContent = expanded ? 'Recolher' : 'Expandir';
  }

  function toggle(section, setCollapsed) {
    ensureControls(section);
    const willCollapse = setCollapsed !== undefined ? setCollapsed : !section.classList.contains('collapsed');
    if (willCollapse) section.classList.add('collapsed');
    else section.classList.remove('collapsed');
    const btn = section.querySelector('.collapse-toggle');
    if (btn) {
      const expanded = !willCollapse;
      btn.setAttribute('aria-expanded', String(expanded));
      updateButtonLabel(btn, expanded);
    }
  }

  document.addEventListener('click', function(e){
    const btn = e.target.closest('.collapse-toggle');
    if (!btn) return;
    const section = btn.closest('.collapsible');
    if (!section) return;
    e.preventDefault();
    toggle(section);
  });

  // On load, set initial aria states
  document.addEventListener('DOMContentLoaded', function(){
    document.querySelectorAll('.collapsible').forEach(section => {
      ensureControls(section);
      const btn = section.querySelector('.collapse-toggle');
      if (btn) {
        const expanded = !section.classList.contains('collapsed');
        btn.setAttribute('aria-expanded', String(expanded));
        updateButtonLabel(btn, expanded);
      }
    });
  });
})();
