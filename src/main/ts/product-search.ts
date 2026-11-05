// Use the existing server search while retaining the input, cart and page shell.
(() => {
  const form = document.querySelector<HTMLFormElement>('[data-product-search]');
  const input = form?.querySelector<HTMLInputElement>('input[name="q"]');
  const results = document.querySelector<HTMLElement>('#product-results');
  const status = document.querySelector<HTMLElement>('#product-search-status');
  if (!form || !input || !results || !status) return;
  let category = form.querySelector<HTMLButtonElement>('.chip.active')?.value ?? '';
  let timer: ReturnType<typeof setTimeout> | undefined;
  let request: AbortController | undefined;
  let revision = 0;

  const invalidate = (): number => {
    clearTimeout(timer);
    request?.abort();
    return ++revision;
  };
  const search = async (url: URL, current: number): Promise<void> => {
    request = new AbortController();
    results.setAttribute('aria-busy', 'true');
    status.textContent = 'Đang tìm sản phẩm…';
    try {
      const response = await fetch(url, {signal: request.signal, credentials: 'same-origin'});
      if (current !== revision) return;
      if (response.redirected && new URL(response.url).pathname !== url.pathname) {
        window.location.assign(response.url);
        return;
      }
      if (!response.ok) throw new Error('Search failed');
      const page = new DOMParser().parseFromString(await response.text(), 'text/html');
      if (current !== revision) return;
      const next = page.querySelector('#product-results');
      if (!next) throw new Error('Missing results');
      results.replaceChildren(...Array.from(next.childNodes));
      decorateProductImages(results);
      const count = results.querySelectorAll('.product-card').length;
      status.textContent = count ? `Đang hiển thị ${count} sản phẩm phù hợp.` : 'Không tìm thấy sản phẩm phù hợp.';
      form.querySelectorAll<HTMLButtonElement>('.chip').forEach(button => {
        button.classList.toggle('active', button.value === category);
      });
      history.replaceState(null, '', url);
    } catch (error) {
      if (current === revision && !(error instanceof DOMException && error.name === 'AbortError')) {
        status.textContent = 'Chưa tải được gợi ý. Nhấn Tìm để thử lại.';
      }
    } finally {
      if (current === revision) results.removeAttribute('aria-busy');
    }
  };
  const queryUrl = (): URL => {
    const url = new URL(form.action);
    if (input.value.trim()) url.searchParams.set('q', input.value.trim());
    if (category) url.searchParams.set('category', category);
    return url;
  };
  input.addEventListener('input', event => {
    const current = invalidate();
    if ((event as InputEvent).isComposing) return;
    timer = setTimeout(() => void search(queryUrl(), current), 220);
  });
  input.addEventListener('compositionend', () => {
    const current = invalidate();
    timer = setTimeout(() => void search(queryUrl(), current), 220);
  });
  form.addEventListener('submit', event => {
    event.preventDefault();
    const button = (event as SubmitEvent).submitter;
    if (button instanceof HTMLButtonElement && button.name === 'category') category = button.value;
    void search(queryUrl(), invalidate());
  });
  results.addEventListener('click', event => {
    const link = (event.target as Element).closest<HTMLAnchorElement>('nav a');
    if (!link || (event as MouseEvent).ctrlKey || (event as MouseEvent).metaKey || (event as MouseEvent).shiftKey) return;
    event.preventDefault();
    void search(new URL(link.href), invalidate());
  });
})();
