/** Progressive enhancement: retain server validation/CSRF and patch only the cart. */
(() => {
  const root = document.querySelector<HTMLElement>('main[data-cart]');
  if (!root) return;
  let pending = false;
  let needsReload = false;
  const notice = document.createElement('div');
  notice.dataset.cartNotice = ''; notice.hidden = true; notice.setAttribute('aria-live', 'polite');
  root.querySelector('.pagehead')?.after(notice);
  const message = (text: string): void => {
    notice.textContent = text; notice.className = 'notice danger'; notice.hidden = false;
  };
  const patchCart = (html: string): boolean => {
    const page = new DOMParser().parseFromString(html, 'text/html');
    const next = page.querySelector<HTMLElement>('main[data-cart] .cart');
    const current = root.querySelector<HTMLElement>('.cart');
    if (!next || !current) return false;
    const scrollTop = current.querySelector('.cart-items')?.scrollTop ?? 0;
    current.replaceChildren(...Array.from(next.childNodes));
    const items = current.querySelector('.cart-items');
    if (items) items.scrollTop = scrollTop;
    current.querySelectorAll<HTMLImageElement>('img[src$="product-placeholder.svg"]').forEach(image => {
      const match = Array.from(root.querySelectorAll<HTMLImageElement>('.product-art img')).find(product => product.alt === image.alt);
      if (match) image.src = match.src;
    });
    return true;
  };
  root.addEventListener('click', event => {
    if (!(event.target instanceof Element)) return;
    const button = event.target.closest<HTMLButtonElement>('[data-quantity-change]');
    if (!button) return;
    // Delegate these controls because their rows are replaced after each update.
    event.preventDefault(); event.stopImmediatePropagation();
    if (pending || needsReload) return;
    const form = button.form;
    const quantity = form?.querySelector<HTMLInputElement>('input[name="quantity"]');
    if (!form || !quantity || !quantity.checkValidity()) return;
    const steps = quantity.step === '0.001' ? 1000 : 1;
    if (button.dataset.quantityChange === '1') quantity.stepUp(steps); else quantity.stepDown(steps);
    form.requestSubmit();
  }, true);
  root.addEventListener('submit', async event => {
    const form = event.target;
    if (!(form instanceof HTMLFormElement) || !form.matches('.scan, .product-card, .quantity-form') || form.method.toLowerCase() !== 'post') return;
    event.preventDefault(); event.stopImmediatePropagation();
    if (pending) return;
    if (needsReload) { message('Chưa xác nhận được giỏ hàng. Vui lòng tải lại trang trước khi thử lại.'); return; }
    const quantity = form.querySelector<HTMLInputElement>('input[name="quantity"]');
    if (quantity && Number(quantity.value) === 0 && form.dataset.confirmUrl) {
      window.location.assign(form.dataset.confirmUrl); return;
    }
    const itemId = form.querySelector<HTMLInputElement>('input[name="itemId"]')?.value;
    const focusBefore = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const body = new FormData(form);
    pending = true; notice.hidden = true;
    root.inert = true; root.querySelector('.cart')?.setAttribute('aria-busy', 'true');
    try {
      const response = await fetch(form.action, {method:'POST', body, credentials:'same-origin', headers:{'Accept':'text/html'}});
      const html = await response.text();
      const destination = new URL(response.url);
      if (destination.origin === location.origin && destination.pathname !== location.pathname && response.ok) {
        window.location.assign(destination.href); return;
      }
      if (!response.ok) {
        const errorPage = new DOMParser().parseFromString(html, 'text/html');
        message(errorPage.querySelector('[role="alert"]')?.textContent?.trim() || 'Không thể cập nhật giỏ hàng. Vui lòng kiểm tra lại.');
        if (quantity) form.reset();
        return;
      }
      if (!patchCart(html)) throw new Error('Missing cart response');
      if (form.matches('.scan')) form.querySelector<HTMLInputElement>('#scan')!.value = '';
    } catch {
      // A POST may have succeeded even if its response was lost. Never replay it automatically.
      try {
        const recovery = await fetch(location.href, {credentials:'same-origin', headers:{'Accept':'text/html'}, cache:'no-store'});
        if (!recovery.ok || !patchCart(await recovery.text())) throw new Error('Recovery failed');
        message('Kết nối bị gián đoạn. Giỏ đã được tải lại; kiểm tra số lượng trước khi thêm tiếp.');
      } catch {
        needsReload = true;
        message('Chưa xác nhận được giỏ hàng. Vui lòng tải lại trang trước khi thử lại.');
      }
    } finally {
      pending = false; root.inert = false; root.querySelector('.cart')?.removeAttribute('aria-busy');
      if (form.matches(".scan")) root.querySelector<HTMLInputElement>("#scan")?.focus({preventScroll:true});
      else if (focusBefore?.isConnected) focusBefore.focus({preventScroll:true});
      else if (itemId) {
        const updated = Array.from(root.querySelectorAll<HTMLFormElement>('.quantity-form')).find(row => row.querySelector<HTMLInputElement>('input[name="itemId"]')?.value === itemId);
        updated?.querySelector<HTMLInputElement>('input[name="quantity"]')?.focus({preventScroll:true});
      }
    }
  }, true);
})();
