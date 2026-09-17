import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { stripTypeScriptTypes } from 'node:module';

const source = await readFile(new URL('../src/features/course/utils/regionScope.ts', import.meta.url), 'utf8');
const { matchesRegion } = await import(`data:text/javascript;base64,${Buffer.from(stripTypeScriptTypes(source)).toString('base64')}`);

test('selected district excludes other districts and road-name substring matches', () => {
    assert.equal(matchesRegion({ areaCode: '1', address: '서울특별시 광진구 능동로 1' }, '1', '광진구'), true);
    assert.equal(matchesRegion({ areaCode: '1', address: '서울특별시 성동구 광진구로 1' }, '1', '광진구'), false);
    assert.equal(matchesRegion({ areaCode: '1', address: '서울특별시 강남구 도산대로 1' }, '1', '남구'), false);
    assert.equal(matchesRegion({ areaCode: '6', address: '부산광역시 중구 중앙대로 1' }, '1', '중구'), false);
    assert.equal(matchesRegion({ areaCode: '1', address: null }, '1', '광진구'), false);
});

test('city selection includes its wards and clearing district restores the province', () => {
    const place = { areaCode: '31', address: '경기도 수원시 영통구 광교로 1' };
    assert.equal(matchesRegion(place, '31', '수원시'), true);
    assert.equal(matchesRegion(place, '31', '용인시'), false);
    assert.equal(matchesRegion(place, '31'), true);
    assert.equal(matchesRegion({ areaCode: '8', address: '세종특별자치시 한누리대로 1' }, '8', '세종특별자치시'), true);
});
