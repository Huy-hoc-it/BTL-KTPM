// Runs the k6 clients against an HTTP stub. This validates scripts, not the real API or PostgreSQL.
import http from 'node:http';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {spawn} from 'node:child_process';
import assert from 'node:assert/strict';

const k6=process.env.K6 || 'k6';
const temp=fs.mkdtempSync(path.join(os.tmpdir(),'cinema-k6-client-'));
const fixtureFile=path.join(temp,'fixtures.json');
const id=index=>'00000000-0000-0000-0000-'+String(index).padStart(12,'0');
const loadUsers=Array.from({length:6},(_,index)=>({userId:id(100+index),token:'user-'+index,seatId:id(200+index)}));
const contenders=Array.from({length:3},(_,index)=>({userId:id(300+index),token:'contender-'+index}));
const saved={movieIds:[id(1)],showtimeId:id(2),showtimeDate:'2026-10-29',contentionShowtimeId:id(3),
    contentionSeatIds:Array.from({length:9},(_,index)=>id(4+index)),loadUsers,contenders};
let mode='normal',sequence=1000,wrongOwner=0,created=0,movieCreated=0;
const bookings=new Map();
const movies=new Map([[id(1),{id:id(1),title:'Fixture movie',durationMinutes:120,posterUrl:null}]]);
const server=http.createServer(async(req,res)=>{
    const url=new URL(req.url,'http://localhost');
    const token=(req.headers.authorization || '').replace('Bearer ','');
    const reply=(status,body)=>{res.writeHead(status,{'Content-Type':'application/json'});res.end(body===undefined?undefined:JSON.stringify(body));};
    if(req.method==='GET' && url.pathname==='/api/v1/users/me') {
        const user=[...loadUsers,...contenders].find(value=>value.token===token);
        return reply(user?200:401,{data:user?{id:user.userId,role:'CUSTOMER'}:null});
    }
    if(req.method==='GET' && url.pathname==='/api/v1/bookings')
        return reply(200,[...bookings.values()].filter(value=>value.token===token).map(value=>({bookingId:value.bookingId,createdAt:'2026-10-27T00:00:00Z'})));
    if(req.method==='GET' && url.pathname==='/api/v1/movies') return reply(200,{data:[...movies.values()],meta:{totalPages:1}});
    if(req.method==='GET' && url.pathname.startsWith('/api/v1/movies/')) {
        const movie=movies.get(url.pathname.split('/').pop());
        return movie?reply(200,{data:movie}):reply(404,{error:{code:'MOVIE_NOT_FOUND'}});
    }
    if(req.method==='POST' && url.pathname==='/api/v1/admin/movies') {
        if(token!=='stub-admin') return reply(401,{error:{code:'UNAUTHORIZED'}});
        let raw='';for await(const chunk of req) raw+=chunk;
        const body=JSON.parse(raw);
        if(!body.title) return reply(422,{error:{details:[{field:'title'}]}});
        const movie={...body,id:id(sequence++)};movies.set(movie.id,movie);movieCreated++;
        return reply(201,{data:movie});
    }
    if(req.method==='PATCH' && url.pathname.startsWith('/api/v1/admin/movies/')) {
        if(token!=='stub-admin') return reply(401,{error:{code:'UNAUTHORIZED'}});
        const movie=movies.get(url.pathname.split('/').pop());if(!movie) return reply(404,{error:{code:'MOVIE_NOT_FOUND'}});
        let raw='';for await(const chunk of req) raw+=chunk;
        Object.assign(movie,JSON.parse(raw));return reply(200,{data:movie});
    }
    if(req.method==='DELETE' && url.pathname.startsWith('/api/v1/admin/movies/')) {
        if(token!=='stub-admin') return reply(401,{error:{code:'UNAUTHORIZED'}});
        movies.delete(url.pathname.split('/').pop());return reply(204);
    }
    if(req.method==='GET' && url.pathname.startsWith('/api/v1/showtimes')) return reply(200,{data:[]});
    if(req.method==='POST' && url.pathname==='/api/v1/bookings') {
        let raw='';for await(const chunk of req) raw+=chunk;
        const body=JSON.parse(raw);
        const existing=[...bookings.values()].find(value=>value.showtimeId===body.showtimeId &&
            value.seatId===body.seatIds[0] && value.status==='CONFIRMED');
        if(existing && mode!=='all-win') return reply(409,{error:{code:mode==='wrong-code'?'DATA_CONFLICT':'SEAT_ALREADY_BOOKED'}});
        const bookingId=id(sequence++);
        const booking={bookingId,token,showtimeId:body.showtimeId,seatId:body.seatIds[0],status:'CONFIRMED'};
        bookings.set(bookingId,booking);created++;return reply(201,{data:booking});
    }
    if(req.method==='DELETE' && url.pathname.startsWith('/api/v1/bookings/')) {
        const booking=bookings.get(url.pathname.split('/').pop());
        if(!booking || booking.token!==token) {wrongOwner++;return reply(403,{error:{code:'FORBIDDEN'}});}
        booking.status='CANCELLED';return reply(204);
    }
    reply(404,{error:{code:'NOT_FOUND'}});
});
await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
const base='http://127.0.0.1:'+server.address().port;
fs.mkdirSync('load-test/results',{recursive:true});
fs.writeFileSync(fixtureFile,JSON.stringify(saved));
const results=[];
async function run(name,script,expectedExit) {
    const child=spawn(k6,['run','--quiet','--include-system-env-vars','load-test/'+script+'.js'],{env:{...process.env,BASE_URL:base,FIXTURES:fixtureFile,
        ADMIN_TOKEN:'stub-admin',VUS:'3',WARMUP_SECONDS:'1',DURATION_SECONDS:'2',SUMMARY_PATH:'load-test/results/client-'+name+'.json'},windowsHide:true});
    let output='';child.stdout.on('data',value=>output+=value);child.stderr.on('data',value=>output+=value);
    const code=await new Promise((resolve,reject)=>{child.on('error',reject);child.on('close',resolve);});
    assert.equal(code,expectedExit,name+' returned '+code+'\n'+output);
    results.push({name,exitCode:code,expectedExitCode:expectedExit});
}
try {
    await run('smoke','smoke',0);
    assert(movieCreated>0,'smoke must create a Movie');
    assert.equal(movies.size,1,'smoke must delete its temporary Movie');
    await run('movie-read','movie-read',0);
    await run('booking-normal','booking',0);
    assert.equal(wrongOwner,0,'warmup state must not cancel another measurement user booking');
    assert(created>0,'normal script must actually create bookings');
    assert([...bookings.values()].every(value=>value.status==='CANCELLED'),'normal run must cleanup all fixture bookings');
    for(const scenario of ['normal','all-win','wrong-code']) {
        bookings.clear();mode=scenario;
        await run('contention-'+scenario,'contention',scenario==='normal'?0:99);
    }
    fs.writeFileSync('load-test/results/client-check.json',JSON.stringify({kind:'HTTP stub, not Booking/DB integration',results},null,2));
    console.log('Six k6 client scenarios passed, including rejection of multiple winners and wrong conflict codes.');
} finally {
    server.close();fs.unlinkSync(fixtureFile);fs.rmdirSync(temp);
}
