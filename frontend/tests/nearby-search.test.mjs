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
const { searchAllNearbyPlaces } = await import(`data:text/javascript;base64,${Buffer.from(stripTypeScriptTypes(source)).toString('base64')}`);

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
