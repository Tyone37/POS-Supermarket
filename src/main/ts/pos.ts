const cashForm = document.querySelector<HTMLFormElement>("#cash-form");
const cashInput = document.querySelector<HTMLInputElement>("#cashReceived");
const preview = document.querySelector<HTMLOutputElement>("#change-preview");
if (cashForm && cashInput && preview) {
  const total = BigInt((cashForm.dataset.total ?? "0").split(".")[0]);
  const update = (): void => {
    const input = cashInput.value.trim();
    if (!/^\d{1,16}$/.test(input)) {
      preview.textContent = "Nhập số tiền nguyên đồng hợp lệ";
      return;
    }
    const difference = BigInt(input) - total;
    preview.textContent = difference < 0n
      ? `Còn thiếu: ${(-difference).toLocaleString("vi-VN")} đ`
      : `Tiền thừa: ${difference.toLocaleString("vi-VN")} đ`;
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
