/** Parse Vietnamese whole-dong input without floating-point rounding. */
function parseMoneyInput(value: string): string | null {
  const text = value.trim().toLowerCase().replace(/\s+/g, '').normalize('NFD').replace(/[\u0300-\u036f]/g, '');
  if (!text || text.length > 40) return null;
  const million = text.match(/^(.*?)(?:tr|trieu)$/);
  if (million) {
    let base = million[1];
    if (/^\d{1,3}(?:\.\d{3})+$/.test(base)) base = base.replace(/\./g, '');
    const parts = base.match(/^(\d+)(?:[.,](\d{1,6}))?$/);
    if (!parts) return null;
    return (BigInt(parts[1]) * 1000000n + BigInt((parts[2] ?? '').padEnd(6, '0') || '0')).toString();
  }
  if (!/^\d+$/.test(text) && !/^\d{1,3}(?:\.\d{3})+$/.test(text)) return null;
  return BigInt(text.replace(/\./g, '')).toString();
}

(() => {
  document.querySelectorAll<HTMLInputElement>('input[name="price"],input[name="openingCash"],input[name="received"]').forEach(input => {
    const minimum = BigInt(input.min || '0');
    const maximum = BigInt(input.max || '9999999999999999');
    input.type = 'text'; input.inputMode = 'text'; input.autocomplete = 'off'; input.maxLength = 40;
    input.classList.add('money-input');
    let previousFormatted = '';
    const update = (event?: Event): void => {
      let raw = input.value;
      const position = input.selectionStart ?? raw.length;
      const digitsBefore = raw.slice(0, position).replace(/\D/g, '').length;
      if (event instanceof InputEvent && previousFormatted.includes('.') && /^[\d.]+$/.test(raw)
          && (/^\d+$/.test(event.data ?? '') || event.inputType.startsWith('delete'))) raw = raw.replace(/\./g, '');
      const amount = parseMoneyInput(raw);
      input.setCustomValidity(amount === null
        ? (raw || input.required ? 'Nhập số tiền nguyên, ví dụ 1.000 hoặc 1tr.' : '')
        : BigInt(amount) < minimum ? `Số tiền phải từ ${minimum.toLocaleString('vi-VN')} VNĐ.`
        : BigInt(amount) > maximum ? 'Số tiền vượt quá giới hạn cho phép.' : '');
      if (amount === null) return;
      const formatted = BigInt(amount).toLocaleString('vi-VN');
      input.value = formatted;
      previousFormatted = formatted;
      if (document.activeElement === input) {
        let caret = formatted.length;
        if (!/[a-z]/i.test(raw)) {
          let seen = 0; caret = 0;
          while (caret < formatted.length && seen < digitsBefore) { if (/\d/.test(formatted[caret])) seen++; caret++; }
        }
        input.setSelectionRange(caret, caret);
      }
    };
    update();
    input.addEventListener('input', update);
    input.addEventListener('blur', () => update());
    input.form?.addEventListener('reset', () => queueMicrotask(() => update()));
    input.form?.addEventListener('formdata', event => {
      const amount = parseMoneyInput(input.value);
      (event as FormDataEvent).formData.set(input.name, amount ?? '');
    });
  });
})();
