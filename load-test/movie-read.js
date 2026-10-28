import { check, fail, sleep } from 'k6';
import exec from 'k6/execution';
import { constantOptions, fixture, request, data, parse, summary } from './lib/common.js';
const saved = fixture();
export const options = constantOptions();
export function setup() {
    let ids = saved && saved.movieIds;
    if (!ids || !ids.length) {
        const response = request('GET','/api/v1/movies?pageSize=100',null,null,[200],'setup','setup');
        const movies = data(response);
        if (!Array.isArray(movies)) fail('Movie list did not return data[]');
        ids = movies.map(movie => movie.id);
    }
    if (!ids.length) fail('Seed at least one undeleted movie');
    return { ids: ids.slice(0,100) };
}
export function traffic(context) {
    const ids = context.ids;
    const list = Math.random() < 0.5;
    const id = ids[Math.floor(Math.random() * ids.length)];
    const response = list
        ? request('GET','/api/v1/movies?page=1&pageSize=20',null,null)
        : request('GET','/api/v1/movies/' + id,null,null);
    check(response, { 'Movie JSON matches contract': r => {
        const body = parse(r);
        return !!body && (list ? Array.isArray(body.data) && !!body.meta : !!body.data && body.data.id === id);
    } }, { phase: exec.scenario.name });
    sleep(0.1);
}
export function handleSummary(result) { return summary('movie-read',result); }
