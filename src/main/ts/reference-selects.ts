// @ts-nocheck -- Vendored presentation code from the supplied demo.
/* Select presentation copied from the supplied UI source; native form values are retained. */
(()=>{'use strict';
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const matches=(text,query)=>String(text??'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().includes(String(query??'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase());
const reducedMotion=()=>window.matchMedia('(prefers-reduced-motion: reduce)').matches;
const icon=name=>`<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${name==='check'?'<path d="m5 12 4 4L20 5"/>':'<path d="m9 5 7 7-7 7"/>'}</svg>`;
// Progressively enhanced selects keep the original form values/change handlers.
// The visible trigger/listbox are fully custom; the native popup never opens.
let activeSelect=null;
function closeOpenSelect(immediate=false){activeSelect?.close(immediate,false)}
function animateSelection(selector){if(reducedMotion())return;document.querySelector?.(selector)?.animate?.([{transform:'scale(.97)'},{transform:'scale(1)'}],{duration:190,easing:'cubic-bezier(.22,1,.36,1)'})}
function enhanceSelects(scope=document){
 scope.querySelectorAll('select:not([data-enhanced])').forEach(native=>{
  native.dataset.enhanced='true';
  const id=native.id||'select-'+Math.random().toString(36).slice(2),role=id==='role-switch';
  const labels=[...(native.labels||[])],label=native.getAttribute('aria-label')||labels.map(l=>l.textContent.trim()).join(' ')||'Chọn một mục';
  const wrap=document.createElement('div');wrap.className='custom-select'+(role?' role-select':'');native.before(wrap);wrap.append(native);native.hidden=true;
  const trigger=document.createElement('button');trigger.type='button';trigger.id=id+'--trigger';trigger.className='select-trigger';trigger.setAttribute('role','combobox');trigger.setAttribute('aria-haspopup','listbox');trigger.setAttribute('aria-expanded','false');trigger.setAttribute('aria-label',label);trigger.setAttribute('aria-controls',id+'--list');trigger.disabled=native.disabled;
  native.addEventListener('invalid',e=>{e.preventDefault();trigger.setAttribute('aria-invalid','true');trigger.focus();let error=wrap.querySelector('.select-validation');if(!error){error=document.createElement('p');error.className='select-validation';error.setAttribute('role','alert');wrap.append(error)}error.textContent='Vui lòng chọn một mục.'});
  native.addEventListener('change',()=>{trigger.removeAttribute('aria-invalid');wrap.querySelector('.select-validation')?.remove()});
  native.form?.addEventListener('reset',()=>queueMicrotask(refresh));
  if(native.required||labels.some(l=>l.classList.contains('required')))trigger.setAttribute('aria-required','true');labels.forEach(l=>l.htmlFor=trigger.id);wrap.append(trigger);
  const menu=document.createElement('div');menu.className='select-menu'+(role?' role-menu':'');menu.setAttribute('popover','manual');wrap.append(menu);
  const details={cashier:['pos','Bán hàng và ca tại quầy'],manager:['chart','Sản phẩm, doanh thu và vận hành'],admin:['shield','Tài khoản và phân quyền']};
  let expanded=false,timer,highlight=-1;
  const refresh=()=>{trigger.innerHTML=`<span class="select-value">${esc(native.selectedOptions[0]?.textContent||'Chọn…')}</span><span class="select-chevron">${icon('chevron')}</span>`;trigger.disabled=native.disabled};refresh();
  native.addEventListener('change',refresh);
  const options=()=>[...menu.querySelectorAll('[role="option"]:not([disabled])')];
  const mark=index=>{const nodes=options();if(!nodes.length)return;highlight=Math.max(0,Math.min(nodes.length-1,index));nodes.forEach((n,i)=>n.dataset.highlighted=String(i===highlight));nodes[highlight].focus({preventScroll:true});nodes[highlight].scrollIntoView({block:'nearest'})};
  const draw=query=>{const nodes=[...native.options].filter(o=>matches(o.textContent,query));menu.querySelector('.select-options').innerHTML=nodes.map(o=>{const checked=o.value===native.value,detail=role?details[o.value]:null;return `<button type="button" role="option" aria-selected="${checked}" class="select-option ${checked?'is-selected':''}" data-value="${esc(o.value)}" tabindex="-1" ${o.disabled?'disabled':''}>${detail?`<span class="select-option-icon">${icon(detail[0])}</span>`:''}<span class="select-option-text">${esc(o.textContent)}${detail?`<small>${detail[1]}</small>`:''}</span><span class="select-tick">${checked?icon('check'):''}</span></button>`}).join('')||'<div class="select-no-results">Không có lựa chọn phù hợp.</div>';highlight=-1};
  const reposition=()=>{if(!expanded||!trigger.isConnected)return;const rect=trigger.getBoundingClientRect(),gutter=12,width=Math.min(Math.max(rect.width,role?265:205),innerWidth-gutter*2),below=innerHeight-rect.bottom-8,above=rect.top-8,up=below<190&&above>below;menu.style.width=width+'px';menu.style.maxHeight=Math.max(150,innerHeight-24)+'px';menu.dataset.direction=up?'up':'down';const list=menu.querySelector('.select-options');list.style.maxHeight=Math.max(96,Math.min(280,(up?above:below)-(menu.querySelector('.select-menu-search')?62:role?55:16)))+'px';const height=menu.getBoundingClientRect().height;menu.style.left=Math.max(gutter,Math.min(rect.left,innerWidth-width-gutter))+'px';menu.style.top=Math.max(gutter,Math.min(up?rect.top-height-7:rect.bottom+7,innerHeight-height-gutter))+'px';menu.style.transformOrigin=up?'bottom center':'top center'};

  const close=(immediate=false,restore=true)=>{if(!expanded)return;expanded=false;trigger.setAttribute('aria-expanded','false');menu.style.pointerEvents='none';if(activeSelect?.native===native)activeSelect=null;const hide=()=>{if(!menu.isConnected)return;if(menu.hidePopover){if(menu.matches(':popover-open'))menu.hidePopover()}else menu.hidden=true;menu.classList.remove('is-closing')};clearTimeout(timer);if(!immediate&&!reducedMotion()&&menu.animate){menu.animate([{opacity:1,transform:'translateY(0) scale(1)'},{opacity:0,transform:`translateY(${menu.dataset.direction==='up'?3:-3}px) scale(.98)`}],{duration:130,easing:'ease-in',fill:'forwards'});timer=setTimeout(hide,130)}else hide();if(restore&&trigger.isConnected)trigger.focus({preventScroll:true})};
  const open=(keyboard=false,last=false)=>{if(native.disabled)return;if(expanded){close();return}closeOpenSelect(true);clearTimeout(timer);menu.getAnimations?.().forEach(a=>a.cancel());menu.style.pointerEvents='auto';menu.classList.remove('is-closing');menu.innerHTML=(role?'<div class="select-menu-heading">Chọn không gian làm việc</div>':native.options.length>6?`<div class="select-menu-search">${icon('search')}<input type="search" aria-label="Tìm trong ${esc(label)}" placeholder="Tìm lựa chọn…" autocomplete="off"></div>`:'')+`<div class="select-options" role="listbox" id="${id}--list" aria-label="${esc(label)}"></div>`;draw('');expanded=true;trigger.setAttribute('aria-expanded','true');if(menu.showPopover)menu.showPopover();else menu.hidden=false;activeSelect={native,close,reposition};reposition();if(!reducedMotion())menu.animate?.([{opacity:0,transform:`translateY(${menu.dataset.direction==='up'?4:-4}px) scale(.975)`},{opacity:1,transform:'translateY(0) scale(1)'}],{duration:190,easing:'cubic-bezier(.2,.8,.2,1)'});const search=menu.querySelector('input');if(search){search.oninput=()=>{draw(search.value);reposition()};search.onkeydown=e=>{if(e.key==='ArrowDown'){e.preventDefault();mark(0)}}}if(keyboard){const selected=options().findIndex(n=>n.getAttribute('aria-selected')==='true');mark(last?options().length-1:Math.max(0,selected))}else search?.focus({preventScroll:true})};
  wrap.addEventListener('focusout',()=>queueMicrotask(()=>{if(expanded&&!wrap.contains(document.activeElement))close(true,false)}));
  trigger.onclick=()=>open();
  trigger.onkeydown=e=>{if(e.key==='ArrowDown'||e.key==='ArrowUp'){e.preventDefault();if(expanded)mark(e.key==='ArrowUp'?options().length-1:Math.max(0,options().findIndex(n=>n.getAttribute('aria-selected')==='true')));else open(true,e.key==='ArrowUp')}else if(e.key==='Enter'||e.key===' '){e.preventDefault();if(expanded)close();else open(true)}else if(e.key==='Escape'){if(expanded){e.preventDefault();e.stopPropagation();close()}}else if(e.key.length===1&&!e.ctrlKey&&!e.metaKey){if(!expanded)open();const search=menu.querySelector('input');if(search){search.value=e.key;draw(e.key);search.focus()}else{const index=options().findIndex(n=>matches(n.textContent.trim().slice(0,e.key.length),e.key));if(index>=0)mark(index)}}};

  menu.addEventListener('pointerdown',e=>{if(e.target.closest('[role="option"]'))e.preventDefault()});
  menu.onclick=e=>{const option=e.target.closest('[role="option"]');if(!option||option.disabled)return;const next=option.dataset.value,changed=next!==native.value;close(true,false);native.value=next;refresh();if(changed)native.dispatchEvent(new Event('change',{bubbles:true}));document.getElementById(trigger.id)?.focus({preventScroll:true})};
  menu.onkeydown=e=>{if(e.key==='Escape'){e.preventDefault();e.stopPropagation();close()}else if(e.key==='Tab'){close(true,true)}else if(e.target.tagName!=='INPUT'&&['ArrowDown','ArrowUp','Home','End'].includes(e.key)){e.preventDefault();mark(e.key==='Home'?0:e.key==='End'?options().length-1:highlight+(e.key==='ArrowDown'?1:-1))}};
  menu.hidden=!menu.showPopover;
 });
}
document.addEventListener('pointerdown',e=>{if(activeSelect&&!e.target.closest('.custom-select'))closeOpenSelect()});
window.addEventListener('resize',()=>activeSelect?.reposition());
window.addEventListener('scroll',()=>activeSelect?.reposition(),true);


enhanceSelects();
})();
