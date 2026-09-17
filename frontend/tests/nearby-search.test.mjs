/* 주변 검색 취소 후 늦은 응답 처리와, 제한된 표시 목록·전체 건수의 구분을 검증한다. */
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { stripTypeScriptTypes } from 'node:module';

const nativeFetch = globalThis.fetch;
afterEach(() => { globalThis.fetch = nativeFetch; });
const source = (await readFile(new URL('../src/features/place/api/placeApi.ts', import.meta.url), 'utf8'))
    .replace('import { API_URL } from "../../../shared/api/config";', 'const API_URL = "https://api.petway.test";')
    .replace('import { apiFetch } from "../../../shared/api/client";', 'const apiFetch = (...args) => globalThis.fetch(...args);');
const { searchAllPlaces, searchAllNearbyPlaces, loadPlaceCompatibilities } = await import(`data:text/javascript;base64,${Buffer.from(stripTypeScriptTypes(source)).toString('base64')}`);

test('changing travel region cancels area pagination even when the old response arrives late', async () => {
    const controller = new AbortController();
    let requests = 0;
    globalThis.fetch = async (_url, options) => {
        requests++;
        assert.equal(options.signal, controller.signal);
        controller.abort();
        return Response.json({ totalCount: 300, items: [{ contentId: 'old-region' }] });
    };
    await assert.rejects(searchAllPlaces('1', controller.signal), { name: 'AbortError' });
    assert.equal(requests, 1);
});

test('automatic compatibility checks bound concurrency, deduplicate places and retain per-place errors', async () => {
    let active = 0;
    let peak = 0;
    let requests = 0;
    globalThis.fetch = async (url) => {
        active++;
        peak = Math.max(peak, active);
        requests++;
        await new Promise((resolve) => setImmediate(resolve));
        active--;
        return url.includes('/bad/')
            ? Response.json({ message: 'Provider unavailable' }, { status: 502 })
            : Response.json({ contentId: url.split('/').at(-2), petwayAssessment: { status: 'AVAILABLE' } });
    };
    const results = new Map();
    await loadPlaceCompatibilities(['a', 'b', 'bad', 'c', 'd', 'e', 'a'], new AbortController().signal,
        (id, entry) => results.set(id, entry));
    assert.equal(requests, 6);
    assert.equal(peak, 4);
    assert.equal(results.get('bad').error, 'Provider unavailable');
    assert.equal(results.get('e').result.petwayAssessment.status, 'AVAILABLE');
});

test('canceled compatibility checks cannot publish stale results or start queued requests', async () => {
    const controller = new AbortController();
    let requests = 0;
    let results = 0;
    globalThis.fetch = async () => {
        requests++;
        controller.abort();
        return Response.json({ contentId: 'old-user' });
    };
    await loadPlaceCompatibilities(['a', 'b', 'c', 'd', 'e'], controller.signal, () => results++);
    assert.equal(requests, 1);
    assert.equal(results, 0);
});

test('missing primary pet stops further provider requests without calling it a place restriction', async () => {
    let requests = 0;
    globalThis.fetch = async () => {
        requests++;
        return Response.json({ message: '대표 반려견이 없습니다.' }, { status: 409 });
    };
    const results = [];
    await loadPlaceCompatibilities(Array.from({ length: 20 }, (_, i) => String(i)), new AbortController().signal,
        (_id, entry) => results.push(entry));
    assert.ok(requests <= 4);
    assert.equal(results.length, 20);
    assert.ok(results.every((entry) => entry.error === '대표 반려견이 없습니다.' && !entry.result));
});

test('canceled search never publishes a late response or starts the next page', async () => {
    const controller = new AbortController();
    let requests = 0;
    let progress = 0;
    globalThis.fetch = async (_url, init) => {
        requests++;
        assert.equal(init.signal, controller.signal);
        controller.abort();
        // Simulate a response that was already received when cancellation occurred.
        return Response.json({ page: 1, size: 100, totalCount: 200, items: [{ contentId: 'late' }] });
    };
    await assert.rejects(searchAllNearbyPlaces(37.5665, 126.9780, 5000, () => progress++, {}, controller.signal), { name: 'AbortError' });
    assert.equal(requests, 1);
    assert.equal(progress, 0);
});

test('keeps full result count distinct from a deliberately limited displayed subset', async () => {
    let requests = 0;
    globalThis.fetch = async (_url, init) => {
        requests++;
        assert.ok(init.signal instanceof AbortSignal);
        return Response.json({ page: requests, size: 100, totalCount: 300,
            items: Array.from({ length: 100 }, (_, index) => ({ contentId: `${requests}-${index}` })) });
    };
    const result = await searchAllNearbyPlaces(37.5665, 126.9780, 5000, undefined,
        { targetUniqueCount: 100 }, new AbortController().signal);
    assert.equal(requests, 1);
    assert.equal(result.totalCount, 300);
    assert.equal(result.items.length, 100);
});
