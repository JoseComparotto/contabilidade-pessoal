// Simple currency formatting enhancer for inputs with data-enhance="money"
// Assumes locale pt-BR by default. Keeps a hidden raw numeric value for server side if needed.
(function(){
  const locale = 'pt-BR';
  const moneyInputs = document.querySelectorAll('input[data-enhance="money"]');
  if(!moneyInputs.length) return;

  function parseToNumber(str, scale){
    if(!str) return '';
    let s = str.trim();
    if(!s) return '';
    // Keep only digits, separators
    s = s.replace(/[^0-9.,]/g,'');
    if(!s) return '';
    // Determine decimal separator (last occurring separator among , and .)
    const lastComma = s.lastIndexOf(',');
    const lastDot = s.lastIndexOf('.');
    let decimalSep = null;
    if(lastComma === -1 && lastDot === -1){
      // integer only
    } else if(lastComma === -1){
      decimalSep = '.'; // only dot
    } else if(lastDot === -1){
      decimalSep = ','; // only comma
    } else {
      decimalSep = lastComma > lastDot ? ',' : '.'; // whichever appears later is decimal
    }

    if(decimalSep){
      const thousandSep = decimalSep === ',' ? '.' : ',';
      // Remove thousand separators
      const parts = s.split(decimalSep);
      let intPart = parts[0].replace(new RegExp('\\' + thousandSep, 'g'), '');
  // intPart now free of thousand separators
      let fracPart = parts.slice(1).join('').replace(new RegExp('[^0-9]', 'g'), '');
      s = intPart + '.' + fracPart;
    } else {
      // remove any stray separators (treat as thousands) since no decimal part
      s = s.replace(/[.,]/g,'');
    }

    if(s === '' || s === '.') return '';
    const num = Number(s);
    if(Number.isNaN(num)) return '';
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
