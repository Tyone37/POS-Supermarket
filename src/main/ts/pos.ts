const cashForm = document.querySelector<HTMLFormElement>("#cash-form");
const cashInput = document.querySelector<HTMLInputElement>("#cashReceived");
const preview = document.querySelector<HTMLOutputElement>("#change-preview");
if (cashForm && cashInput && preview) {
  const total = BigInt((cashForm.dataset.total ?? "0").split(".")[0]);
  const update = (): void => {
    const input = typeof parseMoneyInput === "function" ? parseMoneyInput(cashInput.value) : cashInput.value.trim();
    if (input === null || !/^\d{1,16}$/.test(input)) {
      preview.textContent = "";
      if (preview.parentElement) preview.parentElement.hidden = true;
      return;
    }
    if (preview.parentElement) preview.parentElement.hidden = false;
    const difference = BigInt(input) - total;
    const unit = document.createElement("span");
    unit.className = "currency-unit"; unit.textContent = "VNĐ";
    const amount = difference < 0n
      ? `Còn thiếu: ${(-difference).toLocaleString("vi-VN")}`
      : `Tiền thừa: ${difference.toLocaleString("vi-VN")}`;
    preview.replaceChildren(document.createTextNode(amount), unit);
  };
  cashInput.addEventListener("input", update);
  update();
}
document.querySelectorAll<HTMLFormElement>(".quantity-form").forEach((form) => {
  form.addEventListener("submit", (event) => {
    const quantity = form.querySelector<HTMLInputElement>('input[name="quantity"]');
    if (quantity && Number(quantity.value) === 0 && form.dataset.confirmUrl) {
      event.preventDefault();
      window.location.assign(form.dataset.confirmUrl);
    }
  });
});

const cart = document.querySelector<HTMLElement>("main[data-cart]");
let cartPending = false;
cart?.addEventListener("submit", (event) => {
  const form = event.target;
  if (!(form instanceof HTMLFormElement) || form.method.toLowerCase() !== "post" || event.defaultPrevented) return;
  if (cartPending) {
    event.preventDefault();
    return;
  }
  cartPending = true;
  cart.inert = true;
});
window.addEventListener("pageshow", () => {
  cartPending = false;
  if (cart) cart.inert = false;
  cart?.querySelectorAll<HTMLInputElement>('input[name="quantity"]').forEach((input) => {
    input.value = input.defaultValue;
  });
});
document.querySelectorAll<HTMLButtonElement>("[data-quantity-change]").forEach((button) => {
  button.addEventListener("click", () => {
    const form = button.form;
    const quantity = form?.querySelector<HTMLInputElement>('input[name="quantity"]');
    if (cartPending || !form || !quantity || !quantity.checkValidity()) return;
    const steps = quantity.step === "0.001" ? 1000 : 1;
    if (button.dataset.quantityChange === "1") quantity.stepUp(steps);
    else quantity.stepDown(steps);
    form.requestSubmit();
  });
});
