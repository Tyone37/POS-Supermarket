const form = document.querySelector<HTMLFormElement>("#create-product");
const submitButton = form?.querySelector<HTMLButtonElement>('button[type="submit"]');

if (form && submitButton) {
  const label = submitButton.textContent;
  form.addEventListener("submit", (event) => {
    if (submitButton.disabled) {
      event.preventDefault();
      return;
    }
    submitButton.disabled = true;
    submitButton.textContent = "Đang lưu…";
  });
  window.addEventListener("pageshow", () => {
    submitButton.disabled = false;
    submitButton.textContent = label;
  });
  form.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus();
}
