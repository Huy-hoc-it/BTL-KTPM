import fs from 'node:fs';
import path from 'node:path';

const base=(process.env.BASE_URL || 'http://127.0.0.1:8080').replace(/\/$/,'');
const full=process.env.SEED_SCOPE === 'all';
const count=Number(process.env.MOVIE_COUNT || 1000);
const deletedCount=Number(process.env.DELETED_COUNT || 50);
const vus=Number(process.env.VUS || 50);
const prefix='load-' + Date.now().toString(36);
if (!Number.isInteger(count) || count < 1 || deletedCount < 0 || deletedCount >= count || !Number.isInteger(vus) || vus < 1 || vus > 50)
    throw new Error('Use MOVIE_COUNT>DELETED_COUNT>=0 and VUS between 1 and 50');
async function api(method, route, body, token, expected=200) {
    const response=await fetch(base+route,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},
        body:body===undefined?undefined:JSON.stringify(body)});
    const result=response.status===204?null:await response.json();
    if(response.status!==expected) throw new Error(method+' '+route+' returned '+response.status);
    return result && (result.data===undefined?result:result.data);
}
let admin=process.env.ADMIN_TOKEN;
if(!admin) {
    if(!process.env.ADMIN_USERNAME || !process.env.ADMIN_PASSWORD) throw new Error('Set ADMIN_TOKEN or ADMIN_USERNAME and ADMIN_PASSWORD');
    admin=(await api('POST','/api/v1/auth/login',{username:process.env.ADMIN_USERNAME,password:process.env.ADMIN_PASSWORD})).accessToken;
}
const saved={baseUrl:base,createdAt:new Date().toISOString(),scope:full?'all':'movie',movieIds:[],createdCount:count,deletedCount};
fs.mkdirSync('load-test/results',{recursive:true});
const output=path.resolve(process.env.FIXTURES || 'load-test/fixtures.local.json');
function persist(){fs.writeFileSync(output,JSON.stringify(saved,null,2)+'\n');}
persist();
for(let index=0;index<count;index++) {
    const movie=await api('POST','/api/v1/admin/movies',{title:prefix+' movie '+index,description:'Dataset for repeatable load tests',
        durationMinutes:120,ageRating:'P',releaseDate:'2026-10-01',status:'NOW_SHOWING'},admin,201);
    if(index<deletedCount) await api('DELETE','/api/v1/admin/movies/'+movie.id,undefined,admin,204);
    else saved.movieIds.push(movie.id);
    persist();
}
if(full) {
    const room=await api('POST','/api/v1/admin/auditoriums',{name:prefix,seatLayout:[{row:'A',from:1,to:2*vus+9,type:'STANDARD'}]},admin,201);
    const start=new Date(Date.now()+48*3600000);
    const showtime=await api('POST','/api/v1/admin/showtimes',{movieId:saved.movieIds[0],auditoriumId:room.id,startsAt:start.toISOString(),basePrice:90000},admin,201);
    const contention=await api('POST','/api/v1/admin/showtimes',{movieId:saved.movieIds[0],auditoriumId:room.id,
        startsAt:new Date(start.getTime()+24*3600000).toISOString(),basePrice:90000},admin,201);
    saved.showtimeId=showtime.id;saved.contentionShowtimeId=contention.id;
    saved.showtimeDate=start.toISOString().slice(0,10);
    const seats=[...room.seats].sort((a,b)=>a.number-b.number);
    if(seats.length<2*vus+9) throw new Error('Room response does not contain enough seats');
    saved.contentionSeatIds=seats.slice(2*vus,2*vus+9).map(seat=>seat.id);
    saved.loadUsers=[];saved.contenders=[];
    const password=process.env.LOAD_PASSWORD || 'load-test-123';
    for(let index=0;index<3*vus;index++) {
        const username=prefix+'-'+index;
        const user=await api('POST','/api/v1/auth/register',{username,password},undefined,201);
        const login=await api('POST','/api/v1/auth/login',{username,password});
        const entry={userId:user.id,username,token:login.accessToken};
        if(index<2*vus) saved.loadUsers.push({...entry,seatId:seats[index].id});else saved.contenders.push(entry);
        persist();
    }
}
persist();
console.log('Created '+count+' movies; '+deletedCount+' soft-deleted. Fixtures: '+output);
console.log('Fixtures contain tokens when scope=all. Regenerate them after login tokens expire.');
