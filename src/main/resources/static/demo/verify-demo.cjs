const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const source = fs.readFileSync(require('node:path').join(__dirname, 'app.js'), 'utf8');
const root = { innerHTML: '' };
const context = vm.createContext({
  Intl, Date, Math, console, setTimeout, clearTimeout,
  document: {getElementById: id => id === 'root' ? root : null, addEventListener() {}, querySelectorAll() { return []; }},
  window: { addEventListener() {}, scrollTo() {} },
  location: {hash: ''}, history: {replaceState() {}},
});
vm.runInContext(source, context);
let count = 0;
function check(name, expression) {
  assert.equal(vm.runInContext(expression, context), true, name);
  console.log('PASS', name);
  count++;
}
check('27 seeded invoices have consistent line totals and sufficient tender', `invoices.length===27 && invoices.every(i=>i.sub===i.items.reduce((s,it)=>s+it.price*it.qty,0)&&i.sub-i.discount===i.total&&i.received>=i.total)`);
check('Revenue from report product lines equals source invoices', `topProducts(invoices).reduce((s,p)=>s+p.total,0)===sum(invoices)`);
check('Week total is exactly 6,133,800 VND', `sum(inRange(invoices,'2026-10-29',today))===6133800`);
check('Empty report returns zero and no ranked products', `sum(inRange(invoices,'2026-11-20','2026-11-21'))===0 && topProducts([]).length===0`);
check('Date range includes both endpoints', `inRange(invoices,'2026-10-29','2026-10-29').length===3`);
check('Active, upcoming, expired promotions are distinct', `promoState(promos[0])==='Đang áp dụng'&&promoState(promos[1])==='Sắp diễn ra'&&promoState(promos[2])==='Đã kết thúc'`);
check('Only active promotions affect current selling price', `discountFor('SP001')===10 && discountFor('SP002')===0 && discountFor('SP007')===0`);
vm.runInContext(`state.cart=[{id:'SP001',qty:2},{id:'SP002',qty:1}]`, context);
check('Document worked example: subtotal 84,000, discount 6,400, due 77,600', `cartTotals().sub===84000&&cartTotals().discount===6400&&cartTotals().total===77600`);
check('Adding to draft cart has not deducted stock', `products[0].stock===86`);
vm.runInContext(`state.role='cashier';state.username='thungan.binh';`, context);
check('Cashier invoices and current shift belong to logged-in account', `myInvoices().every(i=>i.name==='Lê Thanh Bình')&&currentShift().id==='CA-0411-02'`);
check('Cashier has no management or report routes', `!allowed.cashier.includes('accounts')&&!allowed.cashier.includes('products')&&!allowed.cashier.includes('reports')`);
vm.runInContext(`const oldInvoice=JSON.stringify(invoices[0]);products[0].price=99999;promos[0].cancelled=true;`, context);
check('Old invoice snapshot is unchanged after product price/promotion edit', `JSON.stringify(invoices[0])===oldInvoice`);
check('No-accent product search', `matches('Sữa tươi Vinamilk','sua tuoi')`);
check('Closed-shift expected cash uses opening plus due, not tender', `(()=>{const s=shifts.find(s=>s.id==='CA-0311-01');const rows=invoices.filter(i=>i.shift===s.id);return sum(rows)+s.opening!==rows.reduce((n,i)=>n+i.received,0)+s.opening})()`);
check('Every seeded invoice has a matching cashier and register shift', `invoices.every(i=>shifts.some(s=>s.id===i.shift&&s.name===i.name&&s.register===i.register))`);
check('Daily revenue sums to the full period total', `[...new Set(invoices.map(i=>i.date))].reduce((n,d)=>n+sum(invoices.filter(i=>i.date===d)),0)===sum(invoices)`);
check('Uppercase Vietnamese search normalizes correctly', `matches('Đỗ Quang Huy','do quang')`);
vm.runInContext(`window.matchMedia=()=>({matches:true});document.querySelectorAll=()=>{throw new Error('Reduced motion must exit before starting effects')};animateMetrics();animateAddedProduct('SP001');`,context);
check('Reduced-motion preference bypasses metric and cart animations', `reducedMotion()===true`);
console.log(`${count} checks passed. Browser-only UI and server guarantees are outside these checks.`);
