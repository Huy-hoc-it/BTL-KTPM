import fs from 'node:fs';
const saved=JSON.parse(fs.readFileSync(process.env.FIXTURES || 'load-test/fixtures.local.json','utf8'));
const base=(process.env.BASE_URL || saved.baseUrl).replace(/\/$/,'');
const users=[...(saved.loadUsers || [])];
if(process.argv.includes('--after-contention-db-check')) users.push(...(saved.contenders || []));
for(const user of users) {
    const response=await fetch(base+'/api/v1/bookings',{headers:{Authorization:'Bearer '+user.token}});
    if(response.status!==200) throw new Error('Cannot list bookings for fixture user');
    const result=await response.json();
    const bookings=Array.isArray(result)?result:result.data;
    if(!Array.isArray(bookings)) throw new Error('Booking list did not return an array');
    for(const booking of bookings) {
        if(!booking.bookingId) throw new Error('Booking list did not return bookingId');
        const cancelled=await fetch(base+'/api/v1/bookings/'+booking.bookingId,{method:'DELETE',headers:{Authorization:'Bearer '+user.token}});
        if(cancelled.status!==204) throw new Error('Cannot cancel fixture booking');
    }
}
console.log('Cancelled bookings owned by dedicated fixture users. Seeded movies, rooms and users are retained.');
