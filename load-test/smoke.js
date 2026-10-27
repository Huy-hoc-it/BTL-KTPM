import { check, fail } from 'k6';
import { adminToken, data, parse, request, summary } from './lib/common.js';
export const options = { vus: 1, iterations: 1, thresholds: { checks: ['rate==1'] } };
export default function () {
    const admin = adminToken();
    let movieId;
    try {
        const created = request('POST','/api/v1/admin/movies',{ title: 'k6 smoke', description: 'Temporary movie',
            durationMinutes: 120, ageRating: 'P', releaseDate: '2026-10-01', status: 'NOW_SHOWING' },admin,[201]);
        const movie = data(created);
        if (!movie || !movie.id) fail('Cannot create smoke movie');
        movieId = movie.id;
        const read = request('GET','/api/v1/movies/' + movieId,null,admin);
        check(read, { 'Authenticated user reads created movie': r => data(r).id === movieId });
        const patched = request('PATCH','/api/v1/admin/movies/' + movieId,{ title: 'k6 updated', posterUrl: null },admin,[200]);
        check(patched, { 'PATCH keeps omitted duration': r => data(r).durationMinutes === 120 });
        request('POST','/api/v1/admin/movies',{},null,[401]);
        const bad = request('POST','/api/v1/admin/movies',{},admin,[422]);
        check(bad, { 'Validation includes field details': r => parse(r).error.details.length > 0 });
    } finally {
        if (movieId) {
            request('DELETE','/api/v1/admin/movies/' + movieId,null,admin,[204]);
            request('GET','/api/v1/movies/' + movieId,null,admin,[404]);
        }
    }
}
export function handleSummary(result) { return summary('smoke',result); }
