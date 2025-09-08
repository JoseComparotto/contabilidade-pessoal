// Simple currency formatting enhancer for inputs with data-enhance="money"
// Assumes locale pt-BR by default. Keeps a hidden raw numeric value for server side if needed.
(function(){
  const locale = 'pt-BR';
  const moneyInputs = document.querySelectorAll('input[data-enhance="money"]');
  if(!moneyInputs.length) return;

  function parseToNumber(str, scale){
    if(!str) return '';
    // Remove everything except digits and comma/dot
    const cleaned = str.replace(/[^0-9,\.]/g,'').replace(/\./g,'').replace(/,/g,'.');
    if(!cleaned) return '';
    const num = Number(cleaned);
    if(Number.isNaN(num)) return '';
    // Keep raw numeric with fixed scale for consistency (server expects decimal string)
    return num.toFixed(scale);
  }

  function format(num, scale){
    if(num === '' || num == null) return '';
    const n = Number(num);
    if(Number.isNaN(n)) return '';
    return n.toLocaleString(locale,{ minimumFractionDigits: scale, maximumFractionDigits: scale });
  }

  moneyInputs.forEach(input => {
    const scale = Number(input.getAttribute('data-scale')||'2');
    const hiddenRaw = document.getElementById(input.id + '_raw');

    // initialize formatting if value present
    if(input.value){
      const numeric = parseToNumber(input.value, scale);
      if(hiddenRaw) hiddenRaw.value = numeric; // numeric string with dot decimal
      input.value = format(numeric, scale); // formatted for display
    }

    input.addEventListener('focus', () => {
      // show raw (unformatted) while editing for easier caret movement
      if(hiddenRaw && hiddenRaw.value){
        input.value = hiddenRaw.value.replace('.', ',');
        setTimeout(()=>{ input.select(); },0);
      }
    });

    input.addEventListener('blur', () => {
      const numeric = parseToNumber(input.value, scale);
      if(hiddenRaw) hiddenRaw.value = numeric;
      input.value = format(numeric, scale);
    });

    input.addEventListener('input', () => {
      const numeric = parseToNumber(input.value, scale);
      if(hiddenRaw) hiddenRaw.value = numeric;
    });
  });
})();
