const invoicePrintButton = document.querySelector<HTMLButtonElement>('[data-print-invoice]');
const invoicePrintStatus = document.querySelector<HTMLElement>('[data-print-status]');

const showInvoicePrintStatus = (message: string): void => {
  if (invoicePrintStatus) {
    invoicePrintStatus.textContent = message;
  }
};

invoicePrintButton?.addEventListener('click', async () => {
  if (invoicePrintButton.disabled) {
    return;
  }
  invoicePrintButton.disabled = true;
  showInvoicePrintStatus('Đang mở hộp thoại in…');
  try {
    await document.fonts.ready;
    if (typeof window.print !== 'function') {
      throw new Error('Printing unavailable');
    }
    window.print();
    showInvoicePrintStatus('Nếu đã đóng hoặc hủy hộp thoại in, bạn có thể in lại từ trang này.');
  } catch {
    showInvoicePrintStatus('Không mở được hộp thoại in. Vui lòng thử lại hoặc dùng chức năng in của trình duyệt.');
  } finally {
    invoicePrintButton.disabled = false;
  }
});

window.addEventListener('afterprint', () => {
  showInvoicePrintStatus('Đã đóng hộp thoại in. Bạn có thể in lại nếu cần.');
});
