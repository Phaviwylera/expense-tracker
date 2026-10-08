// Tests the actual Android WebView through local ADB, not a simulated browser DOM.
const fs=require('node:fs'),assert=require('node:assert/strict');
const deadline=setTimeout(()=>{console.error('WebView test timed out');process.exit(1);},30000);deadline.unref();
(async()=>{
 const pages=await (await fetch('http://127.0.0.1:9222/json')).json();
 const page=pages.find(p=>p.url.includes('android_asset/index.html'));
 assert(page,'Android WebView page must load');
 const ws=new WebSocket(page.webSocketDebuggerUrl);await new Promise((resolve,reject)=>{ws.onopen=resolve;ws.onerror=reject;});
 let id=0;const pending=new Map();ws.onmessage=e=>{const r=JSON.parse(e.data);if(r.id&&pending.has(r.id)){pending.get(r.id)(r);pending.delete(r.id);}};
 async function evaluate(expression){const reply=await new Promise(resolve=>{const n=++id;pending.set(n,resolve);setTimeout(()=>{if(pending.has(n)){console.error('Stalled evaluation:',expression);process.exit(1);}},8000).unref();ws.send(JSON.stringify({id:n,method:'Runtime.evaluate',params:{expression,returnByValue:true,awaitPromise:true}}));});if(reply.result.exceptionDetails)throw new Error(JSON.stringify(reply.result.exceptionDetails));return reply.result.result.value;}
 assert.equal(await evaluate("document.getElementById('expense').textContent.replace(/\\s/g,'')"),'₹0.00');
 assert.equal(await evaluate("document.getElementById('period').options.length>0"),true);
 await evaluate('clearInterval(liveRefreshTimer)'); // Keep isolated UI fixtures separate from live database refreshes during assertions.
 const fixture={smsGranted:true,transactions:[{id:'ui-fixture-only',time:Date.now(),bank:'SBI',account:'TEST',amount:50000,direction:'debit',merchant:'UI fixture (not a bank entry)',category:'Food',status:'confirmed',raw:'Synthetic UI fixture, never persisted'}],plans:[]};
 await evaluate(`window.nativeEvent('snapshot',${JSON.stringify(JSON.stringify(fixture))})`);
 await evaluate('new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)))');
 assert.equal(await evaluate("document.getElementById('expense').textContent.replace(/\\s/g,'')"),'₹500.00');
 assert.equal(await evaluate("document.getElementById('legend').children.length"),1);
 assert.equal(await evaluate("Array.from(document.getElementById('bank').options).some(o=>o.value==='SBI')"),true);
 console.log('WebView initial snapshot and totals verified.');
 fs.writeFileSync('smoke-output/premium-overview.png',require('node:child_process').execFileSync('adb',['exec-out','screencap','-p']));
 assert.equal(await evaluate("document.documentElement.scrollWidth<=window.innerWidth"),true);
 await evaluate("edit(state.transactions[0])");
 assert.equal(await evaluate("document.getElementById('editBank').value"),'SBI');
 await evaluate("document.getElementById('editDialog').close()");
 assert.equal(await evaluate("trendPoints.length>0"),true);
 await evaluate("document.getElementById('trendMode').value='cumulative';document.getElementById('trendMode').onchange()");
 await evaluate('new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)))');
 assert.equal(await evaluate("document.getElementById('trendSubtitle').textContent"),'Cumulative outflow · daily points');
 // Regression: repeated redraws at Android pixel density must never grow canvas CSS height.
 const heights=await evaluate("['trend','pie','bars'].map(id=>document.getElementById(id).getBoundingClientRect().height)");
 await evaluate("for(let i=0;i<12;i++){lastChartKey='';charts();adjustCamera('trend','in');adjustCamera('pie','in');adjustCamera('bars','in');}");
 assert.deepEqual(await evaluate("['trend','pie','bars'].map(id=>document.getElementById(id).getBoundingClientRect().height)"),heights);
 await evaluate("['trend','pie','bars'].forEach(id=>adjustCamera(id,'reset'))");
 assert.equal(await evaluate("document.getElementById('legend').querySelector('.categoryFill span').style.width"),'100%');
 await evaluate("window.testRow=document.getElementById('recent').firstChild");
 await evaluate(`window.nativeEvent('snapshot',${JSON.stringify(JSON.stringify(fixture))})`);
 assert.equal(await evaluate("window.testRow===document.getElementById('recent').firstChild"),true);
 fixture.transactions[0].category='Uncategorised';await evaluate(`window.nativeEvent('snapshot',${JSON.stringify(JSON.stringify(fixture))})`);
 await evaluate("document.getElementById('txTag').value='untagged';renderTransactions()");
 assert.equal(await evaluate("document.getElementById('txList').querySelectorAll('.tx').length"),1);
 fixture.transactions[0].category='Food';await evaluate(`window.nativeEvent('snapshot',${JSON.stringify(JSON.stringify(fixture))})`);
 assert.equal(await evaluate("document.getElementById('txList').querySelectorAll('.tx').length"),0);
 assert.equal(await evaluate("document.getElementById('categoryBars').querySelector('.categoryHeading strong').textContent"),'Food');
 await evaluate("document.getElementById('txTag').value='tagged';renderTransactions()");
 assert.equal(await evaluate("document.getElementById('txList').querySelectorAll('.tx').length"),1);
 // Temporal zoom changes bucket sizes and date range, never canvas geometry.
 await evaluate("setTimeRange('year')");assert.equal(await evaluate("timeline.unit"),'month');
 await evaluate("setTimeRange('day')");assert.equal(await evaluate("timeline.unit"),'hour');
 await evaluate("adjustCamera('trend','out');adjustCamera('trend','out')");assert.equal(await evaluate("timeline.unit"),'day');
 assert.deepEqual(await evaluate("['trend','pie','bars'].map(id=>document.getElementById(id).getBoundingClientRect().height)"),heights);
 await evaluate("showPage('plans');document.getElementById('newPlan').click()");
 assert.equal(await evaluate("document.getElementById('planFlexible').checked"),true);
 assert.equal(await evaluate("document.getElementById('planDate').required"),false);
 await evaluate("document.getElementById('planFlexible').checked=false;planTiming()");assert.equal(await evaluate("document.getElementById('planDate').required"),true);
 await evaluate("document.getElementById('planDialog').close();showPage('overview')");
 await evaluate("Ledger.scanHistory()");await new Promise(resolve=>setTimeout(resolve,1200));
 assert.equal(await evaluate("document.getElementById('scanStats').textContent.includes('messages checked')"),true);
 assert.equal(await evaluate("document.getElementById('scan').disabled"),false);
 await evaluate("showPage('plans')");
 assert.equal(await evaluate("document.getElementById('plans').classList.contains('hidden')"),false);
 await evaluate("showPage('overview');Ledger.refreshData()");
 await new Promise(resolve=>setTimeout(resolve,1500));
 assert.equal(await evaluate("document.getElementById('expense').textContent.replace(/\\s/g,'')"),'₹0.00');
 fs.writeFileSync('smoke-output/browser-checks.txt','PASS actual Android WebView boot, month picker, fixture totals, category chart, daily/cumulative trend, plans navigation and fixture cleanup, bounded chart zoom/redraw, stable unchanged snapshots, category bars, untagged-to-tagged movement and scan completion. No SMS or database records fabricated.\n');
 ws.close();console.log('Actual Android WebView dashboard checks passed.');
})().catch(e=>{console.error(e);process.exit(1);});
