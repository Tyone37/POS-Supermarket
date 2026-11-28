const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('src/main/resources/static/js/invoice-print.js', 'utf8');

function setup(print = () => {}, ready = Promise.resolve(), hasButton = true) {
  const handlers = new Map();
  const status = { textContent: '' };
  const button = { disabled: false, addEventListener: (type, handler) => handlers.set(type, handler) };
  const context = {
    document: {
      querySelector: selector => selector === '[data-print-invoice]' ? (hasButton ? button : null) : status,
      fonts: { ready }
    },
    window: { print, addEventListener: (type, handler) => handlers.set(type, handler) },
    fetch: () => assert.fail('Printing must never send a write request')
  };
  vm.runInNewContext(source, context);
  return { handlers, status, button, context };
}

test('opens native print dialog only after fonts are ready', async () => {
  let resolve;
  const ready = new Promise(done => { resolve = done; });
  let calls = 0;
  const ui = setup(() => { calls++; }, ready);
  const first = ui.handlers.get('click')();
  assert.equal(ui.button.disabled, true);
  assert.equal(calls, 0);
  await ui.handlers.get('click')();
  resolve();
  await first;
  assert.equal(calls, 1);
  assert.equal(ui.button.disabled, false);
});

test('cancel or return from dialog leaves printing available again', async () => {
  let calls = 0;
  const ui = setup(() => { calls++; });
  await ui.handlers.get('click')();
  ui.handlers.get('afterprint')();
  assert.match(ui.status.textContent, /in lại/);
  assert.equal(ui.button.disabled, false);
  await ui.handlers.get('click')();
  assert.equal(calls, 2);
});

test('print exception shows retry message and enables the button', async () => {
  const ui = setup(() => { throw new Error('Printer error'); });
  await ui.handlers.get('click')();
  assert.match(ui.status.textContent, /Không mở được/);
  assert.equal(ui.button.disabled, false);
});

test('missing print API is handled without changing invoice state', async () => {
  const ui = setup();
  ui.context.window.print = undefined;
  await ui.handlers.get('click')();
  assert.match(ui.status.textContent, /Không mở được/);
  assert.equal(ui.button.disabled, false);
});

test('font loading failure allows retry', async () => {
  const ui = setup(() => assert.fail('Must wait for fonts'), Promise.reject(new Error('Font error')));
  await ui.handlers.get('click')();
  assert.equal(ui.button.disabled, false);
  assert.match(ui.status.textContent, /Không mở được/);
});

test('script safely loads without a print button', () => {
  const ui = setup(() => {}, Promise.resolve(), false);
  assert.equal(ui.handlers.has('click'), false);
});
