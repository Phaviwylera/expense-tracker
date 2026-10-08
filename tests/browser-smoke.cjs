// Tests the actual Android WebView through local ADB, not a simulated browser DOM.
const fs=require('node:fs'),assert=require('node:assert/strict');
(async()=>{
 const pages=await (await fetch('http://127.0.0.1:9222/json')).json();
 const page=pages.find(p=>p.url.includes('android_asset/index.html'));
 assert(page,'Android WebView page must load');
 const ws=new WebSocket(page.webSocketDebuggerUrl);await new Promise((resolve,reject)=>{ws.onopen=resolve;ws.onerror=reject;});
 let id=0;const pending=new Map();ws.onmessage=e=>{const r=JSON.parse(e.data);if(r.id&&pending.has(r.id)){pending.get(r.id)(r);pending.delete(r.id);}};
 async function evaluate(expression){const reply=await new Promise(resolve=>{const n=++id;pending.set(n,resolve);ws.send(JSON.stringify({id:n,method:'Runtime.evaluate',params:{expression,returnByValue:true,awaitPromise:true}}));});if(reply.result.exceptionDetails)throw new Error(JSON.stringify(reply.result.exceptionDetails));return reply.result.result.value;}
 assert.equal(await evaluate("document.getElementById('expense').textContent"),'₹0.00');
 assert.equal(await evaluate("document.getElementById('period').options.length>0"),true);
 const fixture={smsGranted:true,transactions:[{id:'ui-fixture-only',time:Date.now(),bank:'HDFC',account:'TEST',amount:50000,direction:'debit',merchant:'UI fixture (not a bank entry)',category:'Food',status:'confirmed',raw:'Synthetic UI fixture, never persisted'}],plans:[]};
 await evaluate(`window.nativeEvent('snapshot',${JSON.stringify(JSON.stringify(fixture))})`);
 await evaluate('new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)))');
 assert.equal(await evaluate("document.getElementById('expense').textContent"),'₹500.00');
 assert.equal(await evaluate("document.getElementById('legend').children.length"),1);
 assert.equal(await evaluate("trendPoints.length>0"),true);
 await evaluate("document.getElementById('trendMode').value='cumulative';document.getElementById('trendMode').onchange()");
 assert.equal(await evaluate("document.getElementById('trendSubtitle').textContent"),'Cumulative outflow · selected period');
 await evaluate("showPage('plans')");
 assert.equal(await evaluate("document.getElementById('plans').classList.contains('hidden')"),false);
 await evaluate("showPage('overview');Ledger.refreshData()");
 await new Promise(resolve=>setTimeout(resolve,1500));
 assert.equal(await evaluate("document.getElementById('expense').textContent"),'₹0.00');
 fs.writeFileSync('smoke-output/browser-checks.txt','PASS actual Android WebView boot, month picker, fixture totals, category chart, daily/cumulative trend, plans navigation and fixture cleanup. No SMS or database records fabricated.\n');
 ws.close();console.log('Actual Android WebView dashboard checks passed.');
})().catch(e=>{console.error(e);process.exit(1);});
