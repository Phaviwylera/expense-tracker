// Run the real dashboard code against a minimal DOM adapter; no bank data is used.
const vm=require('node:vm'),fs=require('node:fs'),assert=require('node:assert/strict');
class Element{
 constructor(){this.value='';this.children=[];this.style={};this.textContent='';this.classList={toggle(){}};this.disabled=false;this.open=false;this.dataset={};}
 append(...c){this.children.push(...c);}replaceChildren(...c){this.children=c;if(c.length&&this.isSelect)this.value=c[0].value||c[0].textContent;}
 setAttribute(){}getAttribute(){return '200';}getBoundingClientRect(){return {width:340,left:0};}getContext(){return ctx;}showModal(){this.open=true;}close(){this.open=false;}reset(){}
}
const ctx=new Proxy({createLinearGradient(){return {addColorStop(){}};}},{get(o,k){return k in o?o[k]:()=>{};},set(o,k,v){o[k]=v;return true;}}),elements={};
const get=id=>elements[id]||(elements[id]=new Element());['period','bank','trendMode','txStatus','txDirection','editCategory'].forEach(id=>get(id).isSelect=true);get('bank').value='all';get('trendMode').value='daily';get('txStatus').value='all';get('txDirection').value='all';get('txTag').value='all';
const storage=new Map(),sandbox={console,Intl,Date,JSON,Math,Number,String,Object,Array,Set,devicePixelRatio:1,setTimeout:()=>0,setInterval:()=>0,clearTimeout(){},requestAnimationFrame:f=>f(),confirm:()=>true,localStorage:{getItem:k=>storage.get(k)||null,setItem:(k,v)=>storage.set(k,v)},document:{getElementById:get,createElement:()=>new Element(),querySelectorAll:()=>[]}};sandbox.window={addEventListener(){},scrollTo(){}};vm.createContext(sandbox);vm.runInContext(fs.readFileSync('app/src/main/assets/app.js','utf8'),sandbox);
const now=Date.now(),payload={smsGranted:true,transactions:[
{id:'1',time:now,bank:'HDFC',account:'1234',amount:50000,direction:'debit',merchant:'Cafe',category:'Food',status:'confirmed',raw:'test'},
{id:'2',time:now,bank:'KVB',account:'5678',amount:6700000,direction:'credit',merchant:'Employer',category:'Income',status:'confirmed',raw:'test'},
{id:'3',time:now,bank:'HDFC',account:'1234',amount:100000,direction:'debit',merchant:'Uncertain',category:'Other',status:'review',raw:'test'},
{id:'4',time:now,bank:'KVB',account:'5678',amount:200000,direction:'debit',merchant:'Self',category:'Transfer',status:'confirmed',raw:'test'}],plans:[]};
sandbox.window.nativeEvent('snapshot',JSON.stringify(payload));assert.equal(get('expense').textContent,'₹500.00');assert.equal(get('income').textContent,'₹67,000.00');assert.equal(get('net').textContent,'₹66,500.00');console.log('PASS confirmed totals exclude reviews and transfers');
get('bank').value='HDFC';get('bank').onchange();assert.equal(get('income').textContent,'₹0.00');assert.equal(get('expense').textContent,'₹500.00');console.log('PASS bank filter changes totals');
get('txStatus').value='review';get('txStatus').oninput();assert.equal(get('txList').children.length,1);console.log('PASS review filter');
get('available').value='20000';get('expected').value='30000';get('otherCosts').value='25000';get('horizon').value='2099-01-01';payload.plans=[{id:1,name:'Trip',amount:3500000,date:'2098-01-01'},{id:2,name:'Later',amount:100000,date:'2100-01-01'}];sandbox.window.nativeEvent('snapshot',JSON.stringify(payload));assert.equal(get('forecast').textContent,'₹10,000.00');assert.equal(get('forecastLabel').textContent,'Estimated shortfall');console.log('PASS future plan shortfall excludes outside-horizon plans');
get('available').value='';get('available').oninput();assert.equal(get('forecast').textContent,'—');console.log('PASS missing verified balance does not invent forecast');
sandbox.window.nativeEvent('snapshot',JSON.stringify({smsGranted:false,transactions:[],plans:[]}));assert.equal(get('expense').textContent,'₹0.00');assert.equal(get('status').textContent,'SMS access not enabled');console.log('PASS empty state and permission indicator');

console.log('All dashboard checks passed.');

payload.transactions.push({id:'5',time:now,bank:'SBI',account:'9999',amount:10000,direction:'debit',merchant:'Shop',category:'Shopping',status:'confirmed',raw:'test'});sandbox.window.nativeEvent('snapshot',JSON.stringify(payload));assert(get('bank').children.some(o=>o.value==='SBI'));get('bank').value='SBI';get('bank').onchange();assert.equal(get('expense').textContent,'₹100.00');console.log('PASS dynamically detected bank option and totals');
get('bank').value='No-longer-present';sandbox.window.nativeEvent('snapshot',JSON.stringify(payload));assert.equal(get('bank').value,'all');console.log('PASS missing bank filter resets safely');

get('txStatus').value='all';payload.transactions.push({id:'6',time:now,bank:'SBI',account:'9999',amount:25000,direction:'debit',merchant:'Unknown shop',category:'Uncategorised',status:'confirmed',raw:'test'});sandbox.window.nativeEvent('snapshot',JSON.stringify(payload));get('txTag').value='untagged';get('txTag').oninput();assert.equal(get('txList').children.length,1);assert.equal(get('tagCounts').textContent,'1 untagged · 5 tagged');payload.transactions[5].category='Food';sandbox.window.nativeEvent('snapshot',JSON.stringify(payload));assert.equal(get('txList').children[0].textContent,'No transactions match these filters.');get('txTag').value='tagged';get('txTag').oninput();assert.equal(get('txList').children.length,6);console.log('PASS manual category moves transaction from untagged to tagged');
