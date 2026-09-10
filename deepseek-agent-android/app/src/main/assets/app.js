(() => {
  const $ = (s) => document.querySelector(s);
  const messagesEl = $('#messages');
  const emptyEl = $('#empty');
  const composer = $('#composer');
  const input = $('#input');
  const send = $('#send');
  const badge = $('#badge');
  const modal = $('#settingsModal');
  const apiKeyInput = $('#apiKey');
  const settingsError = $('#settingsError');
  let apiKey = '';
  let history = [];
  let busy = false;
  let requestSeq = 0;
  const pending = new Map();

  function nativeAvailable(){ return typeof NativeBridge !== 'undefined'; }
  function providerLabel(key){
    try { return nativeAvailable() ? NativeBridge.detectProvider(key) : 'browser'; }
    catch { return 'unknown'; }
  }
  function updateBadge(){
    const provider = apiKey ? providerLabel(apiKey) : '';
    badge.hidden = !provider || provider === 'unknown';
    badge.textContent = provider;
  }
  function openSettings(err=''){
    apiKeyInput.value = apiKey;
    settingsError.textContent = err;
    settingsError.hidden = !err;
    modal.hidden = false;
    setTimeout(() => apiKeyInput.focus(), 60);
  }
  function closeSettings(){ modal.hidden = true; settingsError.hidden = true; }

  function ensureThread(){
    if (emptyEl && emptyEl.parentElement) emptyEl.remove();
    let thread = messagesEl.querySelector('.thread');
    if (!thread) { thread = document.createElement('div'); thread.className = 'thread'; messagesEl.appendChild(thread); }
    return thread;
  }
  function addMessage(role, text, meta=''){
    const thread = ensureThread();
    const wrap = document.createElement('div'); wrap.className = `msg ${role}`;
    const bubble = document.createElement('div'); bubble.className = 'bubble'; bubble.textContent = text;
    wrap.appendChild(bubble);
    if (meta) { const m = document.createElement('div'); m.className='meta'; m.textContent=meta; wrap.appendChild(m); }
    thread.appendChild(wrap); messagesEl.scrollTop = messagesEl.scrollHeight;
    return {wrap,bubble};
  }
  function addTyping(){
    const thread = ensureThread();
    const wrap = document.createElement('div'); wrap.className='msg assistant';
    const bubble = document.createElement('div'); bubble.className='bubble'; bubble.innerHTML='<span class="typing"><i></i><i></i><i></i></span>';
    wrap.appendChild(bubble); thread.appendChild(wrap); messagesEl.scrollTop=messagesEl.scrollHeight; return wrap;
  }
  function resize(){ input.style.height='auto'; input.style.height=Math.min(input.scrollHeight,140)+'px'; }
  function setBusy(v){ busy=v; send.disabled=v; input.disabled=v; }

  async function ask(text){
    if (busy || !text.trim()) return;
    if (!apiKey) { openSettings('Add your API key first.'); return; }
    const userText = text.trim(); input.value=''; resize();
    addMessage('user', userText);
    history.push({role:'user', content:userText});
    const typing = addTyping(); setBusy(true);
    const id = `r${Date.now()}_${++requestSeq}`;
    try {
      const result = await new Promise((resolve,reject) => {
        pending.set(id,{resolve,reject});
        if (!nativeAvailable()) return reject(new Error('Native bridge is unavailable. Install and run the Android APK.'));
        NativeBridge.sendMessage(id, apiKey, JSON.stringify(history));
        setTimeout(() => { if (pending.has(id)) { pending.delete(id); reject(new Error('The request timed out.')); } }, 100000);
      });
      typing.remove();
      if (!result.ok) throw new Error(result.text || 'Request failed');
      history.push({role:'assistant', content:result.text});
      addMessage('assistant', result.text, result.provider || providerLabel(apiKey));
    } catch (e) {
      typing.remove(); addMessage('assistant', 'Error: ' + (e.message || String(e)), 'request failed');
    } finally { setBusy(false); input.focus(); }
  }

  window.AgentNative = {
    onResult(payloadJson){
      try {
        const data = JSON.parse(payloadJson);
        const p = pending.get(data.id); if (!p) return;
        pending.delete(data.id); p.resolve(data);
      } catch(e) { console.error(e); }
    }
  };

  composer.addEventListener('submit', e => { e.preventDefault(); ask(input.value); });
  input.addEventListener('input', resize);
  input.addEventListener('keydown', e => { if (e.key==='Enter' && !e.shiftKey) { e.preventDefault(); composer.requestSubmit(); } });
  document.querySelectorAll('.chip').forEach(btn => btn.addEventListener('click',()=>ask(btn.dataset.prompt || btn.textContent)));
  $('#settingsBtn').addEventListener('click',()=>openSettings());
  $('#closeModal').addEventListener('click',closeSettings);
  modal.addEventListener('click',e=>{ if(e.target===modal) closeSettings(); });
  $('#saveKey').addEventListener('click',()=>{
    const key=apiKeyInput.value.trim();
    if(!key){ settingsError.textContent='Paste an API key.'; settingsError.hidden=false; return; }
    const provider=providerLabel(key);
    if(provider==='unknown'){ settingsError.textContent='Key format not recognized.'; settingsError.hidden=false; return; }
    apiKey=key; if(nativeAvailable()) NativeBridge.saveKey(key); updateBadge(); closeSettings();
  });
  $('#clearKey').addEventListener('click',()=>{ apiKey=''; apiKeyInput.value=''; if(nativeAvailable()) NativeBridge.clearKey(); updateBadge(); closeSettings(); });
  $('#newChat').addEventListener('click',()=>{ history=[]; location.reload(); });

  try { apiKey = nativeAvailable() ? (NativeBridge.getStoredKey() || '') : ''; } catch { apiKey=''; }
  updateBadge(); resize();
  if(!apiKey) setTimeout(()=>openSettings(),250);
})();
