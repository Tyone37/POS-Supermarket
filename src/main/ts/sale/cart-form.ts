const removeDialog = document.querySelector<HTMLDialogElement>('#remove-dialog');
const cartContent = document.querySelector<HTMLElement>('main');
let pendingRemoveForm: HTMLFormElement | null = null;
let pendingQuantity: HTMLInputElement | null = null;
let cartSubmitting = false;

function setCartSubmitting(submitting: boolean): void {
  cartSubmitting = submitting;
  if (cartContent) {
    cartContent.inert = submitting;
  }
}

function openRemoveDialog(formId: string, input: HTMLInputElement | null): void {
  if (cartSubmitting) {
    return;
  }
  const target = document.getElementById(formId);
  if (!(target instanceof HTMLFormElement) || !removeDialog) {
    return;
  }
  pendingRemoveForm = target;
  pendingQuantity = input;
  removeDialog.showModal();
}

document.querySelectorAll<HTMLButtonElement>('[data-remove]').forEach((button) => {
  button.addEventListener('click', () => openRemoveDialog(button.dataset.remove ?? '', null));
});

document.querySelectorAll<HTMLFormElement>('[data-quantity-form]').forEach((quantityForm) => {
  const input = quantityForm.querySelector<HTMLInputElement>('input[name="quantity"]');
  if (!input) {
    return;
  }
  quantityForm.querySelectorAll<HTMLButtonElement>('[data-step]').forEach((button) => {
    button.addEventListener('click', () => {
      if (cartSubmitting) {
        return;
      }
      const match = /^(\d+)(?:\.(\d{1,3}))?$/.exec(input.value.trim());
      if (!match) {
        return;
      }
      const scaled = BigInt(match[1]) * 1000n + BigInt((match[2] ?? '').padEnd(3, '0'));
      const next = scaled + (button.dataset.step === '1' ? 1000n : -1000n);
      if (next < 0n) {
        return;
      }
      const fraction = (next % 1000n).toString().padStart(3, '0').replace(/0+$/, '');
      input.value = `${next / 1000n}${fraction ? `.${fraction}` : ''}`;
      quantityForm.requestSubmit();
    });
  });
  quantityForm.addEventListener('submit', (event) => {
    if (/^0+(?:\.0{1,3})?$/.test(input.value.trim())) {
      event.preventDefault();
      openRemoveDialog(quantityForm.dataset.removeForm ?? '', input);
    }
  });
});

document.querySelectorAll<HTMLFormElement>('form[method="post"]').forEach((form) => {
  form.addEventListener('submit', (event) => {
    if (event.defaultPrevented) {
      return;
    }
    if (cartSubmitting) {
      event.preventDefault();
      return;
    }
    setCartSubmitting(true);
  });
});
window.addEventListener('pageshow', () => setCartSubmitting(false));

removeDialog?.querySelector<HTMLButtonElement>('[data-remove-cancel]')?.addEventListener('click', () => removeDialog.close());
removeDialog?.querySelector<HTMLButtonElement>('[data-remove-confirm]')?.addEventListener('click', () => {
  const target = pendingRemoveForm;
  pendingQuantity = null;
  removeDialog.close();
  target?.requestSubmit();
});
removeDialog?.addEventListener('close', () => {
  if (pendingQuantity) {
    pendingQuantity.value = pendingQuantity.dataset.originalValue ?? '';
  }
  pendingQuantity = null;
  pendingRemoveForm = null;
});
