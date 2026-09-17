import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { stripTypeScriptTypes } from 'node:module';

const source = await readFile(new URL('../src/features/place/utils/compatibilityReasons.ts', import.meta.url), 'utf8');
const { compatibilityReasonSections } = await import(`data:text/javascript;base64,${Buffer.from(stripTypeScriptTypes(source)).toString('base64')}`);

test('allowance stays outside confirmation section even when overall status needs confirmation', () => {
    const allowed = ['전 구역에서 반려견과 동반할 수 있습니다.', '모든 견종의 동반이 허용됩니다.'];
    const confirmation = ['방문 전 전화 문의가 필요합니다.'];
    const sections = compatibilityReasonSections({
        status: 'NEED_CONFIRMATION', reasons: [...confirmation, ...allowed],
        reasonGroups: { unavailable: [], confirmation, conditions: [], allowed },
    });
    assert.deepEqual(sections, [
        { title: '방문 전 확인할 내용', reasons: confirmation },
        { title: '동반 허용 안내', reasons: [allowed[0]] },
    ]);
});

test('older server response uses a neutral heading instead of mislabeling all reasons', () => {
    assert.deepEqual(compatibilityReasonSections({ status: 'NEED_CONFIRMATION', reasons: ['동반 허용'] }),
        [{ title: '판정 근거', reasons: ['동반 허용'] }]);
});

test('legacy blanket breed allowance is hidden without dropping the actual confirmation reason', () => {
    assert.deepEqual(compatibilityReasonSections({
        reasons: ['모든 견종의 동반이 허용됩니다.', '전화 문의 필요'],
    }), [{ title: '판정 근거', reasons: ['전화 문의 필요'] }]);
    assert.deepEqual(compatibilityReasonSections({ reasons: ['모든 견종의 동반이 허용됩니다.'] }), []);
});
