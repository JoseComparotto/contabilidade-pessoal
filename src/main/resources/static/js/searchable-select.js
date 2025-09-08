(function(){
  function enhanceSelect(sel){
    if(sel.dataset.enhanced==='1') return; sel.dataset.enhanced='1';
    var disabled = sel.disabled; // respect disabled state
    var wrapper = document.createElement('div');
    wrapper.className='searchable-select';
    sel.parentNode.insertBefore(wrapper, sel);
    wrapper.appendChild(sel);
    sel.classList.add('ss-native');

    var input = document.createElement('input');
    input.type='text';
    input.className='ss-input';
    input.placeholder= sel.dataset.placeholder || '';
    if(disabled) { input.disabled = true; }

    var btnClear = document.createElement('button');
    btnClear.type='button';
    btnClear.className='ss-clear';
    btnClear.innerHTML='&times;';
    btnClear.title='Limpar';

    var list = document.createElement('div');
    list.className='ss-options';
    list.setAttribute('role','listbox');

    var hiddenLive = document.createElement('div');
    hiddenLive.className='sr-only';
    hiddenLive.setAttribute('aria-live','polite');

    wrapper.appendChild(input);
    wrapper.appendChild(btnClear);
    wrapper.appendChild(list);
    wrapper.appendChild(hiddenLive);

    function buildOptions(filter){
      list.innerHTML='';
      var q = (filter||'').trim().toLowerCase();
      var count=0;
      Array.prototype.slice.call(sel.options).forEach(function(o){
        if(o.disabled) return; // skip placeholder
        if(q && o.text.toLowerCase().indexOf(q)===-1) return;
        var opt = document.createElement('div');
        opt.className='ss-option';
        opt.textContent=o.text; opt.dataset.value=o.value;
        opt.setAttribute('role','option');
        if(sel.value===o.value) opt.classList.add('selected');
        opt.onclick=function(){
          sel.value=o.value; sel.dispatchEvent(new Event('change', {bubbles:true}));
          input.value=o.text; close();
        };
        list.appendChild(opt); count++;
      });
      hiddenLive.textContent = count + ' opção' + (count!==1?'es':'');
      if(count===0){
        var empty=document.createElement('div'); empty.className='ss-empty'; empty.textContent='Nenhuma opção'; list.appendChild(empty);
      }
    }

    function open(){ if(disabled) return; wrapper.classList.add('open'); buildOptions(input.value); }
    function close(){ wrapper.classList.remove('open'); }

    input.addEventListener('focus', open);
    input.addEventListener('input', function(){ buildOptions(input.value); open(); });
    input.addEventListener('keydown', function(e){
      if(e.key==='ArrowDown'){
        e.preventDefault(); var first=list.querySelector('.ss-option'); if(first){ open(); first.focus(); }
      } else if(e.key==='Enter') {
        // Select first visible option on Enter
        if(!wrapper.classList.contains('open')) open();
        var first = list.querySelector('.ss-option');
        if(first){
          e.preventDefault();
          first.click();
        }
      } else if(e.key==='Escape'){ close(); }
    });

    list.addEventListener('keydown', function(e){
      var current=document.activeElement;
      if(!current.classList.contains('ss-option')) return;
      if(e.key==='ArrowDown'){ e.preventDefault(); var n=current.nextElementSibling; if(n && n.classList.contains('ss-option')) n.focus(); }
      else if(e.key==='ArrowUp'){ e.preventDefault(); var p=current.previousElementSibling; if(p && p.classList.contains('ss-option')) p.focus(); else input.focus(); }
      else if(e.key==='Enter'){ e.preventDefault(); current.click(); }
      else if(e.key==='Escape'){ close(); input.focus(); }
    });

    document.addEventListener('click', function(e){ if(!wrapper.contains(e.target)) close(); });
    btnClear.addEventListener('click', function(){ sel.value=''; input.value=''; sel.dispatchEvent(new Event('change', {bubbles:true})); buildOptions(''); input.focus(); });

    // Initialize input with current selection text
    var selected = sel.options[sel.selectedIndex];
    if(selected && !selected.disabled){ input.value = selected.text; }

    // Hide native select visually but keep for form submit
    // Already moved inside wrapper: we can set display none but keep accessibility? We'll just visually hide.
    sel.style.position='absolute'; sel.style.opacity='0'; sel.style.pointerEvents='none'; sel.style.height='0'; sel.style.width='0'; sel.tabIndex=-1;
  }

  function init(){
    document.querySelectorAll('select[data-enhance="searchable-select"]').forEach(enhanceSelect);
  }
  document.addEventListener('DOMContentLoaded', init);
})();
