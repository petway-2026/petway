import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { stripTypeScriptTypes } from 'node:module';

const nativeFetch = globalThis.fetch;
const nativeWindow = globalThis.window;
afterEach(() => {
    globalThis.fetch = nativeFetch;
    if (nativeWindow === undefined) delete globalThis.window;
    else globalThis.window = nativeWindow;
});
let moduleId = 0;
async function client(apiUrl = 'https://api.petway.test') {
    const source = (await readFile(new URL('../src/shared/api/client.ts', import.meta.url), 'utf8'))
        .replace('import { API_URL } from "./config";', `const API_URL = ${JSON.stringify(apiUrl)};`);
    const code = stripTypeScriptTypes(source);
    return import(`data:text/javascript;base64,${Buffer.from(code).toString('base64')}#${moduleId++}`);
}
const json = (body, status = 200) => new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
const token = (value = 'token-1') => json({ token: value, headerName: 'X-CSRF-TOKEN' });
const api = 'https://api.petway.test';

test('concurrent mutations share one CSRF bootstrap and include credentials/header', async () => {
    const { apiFetch } = await client();
    let bootstraps = 0;
    let mutations = 0;
    globalThis.fetch = async (url, init) => {
        if (url.endsWith('/csrf')) {
            bootstraps++;
            assert.equal(init.credentials, 'include');
            return token();
        }
        mutations++;
        assert.equal(init.credentials, 'include');
        assert.equal(init.headers.get('X-CSRF-TOKEN'), 'token-1');
        assert.equal(init.headers.get('Content-Type'), 'application/json');
        return json({ ok: true });
    };
    await Promise.all([1, 2].map(() => apiFetch(`${api}/api/reviews`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{}',
    })));
    assert.equal(bootstraps, 1);
    assert.equal(mutations, 2);
});

test('refreshes and retries only the explicit pre-controller CSRF rejection', async () => {
    const { apiFetch } = await client();
    let bootstraps = 0;
    let mutations = 0;
    globalThis.fetch = async (url, init) => {
        if (url.endsWith('/csrf')) return token(`token-${++bootstraps}`);
        mutations++;
        if (mutations === 1) return json({ code: 'CSRF_INVALID' }, 403);
        assert.equal(init.headers.get('X-CSRF-TOKEN'), 'token-2');
        return json({ ok: true });
    };
    assert.equal((await apiFetch(`${api}/api/reviews`, { method: 'POST' })).status, 200);
    assert.equal(bootstraps, 2);
    assert.equal(mutations, 2);
});

test('never retries a permission denial or a server failure', async () => {
    for (const status of [403, 500]) {
        const { apiFetch } = await client();
        let mutations = 0;
        globalThis.fetch = async (url) => {
            if (url.endsWith('/csrf')) return token();
            mutations++;
            return json({ code: 'ACCESS_DENIED' }, status);
        };
        assert.equal((await apiFetch(`${api}/api/favorites`, { method: 'POST' })).status, status);
        assert.equal(mutations, 1);
    }
});

test('login and logout clear the token from the preceding session', async () => {
    const { apiFetch } = await client();
    let bootstraps = 0;
    globalThis.fetch = async (url) => url.endsWith('/csrf') ? token(`token-${++bootstraps}`) : json({});
    await apiFetch(`${api}/api/auth/login`, { method: 'POST' });
    await apiFetch(`${api}/api/reviews`, { method: 'POST' });
    await apiFetch(`${api}/api/auth/logout`, { method: 'POST' });
    await apiFetch(`${api}/api/auth/login`, { method: 'POST' });
    assert.equal(bootstraps, 3);
});

test('aborted mutation never reaches the mutation endpoint', async () => {
    const { apiFetch } = await client();
    let mutations = 0;
    globalThis.fetch = async (url) => {
        if (url.endsWith('/csrf')) return token();
        mutations++;
        return json({});
    };
    const controller = new AbortController();
    controller.abort();
    await assert.rejects(apiFetch(`${api}/api/favorites`, { method: 'POST', signal: controller.signal }), { name: 'AbortError' });
    assert.equal(mutations, 0);
});

test('GET keeps the abort signal and does not create an anonymous session', async () => {
    const { apiFetch } = await client();
    const controller = new AbortController();
    let requests = 0;
    globalThis.fetch = async (url, init) => {
        requests++;
        assert.equal(url, `${api}/api/places`);
        assert.equal(init.signal, controller.signal);
        return json({});
    };
    await apiFetch(`${api}/api/places`, { signal: controller.signal });
    assert.equal(requests, 1);
});

test('production relative API URL stays on the browser origin', async () => {
    const { apiFetch } = await client('');
    globalThis.window = { location: { origin: 'https://petway.test' } };
    globalThis.fetch = async (url) => {
        assert.equal(url, '/api/auth/me');
        return new Response(null, { status: 401 });
    };
    assert.equal((await apiFetch('/api/auth/me')).status, 401);
});

test('never sends session tokens to another origin', async () => {
    const { apiFetch } = await client();
    globalThis.fetch = () => { throw new Error('must not fetch'); };
    await assert.rejects(apiFetch('https://untrusted.example/api/reviews', { method: 'POST' }), /허용되지/);
});
