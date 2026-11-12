// Use the existing server search while retaining the input, cart and page shell.
(() => {
  const form = document.querySelector<HTMLFormElement>('[data-product-search]');
  const input = form?.querySelector<HTMLInputElement>('input[name="q"]');
  const results = document.querySelector<HTMLElement>('#product-results');
  const status = document.querySelector<HTMLElement>('#product-search-status');
  if (!form || !input || !results || !status) return;
  const tabs = form.querySelector<HTMLElement>('.category-tabs');
  const toggle = form.querySelector<HTMLButtonElement>('.category-toggle');
  const icons: Record<string, [string, string]> = {
    ALL: ['blue', 'M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3zM14 14h7v7h-7z'],
    UNCLASSIFIED: ['violet', 'M4 7h16v14H4zM3 3h18v4H3zM9 11h6'],
    DAIRY: ['blue', 'M8 3h8v4l3 4v10H5V11l3-4zM8 7h8M5 12h14'],
    DRINK: ['blue', 'M7 8h10l-1 13H8zM12 8V3l5-1'],
    VEGETABLE: ['green', 'M20 4C8 2 3 8 5 15s15 4 15-11ZM5 20 16 9'],
    FRUIT: ['rose', 'M12 7C3 2 1 14 7 20c2 2 3 0 5 0s3 2 5 0c6-6 4-18-5-13ZM12 7V3l4-1'],
    MEAT: ['rose', 'M15 3c8 0 8 9 2 13l-5 3-7-7 3-5c2-3 4-4 7-4ZM5 12l-3 4 6 6 4-3'],
    SEAFOOD: ['blue', 'M4 12c5-9 13-9 17 0-4 9-12 9-17 0ZM4 12 1 7v10zM16 10v.1'],
    FROZEN: ['blue', 'M12 2v20M3 7l18 10M3 17 21 7M9 4l3 3 3-3M9 20l3-3 3 3'],
    SNACK: ['amber', 'M7 3h10l3 5-2 13H6L4 8zM4 8h16M9 13h6M9 17h4'],
    BAKERY: ['amber', 'M5 10C0 8 4 2 8 4c2-4 6-4 8 0 4-2 8 4 3 6v10H5zM9 12v4M15 12v4'],
    PERSONAL_CARE: ['violet', 'M8 8h8l2 13H6zM10 8V4h7M14 4V2'],
    CLEANING: ['green', 'M8 9h9l3 12H5zM9 9V5h8V3H9M13 13v4'],
    HOUSEHOLD: ['amber', 'M3 11 12 3l9 8M5 10v11h14V10M9 21v-7h6v7'],
    BABY: ['rose', 'M9 3h6v4H9zM9 7l-3 4v10h12V11l-3-4M6 13h12'],
    PET: ['amber', 'M8 14c-6 6 1 9 4 6 3 3 10 0 4-6-2-3-6-3-8 0ZM4 6v3M9 3v3M15 3v3M20 6v3'],
    STATIONERY: ['violet', 'm4 16 12-12 4 4L8 20l-5 1zM13 7l4 4'],
  };
  form.querySelectorAll<HTMLButtonElement>('[data-category-code]').forEach(button => {
    const [tone, path] = icons[button.dataset.categoryCode ?? ''] ?? ['green', 'M3 7l9-4 9 4v10l-9 4-9-4zM3 7l9 4 9-4M12 11v10'];
    button.dataset.tone = tone;
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('viewBox', '0 0 24 24'); svg.setAttribute('class', 'category-icon');
    svg.setAttribute('fill', 'none'); svg.setAttribute('stroke', 'currentColor');
    svg.setAttribute('stroke-width', '1.7'); svg.setAttribute('stroke-linecap', 'round');
    svg.setAttribute('stroke-linejoin', 'round'); svg.setAttribute('aria-hidden', 'true');
    const shape = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    shape.setAttribute('d', path); svg.append(shape); button.prepend(svg);
  });
  if (toggle && tabs) {
    toggle.hidden = false;
    toggle.addEventListener('click', () => {
      const expanded = toggle.getAttribute('aria-expanded') !== 'true';
      toggle.setAttribute('aria-expanded', String(expanded));
      tabs.classList.toggle('is-expanded', expanded);
      toggle.querySelector('span')!.textContent = expanded ? 'Thu gọn' : 'Xem toàn bộ';
    });
    tabs.querySelector<HTMLElement>('.active')?.scrollIntoView({block: 'nearest', inline: 'nearest'});
  }
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
        button.setAttribute('aria-pressed', String(button.value === category));
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
