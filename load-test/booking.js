import exec from 'k6/execution';
import { fail, sleep } from 'k6';
import { VUS, constantOptions, fixture, request, data, cleanupUsers, summary } from './lib/common.js';
const saved = fixture(true);
export const options = constantOptions(true);
const bookingsByUser = {};
export function setup() {
    if (!saved.loadUsers || saved.loadUsers.length < 2 * VUS || !saved.showtimeId || !saved.movieIds.length)
        fail('Full fixtures need two separate groups of VUS users/seats for warmup and measurement');
    const chosen = saved.loadUsers.slice(0,2*VUS);
    if (new Set(chosen.map(user => user.userId)).size !== chosen.length) fail('Load users must be distinct');
    if (new Set(chosen.map(user => user.seatId)).size !== chosen.length) fail('Load seats must be distinct');
    for (const user of chosen) {
        const profile = data(request('GET','/api/v1/users/me',null,user.token,[200],'setup','setup'));
        if (!profile || profile.id !== user.userId) fail('Fixture user does not match its token');
    }
    cleanupUsers(chosen);
}
export function traffic() {
    const userIndex = (exec.scenario.name === 'measure' ? VUS : 0) + (exec.vu.idInTest - 1) % VUS;
    const user = saved.loadUsers[userIndex];
    const position = exec.scenario.iterationInTest % 20;
    if (position < 7) request('GET','/api/v1/movies?page=1&pageSize=20');
    else if (position < 12) request('GET','/api/v1/showtimes?movieId=' + saved.movieIds[0] +
        '&date=' + encodeURIComponent(saved.showtimeDate),null,null);
    else if (position < 17) request('GET','/api/v1/showtimes/' + saved.showtimeId + '/seats',null,null);
    else if (position < 19) {
        const lastBooking = bookingsByUser[user.userId];
        if (lastBooking) {
            const cancelled = request('DELETE','/api/v1/bookings/' + lastBooking,null,user.token,[204],'cleanup','cleanup');
            if (cancelled.status !== 204) fail('Cannot release the previous fixture booking');
        }
        delete bookingsByUser[user.userId];
        const response = request('POST','/api/v1/bookings',{ showtimeId: saved.showtimeId, seatIds: [user.seatId] },user.token,[201],'booking');
        const result = data(response);
        if (response.status === 201 && result && result.bookingId) bookingsByUser[user.userId] = result.bookingId;
        else fail('Booking did not return its ID');
    } else request('GET','/api/v1/bookings',null,user.token);
    sleep(0.1);
}
export function teardown() { cleanupUsers(saved.loadUsers.slice(0,2*VUS)); }
export function handleSummary(result) { return summary('booking',result); }
