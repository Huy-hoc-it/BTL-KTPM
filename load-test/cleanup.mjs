import fs from 'node:fs';
const saved=JSON.parse(fs.readFileSync(process.env.FIXTURES || 'load-test/fixtures.local.json','utf8'));
const base=(process.env.BASE_URL || saved.baseUrl).replace(/\/$/,'');
const users=[...(saved.loadUsers || [])];
if(process.argv.includes('--after-contention-db-check')) users.push(...(saved.contenders || []));
for(const user of users) {
    let page=1,pages=1;
    do {
        const response=await fetch(base+'/api/v1/bookings/me?page='+page+'&pageSize=100',{headers:{Authorization:'Bearer '+user.token}});
        if(response.status!==200) throw new Error('Cannot list bookings for fixture user');
        const result=await response.json(); pages=result.meta?.totalPages || 1;
        for(const booking of result.data) {
            if(booking.status!=='CONFIRMED') continue;
            if(![saved.showtimeId,saved.contentionShowtimeId].includes(booking.showtimeId))
                throw new Error('Refusing to cancel a booking outside fixture showtimes');
            const cancelled=await fetch(base+'/api/v1/bookings/'+booking.bookingId,{method:'DELETE',headers:{Authorization:'Bearer '+user.token}});
            if(cancelled.status!==204) throw new Error('Cannot cancel fixture booking');
        }
        page++;
    } while(page<=pages);
}
console.log('Cancelled active bookings owned by fixture users. Seeded movies, rooms and users are retained.');
