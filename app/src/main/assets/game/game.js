(() => {
'use strict';

const canvas = document.getElementById('game');
const ctx = canvas.getContext('2d');
const touchControls = document.getElementById('touchControls');
const W = canvas.width;
const H = canvas.height;
const TAU = Math.PI * 2;

ctx.imageSmoothingEnabled = true;

const heroSide = new Image();
heroSide.src = 'assets/hero_side.png';
const heroFront = new Image();
heroFront.src = 'assets/hero_front.png';

const input = { left:false, right:false, jump:false, hammer:false, jumpPressed:false, hammerPressed:false };
const keys = new Set();
let uiButtons = [];
let lastTime = performance.now();
let accumulator = 0;
let audioCtx = null;
let audioEnabled = true;
let state = 'menu';
let stateTime = 0;
let currentLevelIndex = 0;
let level = null;
let player = null;
let camera = { x:0, targetX:0, shake:0 };
let particles = [];
let floatingTexts = [];
let cutscene = null;
let message = '';
let messageTimer = 0;
let selectedLevel = 0;

const saveKey = 'remontnik-save-v2';
let save = loadSave();

function loadSave() {
  const fallback = { unlocked:1, completed:[false,false,false], bestCoins:[0,0,0], sound:true };
  try {
    const raw = localStorage.getItem(saveKey);
    if (!raw) return fallback;
    const parsed = JSON.parse(raw);
    return {
      unlocked: Math.max(1, Math.min(3, Number(parsed.unlocked)||1)),
      completed: Array.isArray(parsed.completed) ? parsed.completed.slice(0,3).map(Boolean) : fallback.completed,
      bestCoins: Array.isArray(parsed.bestCoins) ? parsed.bestCoins.slice(0,3).map(n=>Number(n)||0) : fallback.bestCoins,
      sound: parsed.sound !== false
    };
  } catch (_) { return fallback; }
}
function storeSave() {
  try { localStorage.setItem(saveKey, JSON.stringify(save)); } catch (_) {}
}
audioEnabled = save.sound;

function beep(type='coin') {
  if (!audioEnabled) return;
  try {
    if (!audioCtx) audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    if (audioCtx.state === 'suspended') audioCtx.resume();
    const now = audioCtx.currentTime;
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.connect(gain); gain.connect(audioCtx.destination);
    let f1=440, f2=660, dur=.11, wave='sine', vol=.06;
    if (type==='jump') { f1=240; f2=460; dur=.14; wave='square'; vol=.035; }
    if (type==='coin') { f1=760; f2=1120; dur=.1; wave='triangle'; vol=.055; }
    if (type==='hammer') { f1=150; f2=80; dur=.12; wave='square'; vol=.06; }
    if (type==='hit') { f1=130; f2=45; dur=.24; wave='sawtooth'; vol=.05; }
    if (type==='checkpoint') { f1=520; f2=900; dur=.35; wave='triangle'; vol=.05; }
    if (type==='win') { f1=392; f2=988; dur=.75; wave='triangle'; vol=.06; }
    osc.type = wave;
    osc.frequency.setValueAtTime(f1, now);
    osc.frequency.exponentialRampToValueAtTime(Math.max(30,f2), now+dur);
    gain.gain.setValueAtTime(vol, now);
    gain.gain.exponentialRampToValueAtTime(.0001, now+dur);
    osc.start(now); osc.stop(now+dur+.02);
  } catch (_) {}
}

function clamp(v,a,b){ return Math.max(a,Math.min(b,v)); }
function lerp(a,b,t){ return a+(b-a)*t; }
function rects(a,b){ return a.x < b.x+b.w && a.x+a.w > b.x && a.y < b.y+b.h && a.y+a.h > b.y; }
function roundRect(c,x,y,w,h,r,fill,stroke,line=2){
  r=Math.min(r,w/2,h/2); c.beginPath(); c.roundRect(x,y,w,h,r); if(fill){c.fillStyle=fill;c.fill();} if(stroke){c.strokeStyle=stroke;c.lineWidth=line;c.stroke();}
}
function text(str,x,y,size=32,align='center',color='#fff',stroke=true){
  ctx.save(); ctx.font=`800 ${size}px Arial`; ctx.textAlign=align; ctx.textBaseline='middle';
  if(stroke){ ctx.lineWidth=Math.max(3,size*.12); ctx.strokeStyle='rgba(0,0,0,.55)'; ctx.strokeText(str,x,y); }
  ctx.fillStyle=color; ctx.fillText(str,x,y); ctx.restore();
}

function makeLevels(){
  const levels=[];
  const L=(name,subtitle,width,theme)=>({name,subtitle,width,theme,platforms:[],coins:[],enemies:[],spikes:[],crates:[],saws:[],falling:[],checkpoints:[],decor:[],goal:{x:width-430,y:480},totalCoins:0});
  const p=(l,x,y,w,h=40,type='ground',extra={})=>l.platforms.push({x,y,w,h,type,baseX:x,baseY:y,dx:0,dy:0,...extra});
  const c=(l,x,y)=>l.coins.push({x,y,taken:false,bob:Math.random()*TAU});
  const coinLine=(l,x,y,n,step=55,arc=0)=>{ for(let i=0;i<n;i++) c(l,x+i*step,y-Math.sin((i/(Math.max(1,n-1)))*Math.PI)*arc); };
  const e=(l,x,y,minX,maxX,type='muck')=>l.enemies.push({x,y,w:54,h:46,minX,maxX,dir:1,speed:type==='dog'?105:72,type,alive:true,stun:0});
  const s=(l,x,y,w=60,h=34)=>l.spikes.push({x,y,w,h});
  const b=(l,x,y,w=60,h=60)=>l.crates.push({x,y,w,h,broken:false,shake:0});
  const saw=(l,x,y,r,axis='y',range=100,speed=1.5,phase=0)=>l.saws.push({x,y,r,axis,range,speed,phase,baseX:x,baseY:y});
  const fall=(l,x,y,w=70,h=50,trigger=180)=>l.falling.push({x,y,w,h,baseY:y,vy:0,active:false,done:false,trigger});
  const cp=(l,x,y)=>l.checkpoints.push({x,y,active:false});

  // Уровень 1 — обучение и двор.
  let l=L('Старый двор','Первые доски и первая крыша',4300,'day');
  p(l,0,600,920,120); p(l,1040,600,840,120); p(l,1990,600,980,120); p(l,3100,600,1200,120);
  p(l,260,500,250,28,'brick'); p(l,650,430,180,28,'brick'); p(l,1160,510,280,28,'wood');
  p(l,1510,415,210,28,'wood'); p(l,2080,500,250,28,'brick'); p(l,2470,405,190,28,'brick');
  p(l,2770,320,170,28,'wood'); p(l,3250,500,260,28,'brick'); p(l,3650,420,240,28,'brick');
  p(l,940,500,90,24,'moving',{axis:'x',range:105,speed:1.0,phase:0});
  p(l,2960,470,120,24,'moving',{axis:'y',range:120,speed:1.25,phase:1.4});
  coinLine(l,170,535,9,60,70); coinLine(l,1090,545,8,65,55); coinLine(l,2050,545,10,70,85); coinLine(l,3160,545,12,70,70);
  coinLine(l,285,440,4,58,25); coinLine(l,1515,355,4,58,20); coinLine(l,2470,345,4,58,15); coinLine(l,3655,360,4,60,18);
  e(l,720,554,610,870); e(l,1320,554,1100,1800); e(l,2220,454,2080,2280); e(l,3380,554,3150,3550,'dog');
  s(l,1550,566,95); s(l,2650,566,110); s(l,3840,566,120);
  b(l,1750,540); b(l,2350,540); b(l,3510,540);
  cp(l,2140,520);
  l.decor=[{type:'tree',x:540},{type:'shed',x:1450},{type:'tree',x:2700},{type:'fence',x:3330}];
  l.totalCoins=l.coins.length; levels.push(l);

  // Уровень 2 — стройка, движущиеся платформы и падающие кирпичи.
  l=L('Большая стройка','Ремонтируем стены и окна',5400,'sunset');
  p(l,0,600,760,120); p(l,900,600,640,120); p(l,1680,600,820,120); p(l,2680,600,700,120); p(l,3530,600,760,120); p(l,4480,600,920,120);
  p(l,220,485,210,28,'wood'); p(l,520,380,170,28,'wood'); p(l,1030,480,190,28,'brick'); p(l,1310,380,150,28,'brick');
  p(l,1810,500,240,28,'steel'); p(l,2140,390,190,28,'steel'); p(l,2770,480,180,28,'brick'); p(l,3050,370,180,28,'wood');
  p(l,3650,500,250,28,'steel'); p(l,4030,390,180,28,'steel'); p(l,4620,500,220,28,'brick'); p(l,4930,390,210,28,'wood');
  p(l,760,500,120,24,'moving',{axis:'x',range:140,speed:1.2,phase:.7});
  p(l,1540,450,120,24,'moving',{axis:'y',range:155,speed:1.35,phase:2.2});
  p(l,2500,470,150,24,'moving',{axis:'x',range:150,speed:1.45,phase:1.1});
  p(l,3380,460,130,24,'moving',{axis:'y',range:140,speed:1.5,phase:.4});
  p(l,4290,470,170,24,'moving',{axis:'x',range:120,speed:1.7,phase:2.4});
  coinLine(l,120,540,8,65,75); coinLine(l,940,545,8,68,60); coinLine(l,1730,545,10,70,65); coinLine(l,2730,545,9,70,80); coinLine(l,3580,545,10,65,60); coinLine(l,4520,545,11,65,75);
  coinLine(l,225,425,4,58,18); coinLine(l,1035,420,4,55,15); coinLine(l,1815,440,5,55,25); coinLine(l,3655,440,5,55,20); coinLine(l,4935,330,4,55,18);
  e(l,420,554,120,700,'dog'); e(l,1100,434,1030,1200); e(l,1860,554,1720,2450); e(l,2860,554,2720,3320,'dog'); e(l,3750,454,3650,3870); e(l,4700,554,4520,5250,'dog');
  s(l,680,566,80); s(l,1210,566,130); s(l,2310,566,120); s(l,3180,566,110); s(l,4100,566,120); s(l,4870,566,90);
  b(l,1450,540); b(l,2030,540); b(l,2930,540); b(l,3970,540); b(l,5100,540);
  saw(l,1600,430,26,'y',110,1.8,.2); saw(l,2600,455,28,'x',120,2.0,1.4); saw(l,4410,440,26,'y',120,2.2,2.1);
  fall(l,2360,210,75,50,220); fall(l,3300,180,80,55,230); fall(l,4190,190,70,50,210);
  cp(l,2750,520);
  l.decor=[{type:'crane',x:1250},{type:'scaffold',x:2050},{type:'crane',x:3300},{type:'scaffold',x:4550}];
  l.totalCoins=l.coins.length; levels.push(l);

  // Уровень 3 — ночь, сложные прыжки и финальный ремонт.
  l=L('Заброшенный квартал','Финальный рывок к дому',6500,'night');
  p(l,0,600,700,120); p(l,860,600,560,120); p(l,1580,600,760,120); p(l,2500,600,680,120); p(l,3340,600,560,120); p(l,4060,600,720,120); p(l,4940,600,560,120); p(l,5660,600,840,120);
  p(l,160,485,190,28,'brick'); p(l,430,370,170,28,'brick'); p(l,950,490,190,28,'steel'); p(l,1230,380,150,28,'steel');
  p(l,1670,500,210,28,'wood'); p(l,1980,390,190,28,'wood'); p(l,2600,490,180,28,'steel'); p(l,2880,365,170,28,'steel');
  p(l,3420,480,190,28,'brick'); p(l,3690,360,150,28,'brick'); p(l,4150,500,220,28,'wood'); p(l,4470,380,180,28,'wood');
  p(l,5030,480,190,28,'steel'); p(l,5300,360,160,28,'steel'); p(l,5740,500,210,28,'brick'); p(l,6060,380,190,28,'brick');
  p(l,700,480,150,24,'moving',{axis:'x',range:150,speed:1.6,phase:.4});
  p(l,1420,440,140,24,'moving',{axis:'y',range:150,speed:1.7,phase:1.2});
  p(l,2340,470,150,24,'moving',{axis:'x',range:160,speed:1.8,phase:2.1});
  p(l,3180,450,140,24,'moving',{axis:'y',range:160,speed:1.9,phase:.8});
  p(l,3900,470,150,24,'moving',{axis:'x',range:150,speed:2.0,phase:1.6});
  p(l,4780,450,150,24,'moving',{axis:'y',range:160,speed:2.1,phase:2.5});
  p(l,5500,470,150,24,'moving',{axis:'x',range:140,speed:2.2,phase:.3});
  coinLine(l,100,540,8,64,70); coinLine(l,900,545,8,62,60); coinLine(l,1615,545,10,65,75); coinLine(l,2540,545,9,65,80); coinLine(l,3370,545,8,62,65); coinLine(l,4100,545,10,65,70); coinLine(l,4970,545,8,62,65); coinLine(l,5690,545,11,65,80);
  coinLine(l,165,425,4,55,20); coinLine(l,950,430,4,55,15); coinLine(l,1675,440,4,55,20); coinLine(l,3425,420,4,55,20); coinLine(l,4155,440,5,55,18); coinLine(l,5745,440,5,55,18); coinLine(l,6065,320,4,55,20);
  e(l,360,554,100,650,'dog'); e(l,1010,444,950,1120); e(l,1740,554,1620,2280,'dog'); e(l,2700,444,2600,2760); e(l,3490,554,3380,3850,'dog'); e(l,4230,454,4150,4350); e(l,5100,554,4980,5430,'dog'); e(l,5860,554,5700,6360,'dog');
  s(l,610,566,90); s(l,1260,566,100); s(l,2180,566,120); s(l,3000,566,120); s(l,3740,566,100); s(l,4560,566,120); s(l,5320,566,110); s(l,6220,566,110);
  b(l,1340,540); b(l,2110,540); b(l,3110,540); b(l,3810,540); b(l,4670,540); b(l,5420,540); b(l,6280,540);
  saw(l,780,430,28,'y',120,2.0,.2); saw(l,1500,430,27,'x',110,2.25,1.0); saw(l,2410,430,30,'y',125,2.35,2.2); saw(l,3260,420,29,'x',115,2.5,.6); saw(l,3980,430,28,'y',125,2.65,1.8); saw(l,4860,420,30,'x',115,2.8,2.6); saw(l,5580,430,29,'y',120,3.0,.9);
  fall(l,1150,170,70,52,200); fall(l,2230,190,75,55,210); fall(l,3890,170,80,55,230); fall(l,5450,185,75,52,210);
  cp(l,3430,520); cp(l,5080,520);
  l.decor=[{type:'lamp',x:500},{type:'ruin',x:1800},{type:'lamp',x:2800},{type:'ruin',x:4250},{type:'lamp',x:5300}];
  l.totalCoins=l.coins.length; levels.push(l);
  return levels;
}

const levelTemplates = makeLevels();

function cloneLevel(index){
  return JSON.parse(JSON.stringify(levelTemplates[index]));
}

function newPlayer(){
  return {x:80,y:480,w:52,h:106,vx:0,vy:0,onGround:false,facing:1,health:3,maxHealth:3,inv:0,hammerTimer:0,hammerCooldown:0,coins:0,checkpoint:{x:80,y:470},deadTimer:0,runTime:0,landSquash:0};
}

function startLevel(index){
  currentLevelIndex=index;
  level=cloneLevel(index);
  player=newPlayer();
  camera.x=0; camera.targetX=0; camera.shake=0;
  particles=[]; floatingTexts=[]; cutscene=null;
  state='playing'; stateTime=0; message=''; messageTimer=0;
  touchControls.classList.add('visible');
  input.left=input.right=input.jump=input.hammer=false;
  beep('checkpoint');
}

function showMenu(){ state='menu'; stateTime=0; touchControls.classList.remove('visible'); }
function showSelect(){ state='select'; stateTime=0; touchControls.classList.remove('visible'); }

function spawnParticles(x,y,type='spark',count=8){
  for(let i=0;i<count;i++){
    const a=Math.random()*TAU, sp=type==='confetti'?80+Math.random()*280:80+Math.random()*180;
    particles.push({x,y,vx:Math.cos(a)*sp,vy:Math.sin(a)*sp-(type==='confetti'?160:40),life:.45+Math.random()*.8,maxLife:1,type,size:3+Math.random()*6,rot:Math.random()*TAU});
  }
}
function floatText(x,y,str,color='#fff'){ floatingTexts.push({x,y,str,color,life:1,maxLife:1}); }

function updateFixed(dt){
  stateTime += dt;
  if(messageTimer>0) messageTimer-=dt;
  updateParticles(dt);
  if(state==='playing') updateGame(dt);
  else if(state==='cutscene') updateCutscene(dt);
}

function updateParticles(dt){
  for(const p of particles){ p.life-=dt; p.vy+=520*dt; p.x+=p.vx*dt; p.y+=p.vy*dt; p.rot+=dt*4; }
  particles=particles.filter(p=>p.life>0);
  for(const f of floatingTexts){ f.life-=dt; f.y-=42*dt; }
  floatingTexts=floatingTexts.filter(f=>f.life>0);
}

function updateMovingPlatforms(dt){
  const t=stateTime;
  for(const p of level.platforms){
    const ox=p.x, oy=p.y;
    if(p.type==='moving'){
      const v=Math.sin(t*p.speed+p.phase)*p.range;
      if(p.axis==='x') p.x=p.baseX+v; else p.y=p.baseY+v;
    }
    p.dx=p.x-ox; p.dy=p.y-oy;
  }
}

function updateGame(dt){
  if(!player || !level) return;
  updateMovingPlatforms(dt);
  player.inv=Math.max(0,player.inv-dt);
  player.hammerCooldown=Math.max(0,player.hammerCooldown-dt);
  player.hammerTimer=Math.max(0,player.hammerTimer-dt);
  player.landSquash=Math.max(0,player.landSquash-dt*4);

  if(player.deadTimer>0){
    player.deadTimer-=dt;
    player.vy+=1900*dt; player.y+=player.vy*dt;
    if(player.deadTimer<=0) respawnPlayer();
    return;
  }

  let move=(input.right?1:0)-(input.left?1:0);
  if(move!==0){
    player.facing=move;
    player.vx += move*1900*dt;
    player.runTime += dt*Math.abs(player.vx)/170;
  } else {
    const drag=player.onGround?14:3.2;
    player.vx *= Math.max(0,1-drag*dt);
  }
  player.vx=clamp(player.vx,-340,340);

  if((input.jumpPressed || input.jump) && player.onGround){
    player.vy=-720; player.onGround=false; beep('jump'); spawnParticles(player.x+player.w/2,player.y+player.h,'dust',5);
  }
  input.jumpPressed=false;

  if((input.hammerPressed || input.hammer) && player.hammerCooldown<=0){
    player.hammerTimer=.28; player.hammerCooldown=.42; beep('hammer');
  }
  input.hammerPressed=false;

  const oldY=player.y;
  const oldBottom=oldY+player.h;
  player.vy += 1950*dt;
  player.vy = Math.min(player.vy,1150);

  // Carry player with platform under feet.
  for(const p of level.platforms){
    if(Math.abs((player.y+player.h)-p.y)<4 && player.x+player.w>p.x+4 && player.x<p.x+p.w-4){ player.x+=p.dx; player.y+=p.dy; }
  }

  // Horizontal collision.
  player.x += player.vx*dt;
  for(const s of solidRects()){
    if(rects(player,s)){
      if(player.vx>0) player.x=s.x-player.w; else if(player.vx<0) player.x=s.x+s.w;
      player.vx=0;
    }
  }

  // Vertical collision.
  player.y += player.vy*dt;
  player.onGround=false;
  let landedOn=null;
  for(const s of solidRects()){
    if(!rects(player,s)) continue;
    if(player.vy>=0 && oldBottom<=s.y+Math.max(12,Math.abs(s.dy||0)+6)){
      player.y=s.y-player.h; player.vy=0; player.onGround=true; landedOn=s;
    } else if(player.vy<0 && oldY>=s.y+s.h-8){
      player.y=s.y+s.h; player.vy=40;
    } else {
      if(player.x+player.w/2<s.x+s.w/2) player.x=s.x-player.w; else player.x=s.x+s.w;
      player.vx=0;
    }
  }
  if(landedOn && oldY+player.h < landedOn.y-12){ player.landSquash=.5; spawnParticles(player.x+player.w/2,player.y+player.h,'dust',4); }

  // Level bounds.
  player.x=clamp(player.x,0,level.width-player.w);
  if(player.y>820) hurtPlayer(true);

  updateCoins();
  updateEnemies(dt);
  updateHazards(dt);
  updateCheckpoints();
  updateHammerHits();

  if(player.x+player.w > level.goal.x && player.onGround){ beginCutscene(); }

  camera.targetX=clamp(player.x-W*.36,0,Math.max(0,level.width-W));
  camera.x=lerp(camera.x,camera.targetX,1-Math.pow(.0005,dt));
  camera.shake=Math.max(0,camera.shake-dt*2.5);
}

function solidRects(){
  const arr=level.platforms.slice();
  for(const b of level.crates) if(!b.broken) arr.push(b);
  for(const f of level.falling) if(!f.done) arr.push(f);
  return arr;
}

function updateCoins(){
  const pc={x:player.x,y:player.y,w:player.w,h:player.h};
  for(const coin of level.coins){
    if(coin.taken) continue;
    const box={x:coin.x-17,y:coin.y-17,w:34,h:34};
    if(rects(pc,box)){
      coin.taken=true; player.coins++; beep('coin');
      spawnParticles(coin.x,coin.y,'coin',8); floatText(coin.x,coin.y-20,'+1','#ffd84d');
    }
  }
}

function updateEnemies(dt){
  for(const e of level.enemies){
    if(!e.alive) continue;
    if(e.stun>0){ e.stun-=dt; continue; }
    e.x += e.dir*e.speed*dt;
    if(e.x<e.minX){e.x=e.minX;e.dir=1;} if(e.x+e.w>e.maxX){e.x=e.maxX-e.w;e.dir=-1;}
    const pb={x:player.x,y:player.y,w:player.w,h:player.h};
    if(rects(pb,e) && player.inv<=0){
      const playerBottom=player.y+player.h;
      if(player.vy>120 && playerBottom<e.y+e.h*.6){
        e.alive=false; player.vy=-470; beep('hammer'); spawnParticles(e.x+e.w/2,e.y+e.h/2,'spark',10); floatText(e.x,e.y-20,'БАМ!','#fff176');
      } else hurtPlayer(false,e.x+e.w/2);
    }
  }
}

function updateHazards(dt){
  for(const saw of level.saws){
    const v=Math.sin(stateTime*saw.speed+saw.phase)*saw.range;
    saw.x=saw.baseX+(saw.axis==='x'?v:0); saw.y=saw.baseY+(saw.axis==='y'?v:0);
    const box={x:saw.x-saw.r*.72,y:saw.y-saw.r*.72,w:saw.r*1.44,h:saw.r*1.44};
    if(rects(player,box)) hurtPlayer(false,saw.x);
  }
  for(const s of level.spikes){ if(rects(player,s)) hurtPlayer(false,s.x+s.w/2); }
  for(const f of level.falling){
    if(!f.active && !f.done && Math.abs(player.x-f.x)<f.trigger){ f.active=true; }
    if(f.active && !f.done){
      f.vy+=1500*dt; f.y+=f.vy*dt;
      if(rects(player,f)) hurtPlayer(false,f.x+f.w/2);
      if(f.y>760) f.done=true;
    }
  }
}

function updateCheckpoints(){
  for(const cp of level.checkpoints){
    if(!cp.active && player.x>cp.x){
      cp.active=true; player.checkpoint={x:cp.x+25,y:450};
      message='Контрольная точка'; messageTimer=1.7; beep('checkpoint'); spawnParticles(cp.x,cp.y,'confetti',16);
    }
  }
}

function updateHammerHits(){
  if(player.hammerTimer<=0) return;
  const reach=75;
  const hit={x:player.facing>0?player.x+player.w-5:player.x-reach+5,y:player.y+25,w:reach,h:72};
  for(const b of level.crates){
    if(!b.broken && rects(hit,b)){
      b.broken=true; camera.shake=.35; spawnParticles(b.x+b.w/2,b.y+b.h/2,'wood',15); floatText(b.x,b.y-12,'ХРУСТ!','#ffcc80');
      if(Math.random()<.55){ player.coins++; floatText(b.x+20,b.y-40,'+1','#ffd84d'); }
    }
  }
  for(const e of level.enemies){
    if(e.alive && rects(hit,e)){
      e.alive=false; camera.shake=.25; spawnParticles(e.x+e.w/2,e.y+e.h/2,'spark',12); floatText(e.x,e.y-20,'ГОТОВО!','#b9ff89');
    }
  }
}

function hurtPlayer(fall=false,sourceX=player.x){
  if(player.inv>0 || player.deadTimer>0) return;
  player.health--; player.inv=1.25; camera.shake=.75; beep('hit');
  player.vx=(player.x+player.w/2<sourceX?-1:1)*-320; player.vy=-410;
  spawnParticles(player.x+player.w/2,player.y+50,'hit',10);
  if(fall || player.health<=0){
    player.deadTimer=.8; player.vy=-320; player.health=Math.max(0,player.health);
  }
}

function respawnPlayer(){
  if(player.health<=0){ state='gameover'; stateTime=0; touchControls.classList.remove('visible'); return; }
  player.x=player.checkpoint.x; player.y=player.checkpoint.y; player.vx=0; player.vy=0; player.inv=1.2;
  camera.x=clamp(player.x-W*.3,0,level.width-W);
}

function beginCutscene(){
  if(state!=='playing') return;
  state='cutscene'; stateTime=0; touchControls.classList.remove('visible');
  player.vx=0; player.vy=0;
  player.x=level.goal.x+30; player.y=494;
  cutscene={time:0,duration:7.2,startStage:currentLevelIndex,endStage:currentLevelIndex+1,hammerHits:0};
  beep('checkpoint');
}

function updateCutscene(dt){
  cutscene.time+=dt;
  const t=cutscene.time;
  const houseX=level.goal.x+240;
  camera.targetX=clamp(houseX-W*.68,0,level.width-W);
  camera.x=lerp(camera.x,camera.targetX,1-Math.pow(.0001,dt));
  if(t>1.0 && t<5.8){
    const hitIndex=Math.floor((t-1)/.72);
    if(hitIndex>=0 && hitIndex!==cutscene.hammerHits){
      cutscene.hammerHits=hitIndex; beep('hammer'); camera.shake=.3; spawnParticles(houseX-15,430-Math.random()*150,'spark',12);
    }
  }
  if(t>6.0 && !cutscene.won){
    cutscene.won=true; beep('win'); spawnParticles(houseX,220,'confetti',70);
  }
  if(t>=cutscene.duration){ finishLevel(); }
}

function finishLevel(){
  const i=currentLevelIndex;
  save.completed[i]=true;
  save.bestCoins[i]=Math.max(save.bestCoins[i]||0,player.coins);
  save.unlocked=Math.max(save.unlocked,Math.min(3,i+2));
  storeSave();
  state='complete'; stateTime=0; touchControls.classList.remove('visible');
}

function updateFrame(dt){
  accumulator+=Math.min(dt,.05);
  while(accumulator>=1/60){ updateFixed(1/60); accumulator-=1/60; }
}

function render(){
  ctx.clearRect(0,0,W,H);
  if(state==='menu') drawMenu();
  else if(state==='select') drawLevelSelect();
  else if(state==='playing' || state==='paused' || state==='cutscene' || state==='gameover' || state==='complete'){
    drawWorld();
    if(state==='playing') drawHUD();
    if(state==='paused') drawPause();
    if(state==='gameover') drawGameOver();
    if(state==='complete') drawComplete();
  }
}

function drawBackground(theme,camX=0){
  const grad=ctx.createLinearGradient(0,0,0,H);
  if(theme==='day'){ grad.addColorStop(0,'#79ccff'); grad.addColorStop(.72,'#dff6ff'); grad.addColorStop(1,'#f5e5a3'); }
  else if(theme==='sunset'){ grad.addColorStop(0,'#6456a8'); grad.addColorStop(.45,'#f09278'); grad.addColorStop(1,'#ffd18a'); }
  else { grad.addColorStop(0,'#071326'); grad.addColorStop(.65,'#17304b'); grad.addColorStop(1,'#27445d'); }
  ctx.fillStyle=grad; ctx.fillRect(0,0,W,H);
  if(theme==='night'){
    ctx.fillStyle='rgba(255,255,220,.85)'; ctx.beginPath(); ctx.arc(1040,105,54,0,TAU); ctx.fill();
    for(let i=0;i<65;i++){ const x=(i*173)%W, y=(i*83)%300; ctx.globalAlpha=.35+((i*17)%60)/100; ctx.fillRect(x,y,2,2); } ctx.globalAlpha=1;
  } else {
    ctx.fillStyle=theme==='sunset'?'rgba(255,232,165,.8)':'rgba(255,248,188,.9)'; ctx.beginPath(); ctx.arc(1040,105,55,0,TAU); ctx.fill();
  }
  // distant hills, parallax
  const px=(camX*.12)%900;
  ctx.fillStyle=theme==='night'?'#142d3f':'#8bc77c';
  ctx.beginPath(); ctx.moveTo(-100,520);
  for(let x=-100;x<=W+200;x+=180){ const yy=390+Math.sin((x+px)/250)*55; ctx.quadraticCurveTo(x+90,yy-90,x+180,yy); }
  ctx.lineTo(W+200,H); ctx.lineTo(-100,H); ctx.closePath(); ctx.fill();
  ctx.fillStyle=theme==='night'?'#102335':'#5da45d';
  ctx.beginPath(); ctx.moveTo(-100,560);
  for(let x=-100;x<=W+200;x+=150){ const yy=455+Math.sin((x+camX*.22)/160)*35; ctx.quadraticCurveTo(x+70,yy-65,x+150,yy); }
  ctx.lineTo(W+200,H); ctx.lineTo(-100,H); ctx.closePath(); ctx.fill();
  if(theme==='night'){
    ctx.strokeStyle='rgba(190,220,255,.18)'; ctx.lineWidth=2;
    for(let i=0;i<55;i++){ const x=((i*71+stateTime*420)%1400)-60, y=(i*53)%720; ctx.beginPath();ctx.moveTo(x,y);ctx.lineTo(x-18,y+45);ctx.stroke(); }
  }
}

function drawWorld(){
  if(!level) return;
  drawBackground(level.theme,camera.x);
  const shakeX=(Math.random()-.5)*camera.shake*18, shakeY=(Math.random()-.5)*camera.shake*12;
  ctx.save(); ctx.translate(-camera.x+shakeX,shakeY);
  drawDecorBehind();
  drawHouse(level.goal.x+240, currentHouseStage());
  drawPlatforms();
  drawCheckpoints();
  drawCoins();
  drawCrates();
  drawFalling();
  drawSpikes();
  drawSaws();
  drawEnemies();
  drawPlayer();
  drawParticlesWorld();
  ctx.restore();
  if(state==='cutscene') drawCutsceneOverlay();
  if(messageTimer>0){ const a=clamp(messageTimer/1.7,0,1); ctx.globalAlpha=Math.min(1,a*2); roundRect(ctx,W/2-210,85,420,64,20,'rgba(15,23,42,.78)','rgba(255,255,255,.35)',2); text(message,W/2,117,28); ctx.globalAlpha=1; }
}

function currentHouseStage(){
  if(state==='complete') return currentLevelIndex+1;
  if(state==='cutscene'){
    const t=clamp((cutscene.time-1.0)/4.8,0,1);
    return cutscene.startStage + easeInOut(t);
  }
  return currentLevelIndex;
}
function easeInOut(t){ return t<.5?2*t*t:1-Math.pow(-2*t+2,2)/2; }

function drawDecorBehind(){
  for(const d of level.decor){
    const x=d.x;
    if(x<camera.x-500||x>camera.x+W+500) continue;
    if(d.type==='tree') drawTree(x,600);
    if(d.type==='shed') drawShed(x,600);
    if(d.type==='fence') drawFence(x,600,420);
    if(d.type==='crane') drawCrane(x,600);
    if(d.type==='scaffold') drawScaffold(x,600);
    if(d.type==='lamp') drawLamp(x,600);
    if(d.type==='ruin') drawRuin(x,600);
  }
}
function drawTree(x,y){ ctx.fillStyle='#70492d';ctx.fillRect(x-22,y-200,44,200);ctx.fillStyle='#2d7c45'; for(const [dx,dy,r] of [[0,-230,82],[-60,-190,62],[62,-190,66]]){ctx.beginPath();ctx.arc(x+dx,y+dy,r,0,TAU);ctx.fill();} }
function drawShed(x,y){ ctx.fillStyle='#9a704f';ctx.fillRect(x-120,y-150,240,150);ctx.fillStyle='#6e4637';ctx.beginPath();ctx.moveTo(x-145,y-150);ctx.lineTo(x,y-245);ctx.lineTo(x+145,y-150);ctx.closePath();ctx.fill();ctx.fillStyle='#463127';ctx.fillRect(x-35,y-95,70,95); }
function drawFence(x,y,w){ ctx.strokeStyle='#9a7449';ctx.lineWidth=13;ctx.beginPath();ctx.moveTo(x,y-85);ctx.lineTo(x+w,y-85);ctx.moveTo(x,y-35);ctx.lineTo(x+w,y-35);ctx.stroke();for(let i=0;i<=w;i+=65){ctx.fillStyle='#b9905f';ctx.fillRect(x+i-10,y-130,20,130);} }
function drawCrane(x,y){ ctx.strokeStyle='#d5a725';ctx.lineWidth=14;ctx.beginPath();ctx.moveTo(x,y);ctx.lineTo(x,y-410);ctx.lineTo(x+330,y-410);ctx.stroke();ctx.lineWidth=7;ctx.beginPath();ctx.moveTo(x,y-350);ctx.lineTo(x+260,y-410);ctx.moveTo(x,y-350);ctx.lineTo(x-90,y-410);ctx.stroke();ctx.strokeStyle='#444';ctx.lineWidth=3;ctx.beginPath();ctx.moveTo(x+250,y-410);ctx.lineTo(x+250,y-235);ctx.stroke();ctx.fillStyle='#555';ctx.fillRect(x+230,y-235,40,28); }
function drawScaffold(x,y){ ctx.strokeStyle='#6b747e';ctx.lineWidth=8;for(let i=0;i<4;i++){ctx.beginPath();ctx.moveTo(x+i*70,y);ctx.lineTo(x+i*70,y-280);ctx.stroke();}for(let j=0;j<5;j++){ctx.beginPath();ctx.moveTo(x,y-j*65);ctx.lineTo(x+210,y-j*65);ctx.stroke();} }
function drawLamp(x,y){ ctx.strokeStyle='#263642';ctx.lineWidth=12;ctx.beginPath();ctx.moveTo(x,y);ctx.lineTo(x,y-255);ctx.quadraticCurveTo(x,y-290,x+35,y-290);ctx.stroke();ctx.fillStyle='rgba(255,229,139,.72)';ctx.beginPath();ctx.arc(x+45,y-278,30,0,TAU);ctx.fill(); }
function drawRuin(x,y){ ctx.fillStyle='#57606a';ctx.fillRect(x-120,y-190,250,190);ctx.fillStyle='#38404a';ctx.beginPath();ctx.moveTo(x-140,y-190);ctx.lineTo(x-50,y-270);ctx.lineTo(x+30,y-210);ctx.lineTo(x+140,y-260);ctx.lineTo(x+140,y-190);ctx.closePath();ctx.fill();ctx.fillStyle='#19242e';ctx.fillRect(x-70,y-130,65,80);ctx.fillRect(x+40,y-145,60,70); }

function drawPlatforms(){
  for(const p of level.platforms){
    if(p.x+p.w<camera.x-100||p.x>camera.x+W+100) continue;
    if(p.type==='ground'){
      ctx.fillStyle=level.theme==='night'?'#394653':'#76523a';ctx.fillRect(p.x,p.y,p.w,p.h);
      ctx.fillStyle=level.theme==='night'?'#4f6b58':'#62a14d';ctx.fillRect(p.x,p.y,p.w,18);
      ctx.fillStyle='rgba(0,0,0,.11)';for(let x=p.x+30;x<p.x+p.w;x+=75)ctx.fillRect(x,p.y+45,32,8);
    } else if(p.type==='brick'){
      ctx.fillStyle='#b86548';ctx.fillRect(p.x,p.y,p.w,p.h);ctx.strokeStyle='#763f33';ctx.lineWidth=3;
      for(let x=p.x;x<p.x+p.w;x+=48){ctx.strokeRect(x,p.y,48,p.h);}
    } else if(p.type==='wood'){
      ctx.fillStyle='#a36d39';ctx.fillRect(p.x,p.y,p.w,p.h);ctx.strokeStyle='#6e4728';ctx.lineWidth=4;ctx.strokeRect(p.x,p.y,p.w,p.h);
      for(let x=p.x+35;x<p.x+p.w;x+=65){ctx.beginPath();ctx.moveTo(x,p.y);ctx.lineTo(x-12,p.y+p.h);ctx.stroke();}
    } else {
      const moving=p.type==='moving'; ctx.fillStyle=moving?'#f0b62f':'#7b8794';ctx.fillRect(p.x,p.y,p.w,p.h);ctx.strokeStyle=moving?'#8a5c00':'#404a54';ctx.lineWidth=4;ctx.strokeRect(p.x,p.y,p.w,p.h);
      for(let x=p.x+18;x<p.x+p.w;x+=42){ctx.fillStyle=moving?'#5f6570':'#333b44';ctx.beginPath();ctx.arc(x,p.y+p.h/2,4,0,TAU);ctx.fill();}
    }
  }
}

function drawCoins(){
  for(const c of level.coins){
    if(c.taken||c.x<camera.x-60||c.x>camera.x+W+60) continue;
    const yy=c.y+Math.sin(stateTime*4+c.bob)*7;
    const scale=.25+.75*Math.abs(Math.sin(stateTime*5+c.bob));
    ctx.save();ctx.translate(c.x,yy);ctx.scale(scale,1);
    ctx.fillStyle='#ffd43b';ctx.strokeStyle='#b66a00';ctx.lineWidth=4;ctx.beginPath();ctx.arc(0,0,17,0,TAU);ctx.fill();ctx.stroke();ctx.strokeStyle='#fff2a3';ctx.lineWidth=3;ctx.beginPath();ctx.arc(-3,-3,9,Math.PI,Math.PI*1.7);ctx.stroke();ctx.restore();
  }
}

function drawCrates(){
  for(const b of level.crates){
    if(b.broken||b.x+b.w<camera.x-80||b.x>camera.x+W+80) continue;
    ctx.save();ctx.translate(b.x+b.w/2,b.y+b.h/2);ctx.rotate(Math.sin(stateTime*30)*b.shake*.02);ctx.translate(-b.w/2,-b.h/2);
    ctx.fillStyle='#b97a3e';ctx.fillRect(0,0,b.w,b.h);ctx.strokeStyle='#6e4224';ctx.lineWidth=5;ctx.strokeRect(0,0,b.w,b.h);ctx.beginPath();ctx.moveTo(6,6);ctx.lineTo(b.w-6,b.h-6);ctx.moveTo(b.w-6,6);ctx.lineTo(6,b.h-6);ctx.stroke();ctx.restore();
  }
}
function drawFalling(){
  for(const f of level.falling){ if(f.done) continue; ctx.fillStyle='#8c4f3f';ctx.fillRect(f.x,f.y,f.w,f.h);ctx.strokeStyle='#512e28';ctx.lineWidth=4;ctx.strokeRect(f.x,f.y,f.w,f.h);ctx.beginPath();ctx.moveTo(f.x+8,f.y+10);ctx.lineTo(f.x+f.w-8,f.y+f.h-12);ctx.stroke(); }
}
function drawSpikes(){
  for(const s of level.spikes){ctx.fillStyle='#aab5bf';ctx.strokeStyle='#48515b';ctx.lineWidth=3;const n=Math.max(2,Math.floor(s.w/22));for(let i=0;i<n;i++){const x=s.x+i*s.w/n;ctx.beginPath();ctx.moveTo(x,s.y+s.h);ctx.lineTo(x+s.w/n/2,s.y);ctx.lineTo(x+s.w/n,s.y+s.h);ctx.closePath();ctx.fill();ctx.stroke();}}
}
function drawSaws(){
  for(const s of level.saws){ctx.save();ctx.translate(s.x,s.y);ctx.rotate(stateTime*5*s.speed);ctx.fillStyle='#aab3bd';ctx.strokeStyle='#3a424b';ctx.lineWidth=4;ctx.beginPath();for(let i=0;i<16;i++){const a=i/16*TAU,r=i%2?s.r*.72:s.r;const x=Math.cos(a)*r,y=Math.sin(a)*r;i?ctx.lineTo(x,y):ctx.moveTo(x,y);}ctx.closePath();ctx.fill();ctx.stroke();ctx.fillStyle='#424b55';ctx.beginPath();ctx.arc(0,0,s.r*.24,0,TAU);ctx.fill();ctx.restore();}
}
function drawCheckpoints(){
  for(const cp of level.checkpoints){ctx.strokeStyle='#76523a';ctx.lineWidth=8;ctx.beginPath();ctx.moveTo(cp.x,cp.y+80);ctx.lineTo(cp.x,cp.y-50);ctx.stroke();ctx.fillStyle=cp.active?'#60d36d':'#d6dce4';ctx.beginPath();ctx.moveTo(cp.x,cp.y-50);ctx.lineTo(cp.x+70,cp.y-25);ctx.lineTo(cp.x,cp.y);ctx.closePath();ctx.fill();text('✓',cp.x+28,cp.y-26,23,'center',cp.active?'#fff':'#65717c',false);}
}
function drawEnemies(){
  for(const e of level.enemies){if(!e.alive||e.x+e.w<camera.x-100||e.x>camera.x+W+100)continue;ctx.save();ctx.translate(e.x+e.w/2,e.y+e.h/2);ctx.scale(e.dir,1);if(e.type==='dog'){ctx.fillStyle='#8a5b36';ctx.beginPath();ctx.ellipse(0,5,27,20,0,0,TAU);ctx.fill();ctx.beginPath();ctx.arc(19,-8,17,0,TAU);ctx.fill();ctx.fillStyle='#5f3a22';ctx.beginPath();ctx.moveTo(12,-20);ctx.lineTo(7,-37);ctx.lineTo(23,-24);ctx.fill();ctx.beginPath();ctx.moveTo(25,-18);ctx.lineTo(34,-32);ctx.lineTo(35,-13);ctx.fill();ctx.fillStyle='#fff';ctx.beginPath();ctx.arc(23,-10,5,0,TAU);ctx.fill();ctx.fillStyle='#222';ctx.beginPath();ctx.arc(25,-10,2,0,TAU);ctx.fill();}else{ctx.fillStyle='#6fa75f';ctx.beginPath();ctx.ellipse(0,7,27,21,0,0,TAU);ctx.fill();ctx.fillStyle='#fff';ctx.beginPath();ctx.arc(-9,-1,6,0,TAU);ctx.arc(9,-1,6,0,TAU);ctx.fill();ctx.fillStyle='#202b22';ctx.beginPath();ctx.arc(-7,0,2,0,TAU);ctx.arc(11,0,2,0,TAU);ctx.fill();}ctx.restore();}
}

function drawPlayer(){
  if(!player) return;
  if(player.inv>0 && Math.floor(player.inv*12)%2===0) return;
  const x=player.x+player.w/2, y=player.y+player.h;
  const moving=player.onGround && Math.abs(player.vx||0)>24;
  const runPhase=player.runTime*7.5;
  const bob=moving?Math.abs(Math.sin(runPhase))*4:Math.sin(stateTime*2.1)*1.2;
  const squash=player.landSquash*.10;
  const airborneTilt=!player.onGround?clamp((player.vy||0)/1100,-.10,.12):0;
  const runTilt=moving?Math.sin(runPhase)*.018:0;
  const hammering=player.hammerTimer>0 || state==='cutscene';

  ctx.save();
  ctx.translate(x,y);
  // Soft contact shadow keeps the detailed sprite readable against every level.
  ctx.save();
  ctx.scale(player.facing,1);
  ctx.fillStyle='rgba(0,0,0,.24)';
  ctx.beginPath();ctx.ellipse(0,2,34-(moving?3:0),8,0,0,TAU);ctx.fill();
  ctx.restore();

  ctx.scale(player.facing,1);
  ctx.rotate(airborneTilt+runTilt);
  ctx.scale(1+squash,1-squash);

  if(heroSide.complete && heroSide.naturalWidth){
    const h=174;
    const w=h*(heroSide.naturalWidth/heroSide.naturalHeight);
    ctx.save();
    ctx.translate(0,-bob);
    // A tiny breathing/running deformation gives the single polished sprite life
    // without using the old pasted-photo head.
    if(moving) ctx.transform(1,0,Math.sin(runPhase)*.025,1,0,0);
    ctx.drawImage(heroSide,-w*.49,-h,w,h);
    ctx.restore();
  }else{
    // Safe fallback while the PNG is loading.
    ctx.fillStyle='#15181d';ctx.beginPath();ctx.roundRect(-27,-118,54,80,15);ctx.fill();
    ctx.fillStyle='#d9a181';ctx.beginPath();ctx.arc(0,-137,27,0,TAU);ctx.fill();
    ctx.fillStyle='#2c211d';ctx.beginPath();ctx.arc(0,-147,27,Math.PI,TAU);ctx.fill();
  }

  // Hammer strike and the end-of-level repair animation are rendered over the
  // character as a separate clean game element.
  if(hammering){
    const phase=state==='cutscene'?Math.sin(cutscene.time*8.5):Math.sin((.30-player.hammerTimer)*18);
    const angle=-1.55+phase*.72;
    ctx.save();ctx.translate(16,-102);ctx.rotate(angle);
    ctx.strokeStyle='#7a4a28';ctx.lineWidth=8;ctx.lineCap='round';
    ctx.beginPath();ctx.moveTo(0,0);ctx.lineTo(55,0);ctx.stroke();
    const metal=ctx.createLinearGradient(48,-15,78,15);metal.addColorStop(0,'#e4e8ed');metal.addColorStop(.5,'#77818b');metal.addColorStop(1,'#d5dbe1');
    ctx.fillStyle=metal;ctx.strokeStyle='#343a41';ctx.lineWidth=3;
    ctx.beginPath();ctx.roundRect(45,-15,38,30,7);ctx.fill();ctx.stroke();
    ctx.restore();
  }
  ctx.restore();
}
function drawHouse(x,stage){
  const y=600; stage=clamp(stage,0,3);
  const roofT=clamp(stage,0,1), wallT=clamp(stage-1,0,1), finalT=clamp(stage-2,0,1);
  // foundation and walls
  const brokenWall=level&&level.theme==='night'?'#6b6862':'#9b7b61';
  const goodWall=`rgb(${Math.round(lerp(155,236,wallT))},${Math.round(lerp(123,203,wallT))},${Math.round(lerp(97,151,wallT))})`;
  ctx.fillStyle=wallT>0?goodWall:brokenWall;ctx.fillRect(x-170,y-255,340,255);
  ctx.strokeStyle='#4c372c';ctx.lineWidth=8;ctx.strokeRect(x-170,y-255,340,255);
  // cracks fade
  ctx.globalAlpha=1-wallT;ctx.strokeStyle='#3d3028';ctx.lineWidth=5;ctx.beginPath();ctx.moveTo(x-90,y-250);ctx.lineTo(x-60,y-190);ctx.lineTo(x-80,y-135);ctx.moveTo(x+110,y-230);ctx.lineTo(x+70,y-175);ctx.lineTo(x+115,y-120);ctx.stroke();ctx.globalAlpha=1;
  // roof: broken to fixed
  ctx.fillStyle=roofT>.5?'#93453f':'#69453a';ctx.beginPath();ctx.moveTo(x-205,y-255);ctx.lineTo(x,y-410);ctx.lineTo(x+205,y-255);ctx.closePath();ctx.fill();ctx.stroke();
  if(roofT<.8){ctx.fillStyle='#79b7d4';ctx.beginPath();ctx.moveTo(x+30,y-388);ctx.lineTo(x+105,y-330);ctx.lineTo(x+70,y-286);ctx.lineTo(x-5,y-345);ctx.closePath();ctx.fill();}
  // roof tiles appear
  ctx.globalAlpha=roofT;ctx.strokeStyle='#5f2e2b';ctx.lineWidth=5;for(let yy=y-360;yy<y-265;yy+=24){ctx.beginPath();ctx.moveTo(x-120+(yy-(y-360))*.45,yy);ctx.lineTo(x+120-(yy-(y-360))*.45,yy);ctx.stroke();}ctx.globalAlpha=1;
  // door
  ctx.fillStyle=finalT>.5?'#5f3a25':'#44352d';ctx.fillRect(x-38,y-132,76,132);ctx.strokeStyle='#2d241f';ctx.strokeRect(x-38,y-132,76,132);ctx.fillStyle='#e0bd55';ctx.beginPath();ctx.arc(x+20,y-66,5,0,TAU);ctx.fill();
  // windows
  for(const wx of [x-118,x+80]){ctx.fillStyle=wallT>.45?'#9de0ff':'#222c33';ctx.fillRect(wx,y-195,72,70);ctx.strokeStyle='#4b352a';ctx.lineWidth=7;ctx.strokeRect(wx,y-195,72,70);ctx.beginPath();ctx.moveTo(wx+36,y-195);ctx.lineTo(wx+36,y-125);ctx.moveTo(wx,y-160);ctx.lineTo(wx+72,y-160);ctx.stroke();}
  // final details
  ctx.globalAlpha=finalT;ctx.fillStyle='#8b5a34';ctx.fillRect(x-85,y-15,170,15);ctx.fillRect(x-80,y-2,18,45);ctx.fillRect(x+62,y-2,18,45);
  ctx.fillStyle='#5e9d4f';for(let i=0;i<7;i++){ctx.beginPath();ctx.arc(x-220+i*70,y-12,18,0,TAU);ctx.fill();}
  ctx.fillStyle='#f2d25e';ctx.beginPath();ctx.arc(x-195,y-35,7,0,TAU);ctx.fill();ctx.globalAlpha=1;
  // sign
  ctx.fillStyle='#7f542f';ctx.fillRect(x-95,y-315,190,48);roundRect(ctx,x-90,y-310,180,38,8,stage>=3?'#55a861':'#c69345','#4f3525',3);text(stage>=3?'ДОМ ГОТОВ!':'СТАРЫЙ ДОМ',x,y-291,20,'center','#fff',false);
}

function drawParticlesWorld(){
  for(const p of particles){ctx.save();ctx.globalAlpha=clamp(p.life/p.maxLife,0,1);ctx.translate(p.x,p.y);ctx.rotate(p.rot);if(p.type==='coin'){ctx.fillStyle='#ffd43b';ctx.beginPath();ctx.arc(0,0,p.size,0,TAU);ctx.fill();}else if(p.type==='wood'){ctx.fillStyle='#9c6538';ctx.fillRect(-p.size,-2,p.size*2,4);}else if(p.type==='confetti'){ctx.fillStyle=['#ff5b5b','#ffd84d','#72e29a','#76b8ff','#dd82ff'][Math.floor((p.x+p.y)%5)];ctx.fillRect(-p.size/2,-p.size,p.size,p.size*2);}else if(p.type==='dust'){ctx.fillStyle='rgba(220,200,170,.8)';ctx.beginPath();ctx.arc(0,0,p.size,0,TAU);ctx.fill();}else{ctx.fillStyle=p.type==='hit'?'#ff6b6b':'#fff176';ctx.fillRect(-p.size/2,-p.size/2,p.size,p.size);}ctx.restore();}
  for(const f of floatingTexts){ctx.globalAlpha=clamp(f.life/f.maxLife,0,1);text(f.str,f.x,f.y,24,'center',f.color,true);ctx.globalAlpha=1;}
}

function drawHUD(){
  roundRect(ctx,24,20,265,76,22,'rgba(8,16,29,.76)','rgba(255,255,255,.24)',2);
  for(let i=0;i<player.maxHealth;i++){drawHeart(60+i*55,58,i<player.health?'#ff5364':'rgba(255,255,255,.18)');}
  drawCoinIcon(215,58,15);text(String(player.coins),242,59,29,'left','#ffe36b');
  roundRect(ctx,W-225,20,201,76,22,'rgba(8,16,29,.76)','rgba(255,255,255,.24)',2);text(`${currentLevelIndex+1}/3`,W-125,47,25);text(level.name,W-125,75,18,'center','#d7e6f4',false);
  roundRect(ctx,W-82,110,58,58,17,'rgba(8,16,29,.65)','rgba(255,255,255,.22)',2);text('Ⅱ',W-53,139,30);
}
function drawHeart(x,y,color){ctx.save();ctx.translate(x,y);ctx.fillStyle=color;ctx.beginPath();ctx.moveTo(0,14);ctx.bezierCurveTo(-28,-4,-25,-23,-9,-23);ctx.bezierCurveTo(0,-23,4,-15,4,-15);ctx.bezierCurveTo(4,-15,9,-23,18,-23);ctx.bezierCurveTo(35,-23,36,-3,0,14);ctx.fill();ctx.restore();}
function drawCoinIcon(x,y,r){ctx.fillStyle='#ffd43b';ctx.strokeStyle='#a96500';ctx.lineWidth=3;ctx.beginPath();ctx.arc(x,y,r,0,TAU);ctx.fill();ctx.stroke();ctx.strokeStyle='#fff1a3';ctx.beginPath();ctx.arc(x-2,y-2,r*.55,Math.PI,Math.PI*1.7);ctx.stroke();}

function drawMenu(){
  drawBackground('day',stateTime*25);
  drawHouse(970,save.completed.filter(Boolean).length);
  // Large, fully illustrated hero on the main menu.
  if(heroFront.complete && heroFront.naturalWidth){
    const heroH=430, heroW=heroH*(heroFront.naturalWidth/heroFront.naturalHeight);
    ctx.save();ctx.translate(255,585+Math.sin(stateTime*2)*3);
    ctx.fillStyle='rgba(0,0,0,.22)';ctx.beginPath();ctx.ellipse(0,4,75,16,0,0,TAU);ctx.fill();
    ctx.drawImage(heroFront,-heroW/2,-heroH,heroW,heroH);ctx.restore();
  }else{
    const temp=player;player={x:170,y:390,w:52,h:106,facing:1,onGround:true,runTime:stateTime*.3,landSquash:0,inv:0,hammerTimer:0,vx:0,vy:0};drawPlayer();player=temp;
  }
  ctx.fillStyle='rgba(5,12,25,.48)';ctx.fillRect(0,0,W,H);
  text('РЕМОНТНИК',W/2,116,74,'center','#fff');
  text('СТАРЫЙ ДОМ',W/2,181,42,'center','#ffd75a');
  text('Собирай монеты • преодолевай препятствия • ремонтируй дом',W/2,231,23,'center','#e9f4ff',false);
  uiButtons=[];
  drawUIButton(W/2-185,310,370,82,'ИГРАТЬ','play');
  drawUIButton(W/2-185,410,370,68,'ВЫБОР УРОВНЯ','select',false);
  drawUIButton(W/2-185,496,370,62,audioEnabled?'ЗВУК: ВКЛ':'ЗВУК: ВЫКЛ','sound',false);
  const completed=save.completed.filter(Boolean).length;
  text(`Дом восстановлен: ${completed}/3`,W/2,598,25,'center','#fff',true);
  text('Управление: A/D или стрелки, пробел — прыжок, X — молоток',W/2,660,19,'center','#d7e6f4',false);
}

function drawLevelSelect(){
  drawBackground('sunset',stateTime*18);ctx.fillStyle='rgba(5,12,25,.55)';ctx.fillRect(0,0,W,H);
  text('ВЫБЕРИ УРОВЕНЬ',W/2,82,54);
  uiButtons=[];
  const cardW=340, gap=35, start=(W-(cardW*3+gap*2))/2;
  for(let i=0;i<3;i++){
    const x=start+i*(cardW+gap), y=165, unlocked=i<save.unlocked;
    roundRect(ctx,x,y,cardW,390,28,unlocked?'rgba(17,30,50,.9)':'rgba(20,25,32,.78)',unlocked?'rgba(255,255,255,.35)':'rgba(255,255,255,.12)',3);
    ctx.save();ctx.beginPath();ctx.roundRect(x+14,y+14,cardW-28,170,20);ctx.clip();drawCardScene(i,x+14,y+14,cardW-28,170);ctx.restore();
    text(`${i+1}`,x+45,y+46,32,'center','#ffd75a');
    text(levelTemplates[i].name,x+cardW/2,y+220,28,'center',unlocked?'#fff':'#7c8794');
    text(levelTemplates[i].subtitle,x+cardW/2,y+258,18,'center',unlocked?'#cfe0ee':'#697684',false);
    drawCoinIcon(x+105,y+310,14);text(`${save.bestCoins[i]||0}/${levelTemplates[i].totalCoins}`,x+130,y+311,22,'left',unlocked?'#ffe36b':'#727d88');
    if(save.completed[i]) text('ПРОЙДЕН',x+cardW/2,y+350,22,'center','#79e58c');
    else if(!unlocked) text('🔒 ЗАКРЫТ',x+cardW/2,y+350,22,'center','#7c8794');
    else text('НАЧАТЬ',x+cardW/2,y+350,22,'center','#fff');
    if(unlocked) uiButtons.push({x,y,w:cardW,h:390,action:`level:${i}`});
  }
  drawUIButton(40,626,230,58,'← НАЗАД','menu',false);
}
function drawCardScene(i,x,y,w,h){
  const theme=levelTemplates[i].theme;const g=ctx.createLinearGradient(0,y,0,y+h);if(theme==='day'){g.addColorStop(0,'#7ed0ff');g.addColorStop(1,'#daf5ff');}else if(theme==='sunset'){g.addColorStop(0,'#6e5aab');g.addColorStop(1,'#f3a070');}else{g.addColorStop(0,'#071326');g.addColorStop(1,'#26455f');}ctx.fillStyle=g;ctx.fillRect(x,y,w,h);ctx.fillStyle=theme==='night'?'#243f4d':'#67a95c';ctx.fillRect(x,y+h-40,w,40);ctx.fillStyle='#76523a';ctx.fillRect(x,y+h-22,w,22);for(let k=0;k<5;k++){drawCoinIcon(x+45+k*52,y+70+Math.sin(k)*20,9);} }

function drawUIButton(x,y,w,h,label,action,primary=true){
  const hover=false;roundRect(ctx,x,y,w,h,22,primary?'#e99628':'rgba(20,34,54,.88)',primary?'#fff1b4':'rgba(255,255,255,.35)',3);text(label,x+w/2,y+h/2+1,primary?31:24,'center','#fff');uiButtons.push({x,y,w,h,action});
}

function drawPause(){ctx.fillStyle='rgba(3,8,18,.72)';ctx.fillRect(0,0,W,H);text('ПАУЗА',W/2,145,58);uiButtons=[];drawUIButton(W/2-175,250,350,72,'ПРОДОЛЖИТЬ','resume');drawUIButton(W/2-175,340,350,66,'ЗАНОВО','restart',false);drawUIButton(W/2-175,425,350,66,'В МЕНЮ','menu',false);}
function drawGameOver(){ctx.fillStyle='rgba(3,8,18,.76)';ctx.fillRect(0,0,W,H);text('НЕ ПОЛУЧИЛОСЬ',W/2,170,54,'center','#ff7474');text('Дом подождёт — попробуем ещё раз!',W/2,225,24,'center','#e6edf5',false);uiButtons=[];drawUIButton(W/2-180,310,360,76,'ПОВТОРИТЬ','restart');drawUIButton(W/2-180,405,360,66,'ВЫБОР УРОВНЯ','select',false);}
function drawComplete(){ctx.fillStyle='rgba(3,8,18,.68)';ctx.fillRect(0,0,W,H);text(currentLevelIndex===2?'ДОМ ПОЛНОСТЬЮ ГОТОВ!':'ЧАСТЬ ДОМА ОТРЕМОНТИРОВАНА!',W/2,125,currentLevelIndex===2?47:40,'center','#ffe16a');drawCoinIcon(W/2-70,220,23);text(`${player.coins} из ${level.totalCoins}`,W/2-35,221,34,'left','#fff');const pct=player.coins/level.totalCoins;const stars=pct>.85?3:pct>.55?2:1;for(let i=0;i<3;i++)drawStar(W/2+(i-1)*92,305,34,i<stars?'#ffd84d':'rgba(255,255,255,.2)');uiButtons=[];if(currentLevelIndex<2)drawUIButton(W/2-190,385,380,78,'СЛЕДУЮЩИЙ УРОВЕНЬ','next');else drawUIButton(W/2-190,385,380,78,'ВЫБОР УРОВНЯ','select');drawUIButton(W/2-190,480,380,66,'ПРОЙТИ ЕЩЁ РАЗ','restart',false);drawUIButton(W/2-190,562,380,62,'В МЕНЮ','menu',false);}
function drawStar(x,y,r,color){ctx.save();ctx.translate(x,y);ctx.fillStyle=color;ctx.strokeStyle='rgba(0,0,0,.35)';ctx.lineWidth=4;ctx.beginPath();for(let i=0;i<10;i++){const a=-Math.PI/2+i*Math.PI/5,rr=i%2===0?r:r*.46;const xx=Math.cos(a)*rr,yy=Math.sin(a)*rr;i?ctx.lineTo(xx,yy):ctx.moveTo(xx,yy);}ctx.closePath();ctx.fill();ctx.stroke();ctx.restore();}
function drawCutsceneOverlay(){const t=cutscene.time;if(t<1){ctx.fillStyle=`rgba(0,0,0,${.5*(1-t)})`;ctx.fillRect(0,0,W,H);}if(t>5.8){const a=clamp((t-5.8)/.5,0,1);ctx.globalAlpha=a;roundRect(ctx,W/2-300,72,600,84,24,'rgba(10,20,35,.82)','rgba(255,255,255,.3)',3);text(currentLevelIndex===2?'ГОТОВО! ДОМ СПАСЁН!':'РЕМОНТ ЗАВЕРШЁН',W/2,114,34,'center','#ffe06b');ctx.globalAlpha=1;} }

function handleAction(action){
  if(action==='play'){ startLevel(Math.min(save.unlocked-1,2)); return; }
  if(action==='select'){ showSelect(); return; }
  if(action==='menu'){ showMenu(); return; }
  if(action==='resume'){ state='playing';touchControls.classList.add('visible');return; }
  if(action==='restart'){ startLevel(currentLevelIndex);return; }
  if(action==='next'){ startLevel(Math.min(2,currentLevelIndex+1));return; }
  if(action==='sound'){audioEnabled=!audioEnabled;save.sound=audioEnabled;storeSave();if(audioEnabled)beep('coin');return;}
  if(action.startsWith('level:')){const i=Number(action.split(':')[1]);if(i<save.unlocked)startLevel(i);}
}

function canvasPoint(ev){const r=canvas.getBoundingClientRect();return{x:(ev.clientX-r.left)*W/r.width,y:(ev.clientY-r.top)*H/r.height};}
canvas.addEventListener('pointerdown',ev=>{
  beep('tap'); const p=canvasPoint(ev);
  if(state==='playing' && p.x>W-100 && p.y<190){state='paused';touchControls.classList.remove('visible');return;}
  for(let i=uiButtons.length-1;i>=0;i--){const b=uiButtons[i];if(p.x>=b.x&&p.x<=b.x+b.w&&p.y>=b.y&&p.y<=b.y+b.h){handleAction(b.action);return;}}
});

function bindButton(id,key){
  const el=document.getElementById(id);
  const down=e=>{e.preventDefault();if(key==='jump'&&!input.jump)input.jumpPressed=true;if(key==='hammer'&&!input.hammer)input.hammerPressed=true;input[key]=true;el.classList.add('active');};
  const up=e=>{e.preventDefault();input[key]=false;el.classList.remove('active');};
  el.addEventListener('pointerdown',down);el.addEventListener('pointerup',up);el.addEventListener('pointercancel',up);el.addEventListener('pointerleave',up);
}
bindButton('btnLeft','left');bindButton('btnRight','right');bindButton('btnJump','jump');bindButton('btnHammer','hammer');

window.addEventListener('keydown',e=>{
  keys.add(e.code);
  if(['ArrowLeft','ArrowRight','ArrowUp','Space'].includes(e.code))e.preventDefault();
  if(e.code==='ArrowLeft'||e.code==='KeyA')input.left=true;
  if(e.code==='ArrowRight'||e.code==='KeyD')input.right=true;
  if((e.code==='ArrowUp'||e.code==='KeyW'||e.code==='Space')&&!input.jump){input.jump=true;input.jumpPressed=true;}
  if((e.code==='KeyX'||e.code==='KeyE'||e.code==='ControlLeft')&&!input.hammer){input.hammer=true;input.hammerPressed=true;}
  if(e.code==='Escape')gameBack();
});
window.addEventListener('keyup',e=>{
  keys.delete(e.code);
  if(e.code==='ArrowLeft'||e.code==='KeyA')input.left=false;
  if(e.code==='ArrowRight'||e.code==='KeyD')input.right=false;
  if(e.code==='ArrowUp'||e.code==='KeyW'||e.code==='Space')input.jump=false;
  if(e.code==='KeyX'||e.code==='KeyE'||e.code==='ControlLeft')input.hammer=false;
});
window.addEventListener('blur',()=>{input.left=input.right=input.jump=input.hammer=false;if(state==='playing'){state='paused';touchControls.classList.remove('visible');}});

function gameBack(){
  if(state==='playing'){state='paused';touchControls.classList.remove('visible');return true;}
  if(state==='paused'||state==='complete'||state==='gameover'||state==='select'){showMenu();return true;}
  if(state==='cutscene')return true;
  return false;
}
window.gameBack=gameBack;

function loop(now){
  const dt=(now-lastTime)/1000;lastTime=now;updateFrame(dt);render();requestAnimationFrame(loop);
}
requestAnimationFrame(loop);

// Expose tiny debug API for automated smoke tests.
window.__remontnik={startLevel,showMenu,getState:()=>state,getPlayer:()=>player,getLevel:()=>level};
})();
