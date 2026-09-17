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

// Execute the page's actual loader with controlled API responses, including late responses.
const pageSource = await readFile(new URL('../src/features/course/pages/TravelCoursePage.tsx', import.meta.url), 'utf8');
const loaderSource = pageSource.slice(pageSource.indexOf('    const loadRecommendations ='), pageSource.indexOf('\n    useEffect(() => {', pageSource.indexOf('    const loadRecommendations =')));
function createLoader(searchAllPlaces, getPlaceCompatibility = async () => ({ petwayAssessment: { status: 'AVAILABLE' } })) {
    const state = {};
    const bindings = {
        searchAllPlaces, matchesRegion, AbortController,
        recommendationRequestRef: { current: null }, COMPATIBILITY_BATCH_SIZE: 6,
        distanceKm: () => 0,
        getPlaceCompatibility,
        ...Object.fromEntries(['RecommendationPage', 'Loading', 'Recommendations', 'RegionPlaceCount', 'RecommendationScope', 'ExpandedCompatibilityId', 'Message']
            .map((name) => [`set${name}`, (value) => { state[name] = value; }])),
    };
    const load = new Function(...Object.keys(bindings), stripTypeScriptTypes(loaderSource) + '\nreturn loadRecommendations;')(...Object.values(bindings));
    return { load, state };
}
const center = { latitude: 33.5, longitude: 126.5 };
const jejuPlaces = [
    { contentId: 'jeju', areaCode: '39', address: '제주특별자치도 제주시 중앙로 1', ...center },
    { contentId: 'seogwipo', areaCode: '39', address: '제주특별자치도 서귀포시 중앙로 1', ...center },
];

test('course loader limits cards and total count to the selected city, then restores all cities', async () => {
    const { load, state } = createLoader(async () => ({ items: jejuPlaces }));
    await load('39', center, '제주특별자치도 제주시', '제주시');
    assert.deepEqual(state.Recommendations.map((place) => place.contentId), ['jeju']);
    assert.equal(state.RegionPlaceCount, 1);
    await load('39', center, '제주특별자치도 서귀포시', '서귀포시');
    assert.deepEqual(state.Recommendations.map((place) => place.contentId), ['seogwipo']);
    assert.equal(state.RegionPlaceCount, 1);
    await load('39', center, '제주특별자치도');
    assert.equal(state.Recommendations.length, 2);
    assert.equal(state.RegionPlaceCount, 2);
    await load('39', center, '없는 시군구', '없는시');
    assert.deepEqual(state.Recommendations, []);
    assert.equal(state.RegionPlaceCount, 0);
});

test('late province response cannot replace the selected district cards or count', async () => {
    let finishProvince;
    const { load, state } = createLoader(() => finishProvince
        ? Promise.resolve({ items: jejuPlaces })
        : new Promise((resolve) => { finishProvince = resolve; }));
    const provinceRequest = load('39', center, '제주특별자치도');
    await load('39', center, '제주특별자치도 서귀포시', '서귀포시');
    finishProvince({ items: jejuPlaces });
    await provinceRequest;
    assert.deepEqual(state.Recommendations.map((place) => place.contentId), ['seogwipo']);
    assert.equal(state.RegionPlaceCount, 1);
    assert.match(state.Message, /서귀포시/);
});

test('selection-driven reload passes the current district and cancels on cleanup', () => {
    const start = pageSource.indexOf('    useEffect(() => {', pageSource.indexOf('    const loadRecommendations ='));
    const end = pageSource.indexOf('\n    useEffect(() => {', start + 1);
    const effectSource = pageSource.slice(start, end);
    const calls = [];
    let cleanup;
    let dependencies;
    const controller = new AbortController();
    const bindings = {
        useEffect: (effect, deps) => { dependencies = deps; cleanup = effect(); },
        REGIONS: [{ code: '39', name: '제주특별자치도', center }],
        DISTRICTS: { '39': [{ name: '서귀포시' }] },
        regionCode: '39', districtName: '서귀포시',
        districtCenters: {}, setOrigin: () => {},
        locationLabel: '제주특별자치도 서귀포시', includeNearbyDistricts: false, nearbyDistricts: [],
        loadRecommendations: (...args) => calls.push(args),
        recommendationRequestRef: { current: controller },
    };
    new Function(...Object.keys(bindings), effectSource)(...Object.values(bindings));
    assert.deepEqual(dependencies, ['39', '서귀포시', false]);
    assert.deepEqual(calls, [['39', center, '제주특별자치도 서귀포시', '서귀포시', []]]);
    cleanup();
    assert.equal(controller.signal.aborted, true);
});

const scopeSource = await readFile(new URL('../src/features/course/utils/recommendationScope.ts', import.meta.url), 'utf8');
const { groupRecommendations } = await import(`data:text/javascript;base64,${Buffer.from(stripTypeScriptTypes(scopeSource)).toString('base64')}`);

test('all regional places survive missing coordinates and failed assessments, with exhaustive counts', async () => {
    const statuses = ['AVAILABLE', 'CONDITIONAL', 'NEED_CONFIRMATION', 'UNAVAILABLE', null];
    const items = statuses.map((status, index) => ({
        ...jejuPlaces[0], contentId: String(index), contentTypeId: index === 0 ? '32' : '12',
        latitude: index === 2 ? null : center.latitude,
    }));
    const { load, state } = createLoader(async () => ({ items }), async (id) => {
        if (!statuses[Number(id)]) throw new Error('Provider unavailable');
        return { petwayAssessment: { status: statuses[Number(id)] } };
    });
    await load('39', center, 'Jeju');
    assert.equal(state.RegionPlaceCount, 5);
    assert.equal(state.Recommendations.length, 5);
    assert.match(state.Recommendations.find((place) => place.contentId === '4').compatibilityError, /Provider unavailable/);
    const groups = groupRecommendations(state.Recommendations, 'all');
    assert.deepEqual(Object.values(groups).map((places) => places.length), [1, 3, 1]);
    assert.equal(new Set(Object.values(groups).flat().map((place) => place.contentId)).size, 5);
    assert.equal(groups.AVAILABLE[0].contentTypeId, '32');
    assert.ok(groups.CONFIRMATION.some((place) => place.latitude === null));
    const filtered = groupRecommendations(state.Recommendations, '12');
    assert.deepEqual(Object.values(filtered).map((places) => places.length), [0, 3, 1]);
    assert.equal(Object.values(filtered).flat().length, 4);
});

const centersSource = await readFile(new URL('../src/features/course/utils/districtCenters.ts', import.meta.url), 'utf8');
const nearbySource = (await readFile(new URL('../src/features/course/utils/nearbyDistricts.ts', import.meta.url), 'utf8'))
    .replace('import { districtCenters } from "./districtCenters";', centersSource);
const { getNearbyDistricts } = await import(`data:text/javascript;base64,${Buffer.from(stripTypeScriptTypes(nearbySource)).toString('base64')}`);

test('Sokcho expands to the requested coastal cities and clearing expansion restores only Sokcho', async () => {
    const districts = ['속초시', '고성군', '양양군', '강릉시', '춘천시'];
    const nearby = getNearbyDistricts('32', '속초시', districts);
    assert.deepEqual(nearby, ['고성군', '양양군', '강릉시']);
    const items = districts.map((name) => ({ ...jejuPlaces[0], contentId: name, areaCode: '32', address: `강원특별자치도 ${name} 중앙로 1`, contentTypeId: '12' }));
    items.push({ ...items[1], contentId: '경남고성', areaCode: '36', address: '경상남도 고성군 중앙로 1' });
    const { load, state } = createLoader(async () => ({ items }));
    await load('32', center, '속초 및 인근', '속초시', nearby);
    assert.deepEqual(state.Recommendations.map((place) => place.contentId), districts.slice(0, 4));
    assert.equal(state.RegionPlaceCount, 4);
    assert.equal(Object.values(groupRecommendations(state.Recommendations, 'all')).flat().length, 4);
    await load('32', center, '속초시', '속초시');
    assert.deepEqual(state.Recommendations.map((place) => place.contentId), ['속초시']);
    assert.equal(state.RegionPlaceCount, 1);
});

test('nearby suggestions stay within the selector and do not expand an empty or unknown district', () => {
    assert.deepEqual(getNearbyDistricts('32', '', ['속초시']), []);
    assert.deepEqual(getNearbyDistricts('32', '없는시', ['속초시']), []);
    const nearby = getNearbyDistricts('31', '수원시', ['수원시', '화성시', '용인시', '오산시', '의왕시', '없는시']);
    assert.equal(nearby.length, 3);
    assert.ok(nearby.every((name) => ['화성시', '용인시', '오산시', '의왕시'].includes(name)));
    assert.deepEqual(getNearbyDistricts('35', '울릉군', ['울릉군', '포항시']), []);
});

test('late expanded response cannot restore nearby cities after unchecking', async () => {
    const items = ['속초시', '양양군'].map((name) => ({ ...jejuPlaces[0], contentId: name, areaCode: '32', address: `강원특별자치도 ${name} 중앙로 1` }));
    let finishExpanded;
    const { load, state } = createLoader(() => finishExpanded ? Promise.resolve({ items }) : new Promise((resolve) => { finishExpanded = resolve; }));
    const expanded = load('32', center, '속초 및 인근', '속초시', ['양양군']);
    await load('32', center, '속초시', '속초시');
    finishExpanded({ items });
    await expanded;
    assert.deepEqual(state.Recommendations.map((place) => place.contentId), ['속초시']);
    assert.equal(state.RegionPlaceCount, 1);
});
