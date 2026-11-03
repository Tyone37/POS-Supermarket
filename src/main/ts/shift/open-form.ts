const openShiftForm = document.querySelector<HTMLFormElement>('#open-shift');
const openShiftButton = openShiftForm?.querySelector<HTMLButtonElement>('button[type="submit"]');

if (openShiftForm && openShiftButton) {
  const openShiftLabel = openShiftButton.textContent;
  openShiftForm.addEventListener('submit', (event) => {
    if (openShiftButton.disabled) {
      event.preventDefault();
      return;
    }
    openShiftButton.disabled = true;
    openShiftButton.textContent = 'Đang mở ca…';
  });
  window.addEventListener('pageshow', () => {
    openShiftButton.disabled = false;
    openShiftButton.textContent = openShiftLabel;
  });
  openShiftForm.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus();
}
