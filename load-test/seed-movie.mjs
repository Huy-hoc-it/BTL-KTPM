import fs from 'node:fs';
import path from 'node:path';

const base = (process.env.BASE_URL || 'http://127.0.0.1:8080').replace(/\/$/, '');
const count = Number(process.env.MOVIE_COUNT || 1000);
const deletedCount = Number(process.env.DELETED_COUNT || 50);
if (!Number.isInteger(count) || count < 1 || deletedCount < 0 || deletedCount >= count) {
    throw new Error('Use MOVIE_COUNT > DELETED_COUNT >= 0');
}

async function api(method, route, body, token, expected = 200) {
    const response = await fetch(base + route, {
        method,
        headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) },
        body: body === undefined ? undefined : JSON.stringify(body),
    });
    const result = response.status === 204 ? null : await response.json();
    if (response.status !== expected) throw new Error(method + ' ' + route + ' returned ' + response.status);
    return result && (result.data === undefined ? result : result.data);
}

let admin = process.env.ADMIN_TOKEN;
if (!admin) {
    if (!process.env.ADMIN_USERNAME || !process.env.ADMIN_PASSWORD) {
        throw new Error('Set ADMIN_TOKEN or ADMIN_USERNAME and ADMIN_PASSWORD');
    }
    admin = (await api('POST', '/api/v1/auth/login', {
        username: process.env.ADMIN_USERNAME,
        password: process.env.ADMIN_PASSWORD,
    })).accessToken;
}

const fixture = {
    baseUrl: base,
    createdAt: new Date().toISOString(),
    scope: 'movie',
    movieIds: [],
    createdCount: count,
    deletedCount,
};
const output = path.resolve(process.env.FIXTURES || 'load-test/fixtures.local.json');
fs.mkdirSync(path.dirname(output), { recursive: true });
const persist = () => fs.writeFileSync(output, JSON.stringify(fixture, null, 2) + '\n');
const prefix = 'load-' + Date.now().toString(36);
persist();

for (let index = 0; index < count; index++) {
    const movie = await api('POST', '/api/v1/admin/movies', {
        title: prefix + ' movie ' + index,
        description: 'Dataset for repeatable Movie read tests',
        durationMinutes: 120,
        ageRating: 'P',
        releaseDate: '2026-10-01',
        status: 'NOW_SHOWING',
    }, admin, 201);
    if (index < deletedCount) {
        await api('DELETE', '/api/v1/admin/movies/' + movie.id, undefined, admin, 204);
    } else {
        fixture.movieIds.push(movie.id);
    }
    persist();
}

console.log('Created ' + count + ' movies; ' + deletedCount + ' soft-deleted. Fixtures: ' + output);
console.log('Fixture contains an Admin token for authenticated Movie reads; regenerate it after the token expires.');
