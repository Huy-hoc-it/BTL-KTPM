import exec from 'k6/execution';
import { check, fail } from 'k6';
import { Counter } from 'k6/metrics';
import { VUS, fixture, request, parse, data, summary } from './lib/common.js';
const saved = fixture(true);
const runIndex = Number(__ENV.CONTENTION_RUN_INDEX || 1);
const winners = new Counter('booking_winners');
const losers = new Counter('expected_seat_conflicts');
const unexpected = new Counter('unexpected_contention_outcomes');
export const options = {
    scenarios: { measure: { executor: 'per-vu-iterations', vus: VUS, iterations: 1, maxDuration: '30s' } },
    thresholds: { booking_winners: ['count==1'], expected_seat_conflicts: ['count==' + (VUS-1)],
        unexpected_contention_outcomes: ['count==0'], checks: ['rate==1'] },
    summaryTrendStats: ['avg','med','p(95)','p(99)','max']
};
export function setup() {
    if (!Number.isInteger(runIndex) || runIndex < 1 || runIndex > 9) fail('CONTENTION_RUN_INDEX must be from 1 to 9');
    if (!saved.contenders || saved.contenders.length < VUS || !saved.contentionShowtimeId || !saved.contentionSeatIds || !saved.contentionSeatIds[runIndex-1])
        fail('Missing contention fixtures');
    if (new Set(saved.contenders.slice(0,VUS).map(user => user.userId)).size !== VUS) fail('Use distinct contention users');
    for (const user of saved.contenders.slice(0,VUS)) {
        const profile = data(request('GET','/api/v1/users/me',null,user.token,[200],'setup','setup'));
        if (!profile || profile.id !== user.userId) fail('Fixture user does not match its token');
    }
}
export default function () {
    const user = saved.contenders[(exec.vu.idInTest-1) % VUS];
    const response = request('POST','/api/v1/bookings',{ showtimeId: saved.contentionShowtimeId,
        seatIds: [saved.contentionSeatIds[runIndex-1]] },user.token,[201,409],'contention');
    const body = parse(response);
    const bookingId = body && body.data && (body.data.bookingId || body.data.id);
    const won = response.status === 201 && !!bookingId;
    const lost = response.status === 409 && body && body.error && body.error.code === 'SEAT_ALREADY_BOOKED';
    winners.add(won ? 1 : 0); losers.add(lost ? 1 : 0); unexpected.add(won || lost ? 0 : 1);
    check(response, { 'One winner or documented seat conflict': () => !!(won || lost) });
}
// Keep the winner until the database check has been completed.
export function handleSummary(result) { return summary('contention',result); }
