import http from 'k6/http';
import exec from 'k6/execution';
import { check, fail } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

export const BASE = (__ENV.BASE_URL || 'http://127.0.0.1:8080').replace(/\/$/, '');
export const VUS = Number(__ENV.VUS || 10);
export const WARMUP = Number(__ENV.WARMUP_SECONDS || 30);
export const DURATION = Number(__ENV.DURATION_SECONDS || 120);
const measuredRequests = new Counter('measured_requests');
const technicalErrors = new Rate('measured_technical_error');
const businessErrors = new Rate('measured_business_error');
const unexpected = new Counter('measured_unexpected');
const readLatency = new Trend('measured_read_latency', true);
const bookingLatency = new Trend('measured_booking_latency', true);

export function parse(response) {
    try { const value = response.json(); return value; } catch (_) { return null; }
}
export function data(response) { const value = parse(response); return value && (value.data === undefined ? value : value.data); }
export function request(method, path, payload, token, expected = [200], kind = 'read', phaseOverride) {
    const phase = phaseOverride || (exec.scenario.name === 'measure' ? 'measure' : 'warmup');
    const route = path.split('?')[0].replace(/[0-9a-f]{8}-[0-9a-f-]{27,}/g, ':id');
    const response = http.request(method, BASE + path, payload == null ? null : JSON.stringify(payload), {
        headers: Object.assign({ 'Content-Type': 'application/json' }, token ? { Authorization: 'Bearer ' + token } : {}),
        tags: { name: method + ' ' + route, phase: phase },
        responseCallback: http.expectedStatuses(...expected)
    });
    check(response, { 'HTTP status matches contract': r => expected.includes(r.status) }, { phase: phase });
    if (phase === 'measure') {
        measuredRequests.add(1);
        technicalErrors.add(response.status === 0 || response.status >= 500);
        businessErrors.add(response.status >= 400 && response.status < 500);
        unexpected.add(expected.includes(response.status) ? 0 : 1);
        if (kind === 'read') readLatency.add(response.timings.duration);
        if (kind === 'booking') bookingLatency.add(response.timings.duration);
    }
    return response;
}
export function constantOptions(includeBooking = false) {
    if (!Number.isInteger(VUS) || VUS < 1 || WARMUP < 0 || DURATION <= 0) fail('Invalid VUS or duration');
    const scenarios = {};
    if (WARMUP > 0) scenarios.warmup = { executor: 'constant-vus', vus: VUS, duration: WARMUP + 's', exec: 'traffic', gracefulStop: '0s' };
    scenarios.measure = { executor: 'constant-vus', vus: VUS, startTime: WARMUP + 's', duration: DURATION + 's', exec: 'traffic', gracefulStop: '5s' };
    const thresholds = {
        measured_technical_error: ['rate<0.01'],
        measured_business_error: ['rate<0.01'],
        measured_unexpected: ['count==0'],
        measured_read_latency: ['p(95)<' + (__ENV.READ_P95_MS || 500)],
        'checks{phase:measure}': ['rate==1']
    };
    if (includeBooking) thresholds.measured_booking_latency = ['p(95)<' + (__ENV.BOOKING_P95_MS || 1000)];
    return { scenarios: scenarios, thresholds: thresholds, summaryTrendStats: ['avg','med','p(95)','p(99)','max'] };
}
export function fixture(required = false) {
    if (!__ENV.FIXTURES) {
        if (required) fail('Set FIXTURES to the absolute path of generated fixtures.local.json');
        return null;
    }
    return JSON.parse(open(__ENV.FIXTURES));
}
export function adminToken() {
    if (__ENV.ADMIN_TOKEN) return __ENV.ADMIN_TOKEN;
    if (!__ENV.ADMIN_USERNAME || !__ENV.ADMIN_PASSWORD) fail('Set ADMIN_TOKEN or ADMIN_USERNAME and ADMIN_PASSWORD');
    const response = request('POST', '/api/v1/auth/login', { username: __ENV.ADMIN_USERNAME, password: __ENV.ADMIN_PASSWORD }, null, [200], 'setup', 'setup');
    const result = data(response);
    if (response.status !== 200 || !result || !result.accessToken) fail('Admin login failed');
    return result.accessToken;
}
export function cleanupUsers(users, allowedShowtimes) {
    for (const user of users) {
        let page = 1;
        let totalPages = 1;
        do {
            const response = request('GET', '/api/v1/bookings/me?page=' + page + '&pageSize=100', null,
                user.token, [200], 'cleanup', 'cleanup');
            const result = parse(response);
            if (response.status !== 200 || !result || !Array.isArray(result.data)) fail('Cannot list bookings for cleanup');
            totalPages = result.meta ? result.meta.totalPages : 1;
            for (const booking of result.data) {
                if (booking.status === 'CONFIRMED') {
                    if (!allowedShowtimes.includes(booking.showtimeId)) fail('Refusing to cancel bookings outside fixture showtimes');
                    const cancelled = request('DELETE', '/api/v1/bookings/' + booking.bookingId, null,
                        user.token, [204], 'cleanup', 'cleanup');
                    if (cancelled.status !== 204) fail('Fixture booking cleanup failed');
                }
            }
            page += 1;
        } while (page <= totalPages);
    }
}
export function summary(name, result) {
    const metric = key => result.metrics[key] ? result.metrics[key].values : {};
    const sustained = name === 'movie-read' || name === 'booking';
    const metadata = { script: name, measuredAt: new Date().toISOString(), baseUrl: BASE,
        gitCommit: __ENV.GIT_COMMIT || 'not recorded', vus: name === 'smoke' ? 1 : VUS, warmupSeconds: sustained ? WARMUP : 0,
        durationSeconds: sustained ? DURATION : null, measuredThroughput: sustained ? (metric('measured_requests').count || 0) / DURATION : null,
        model: name === 'movie-read' ? 'closed loop; one Movie request per iteration; think time 100ms'
            : name === 'booking' ? 'closed loop; one business request per iteration; think time 100ms'
                : 'functional or one-shot contention check',
        resources: 'collect separately; not included in k6 metrics' };
    const files = { stdout: name + ': measured requests=' + (metric('measured_requests').count || 0) +
        ', GET p95=' + (metric('measured_read_latency')['p(95)'] || 0).toFixed(2) + 'ms' +
        ', booking p95=' + (metric('measured_booking_latency')['p(95)'] || 0).toFixed(2) + 'ms' +
        ', business errors=' + ((metric('measured_business_error').rate || 0) * 100).toFixed(2) + '%\n' };
    files[__ENV.SUMMARY_PATH || 'load-test/results/' + name + '-' + Date.now() + '.json'] = JSON.stringify({ metadata: metadata, summary: result }, null, 2);
    return files;
}
