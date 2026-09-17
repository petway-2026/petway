package com.petway.petrule;

import com.petway.petrule.PetRule.RequirementType;

import java.util.List;
import java.util.regex.Pattern;

/** Vocabulary only; contextual parsing and final decisions live elsewhere. */
final class PetRulePatterns {
    private PetRulePatterns() {}

    static final String ENTRY_ACTION = "(?:동반|출입|입장|입실|이용)";

    // 중소형견처럼 긴 표현을 먼저 매칭해 소형견 하나로 잘못 쪼개지 않게 한다.
    static final String SIZE_EXPRESSION =
            "(중소형견|소형견|중형견|대형견)";

    static final Pattern WEIGHT_PATTERN = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*(?:kg|㎏)\\s*(이하|미만)",
            Pattern.CASE_INSENSITIVE
    );
    static final Pattern WEIGHT_RANGE_PATTERN = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*(?:kg|㎏)?\\s*[~～-]\\s*(\\d+(?:\\.\\d+)?)\\s*(?:kg|㎏)",
            Pattern.CASE_INSENSITIVE
    );

    static final Pattern PROHIBITED_SIZE_PATTERN = Pattern.compile(
            SIZE_EXPRESSION + "\\s*(?:은|는|이|가)?\\s*"
                    + ENTRY_ACTION + "?\\s*(?:불가|금지)"
    );

    static final Pattern ALLOWED_SIZE_PATTERN = Pattern.compile(
            SIZE_EXPRESSION + "\\s*(?:은|는|이|가)?\\s*"
                    + "(?:(?:만\\s*)?(?:" + ENTRY_ACTION + "\\s*)?(?:가능|허용)|만)"
    );

    static final Pattern BARE_SIZE_PATTERN = Pattern.compile(
            "^\\s*" + SIZE_EXPRESSION + "\\s*$"
    );

    static final Pattern CAPACITY_SIZE_LABEL_PATTERN = Pattern.compile(
            "(소형|중형|대형)(?:견)?(?=\\s*(?:$|[,/·(]|\\d))"
    );

    // KTO data often abbreviates "중형견·소형견" as "중, 소형견".
    static final Pattern MEDIUM_SMALL_LIST_PATTERN = Pattern.compile(
            "중\\s*[,·/]\\s*소형견|소형견\\s*[,·/]\\s*중형견"
    );

    static final Pattern ALL_BREEDS_ALLOWED_PATTERN = Pattern.compile(
            "(?:전|모든)\\s*견종\\s*(?:은|는|이|가)?\\s*"
                    + ENTRY_ACTION + "?\\s*(?:가능|허용)"
    );

    static final Pattern ALL_AREAS_PATTERN = Pattern.compile(
            "전\\s*구역\\s*(?:에서\\s*)?" + ENTRY_ACTION + "?\\s*(?:가능|허용)"
    );

    static final Pattern PARTIAL_AREA_PATTERN = Pattern.compile(
            "일부\\s*구역\\s*(?:에서\\s*)?(?:만\\s*)?"
                    + ENTRY_ACTION + "?\\s*(?:가능|허용)"
    );

    static final Pattern TERRACE_ONLY_PATTERN = Pattern.compile(
            "(?:야외\\s*)?테라스\\s*(?:에서\\s*)?만\\s*"
                    + ENTRY_ACTION + "?\\s*(?:가능|허용)?"
    );

    static final Pattern OUTDOOR_ONLY_PATTERN = Pattern.compile(
            "(?:야외|실외|외부)(?:\\s*공간)?\\s*(?:에서\\s*)?만\\s*"
                    + ENTRY_ACTION + "?\\s*(?:가능|허용)?"
    );

    static final Pattern INDOOR_PATTERN = Pattern.compile(
            "실내\\s*" + ENTRY_ACTION + "?\\s*(가능|허용|불가|금지)"
    );

    static final Pattern OUTDOOR_PATTERN = Pattern.compile(
            "(?:야외|실외|외부)\\s*" + ENTRY_ACTION + "?\\s*(가능|허용|불가|금지)"
    );

    static final Pattern UNSAFE_POSITIVE_SPACE_PATTERN = Pattern.compile(
            "(?:불가|금지|안\\s*됨|불가능|여부|문의|확인\\s*필요|아님|아니|하지\\s*않)"
    );

    static final Pattern NEGATED_REQUIREMENT_PATTERN = Pattern.compile(
            "(?:불필요|필요\\s*없음|필수\\s*아님|안\\s*함|하지\\s*않음|"
                    + "미착용(?:\\s*가능)?|없음)"
    );

    static final Pattern LEASH_NEED_PATTERN = Pattern.compile(
            "(?:목줄|리드줄)\\s*(?:착용|사용|지참)?\\s*(?:필수|필요)?"
    );

    static final Pattern LEASH_EXPLICIT_PATTERN = Pattern.compile(
            "(?:목줄|리드줄)\\s*(?:착용|사용|지참)?\\s*(?:필수|필요)"
    );

    static final Pattern MUZZLE_NEED_PATTERN = Pattern.compile(
            "입마개\\s*(?:착용|사용|지참)?\\s*(?:필수|필요)?"
    );

    static final Pattern MUZZLE_EXPLICIT_PATTERN = Pattern.compile(
            "입마개\\s*(?:착용|사용|지참)?\\s*(?:필수|필요)"
    );

    static final Pattern WASTE_BAG_NEED_PATTERN = Pattern.compile(
            "배변\\s*봉투\\s*(?:지참|준비|휴대)?\\s*(?:필수|필요)?"
    );

    static final Pattern WASTE_BAG_EXPLICIT_PATTERN = Pattern.compile(
            "배변\\s*봉투\\s*(?:(?:지참|준비|휴대)(?:\\s*(?:필수|필요))?|필수|필요)"
    );

    static final Pattern WASTE_DISPOSAL_NEED_PATTERN = Pattern.compile(
            "배변\\s*처리\\s*(?:필수|필요)?"
    );

    static final Pattern WASTE_DISPOSAL_EXPLICIT_PATTERN = Pattern.compile(
            "배변\\s*처리\\s*(?:필수|필요)"
    );

    static final String CARRIER_EXPRESSION =
            "(?:이동장|캐리어|켄넬|케이지)"
                    + "(?:\\s*\\(\\s*(?:이동장|캐리어|켄넬|케이지)\\s*\\))?";

    static final Pattern CARRIER_NEED_PATTERN = Pattern.compile(
            CARRIER_EXPRESSION
                    + "\\s*(?:사용|이용|착용|지참)?\\s*(?:필수|필요)?"
    );

    static final Pattern CARRIER_EXPLICIT_PATTERN = Pattern.compile(
            CARRIER_EXPRESSION + "\\s*(?:사용|이용|착용|지참)?\\s*(?:필수|필요)"
    );

    static final Pattern DANGEROUS_BREEDS_PROHIBITED_PATTERN = Pattern.compile(
            "맹견(?:류)?\\s*(?:은|는|이|가)?\\s*" + ENTRY_ACTION + "?\\s*(?:불가|금지)"
    );

    static final Pattern DAY_DEPENDENT_RULE_PATTERN = Pattern.compile(
            "(?:월|화|수|목|금|토|일)요일|주말|평일|공휴일"
    );

    static final Pattern CONDITIONAL_PROHIBITION_PREFIX = Pattern.compile(
            "실내|실외|야외|테라스|루프탑|경우|미착용|초과|이상|이하|미만|요일|주말|평일|공휴일"
    );

    static final Pattern NEGATED_PROHIBITION_SUFFIX = Pattern.compile(
            "^\\s*(?:가\\s*)?(?:아님|아니|하지\\s*않)"
    );

    // 이동장 수용 조건 전체를 준비사항으로 읽어 동물 허용 조건의 미해석 문구로 남기지 않는다.
    static final Pattern CARRIER_FULL_BODY_PATTERN = Pattern.compile(
            CARRIER_EXPRESSION + "\\s*(?:안|내부|내)에\\s*"
                    + "(?:반려동물|반려견)(?:의)?\\s*(?:몸\\s*전체|전신)(?:가|이)?\\s*"
                    + "(?:모두\\s*)?들어가야\\s*(?:만\\s*)?(?:동반|입장|출입|이용)\\s*가능"
    );

    static final Pattern CARRIER_ENTRY_PATTERN = Pattern.compile(
            CARRIER_EXPRESSION + "\\s*(?:사용|이용|동반|지참)\\s*(?:시|하면|해야(?:만)?)\\s*"
                    + "(?:반려동물|반려견)?\\s*(?:동반|입장|출입|이용)\\s*가능"
    );

    static final Pattern STROLLER_NEED_PATTERN = Pattern.compile(
            "유모차\\s*(?:사용|이용|지참)?\\s*(?:필수|필요)?"
    );

    static final Pattern STROLLER_EXPLICIT_PATTERN = Pattern.compile(
            "유모차\\s*(?:사용|이용|지참)?\\s*(?:필수|필요)"
    );

    static final Pattern MANNER_BELT_NEED_PATTERN = Pattern.compile(
            "매너\\s*벨트\\s*(?:착용|사용|지참)?\\s*(?:필수|필요)?"
    );

    static final Pattern MANNER_BELT_EXPLICIT_PATTERN = Pattern.compile(
            "매너\\s*벨트\\s*(?:착용|사용|지참)?\\s*(?:필수|필요)"
    );

    static final Pattern VACCINATION_REQUIRED_PATTERN = Pattern.compile(
            "(?:예방\\s*)?접종\\s*완료\\s*(?:한\\s*반려견)?\\s*만\\s*"
                    + "(?:이용|동반|출입)?\\s*(?:가능|허용)|"
                    + "예방\\s*접종\\s*(?:완료\\s*)?(?:은|는|이|가)?\\s*(?:필수|필요)"
    );

    static final Pattern VACCINATION_CERTIFICATE_NEED_PATTERN = Pattern.compile(
            "(?:예방\\s*)?접종\\s*(?:증명서|증빙)\\s*"
                    + "(?:지참|제시|준비)?\\s*(?:필수|필요)?"
    );

    static final Pattern VACCINATION_CERTIFICATE_EXPLICIT_PATTERN = Pattern.compile(
            "(?:예방\\s*)?접종\\s*(?:증명서|증빙)\\s*"
                    + "(?:(?:지참|제시|준비)(?:\\s*(?:필수|필요))?|필수|필요)"
    );

    static final Pattern REQUIREMENT_TARGET_PATTERN = Pattern.compile(
            "(중소형견|소형견|중형견|대형견|맹견)"
                    + "\\s*(?:의\\s*경우|은|는|이|가)?\\s*[:：]?\\s*$"
    );

    // Sex qualifiers are retained as context even though PetSubject currently
    // has no sex field. This prevents "수컷 매너벨트 필수" from leaving the
    // word "수컷" as an unparsed confirmation-triggering fragment.
    static final Pattern SEX_QUALIFIER_PATTERN = Pattern.compile("수컷|암컷");

    static final Pattern REQUIREMENT_SCOPE_PATTERN = Pattern.compile(
            "(실내|야외|실외|외부|건물\\s*내|건물\\s*외)"
                    + "(?:\\s*에서)?(?:\\s+(?:시설|공간))?"
                    + "(?:\\s+(?:이동|이용))?\\s*(?:시)?"
    );

    static final Pattern GENERIC_ALLOWANCE_PATTERN = Pattern.compile(
            "(?:반려동물|반려견)?\\s*(?:은|는|이|가)?\\s*"
                    + "동반\\s*(?:가능|허용)"
    );

    static final Pattern RESERVATION_REQUIRED_PATTERN = Pattern.compile(
            "(?:사전\\s*)?예약\\s*(?:이|가)?\\s*"
                    + "(?:필수|필요|후\\s*(?:이용|동반|출입)?\\s*(?:가능|허용))"
    );

    static final Pattern INQUIRY_REQUIRED_PATTERN = Pattern.compile(
            "(?:(?:사전|전화|개별|현장)\\s*)?문의\\s*(?:이|가)?\\s*"
                    + "(?:필수|필요|후\\s*(?:이용|동반|출입)?\\s*(?:가능|허용))"
    );

    static final String PET_FEE_SUBJECT = "(?:반려견|반려동물|애견)";

    static final Pattern PET_FEE_AMOUNT_PATTERN = Pattern.compile(
            PET_FEE_SUBJECT + "[^,.;\\n]{0,30}?"
                    + "(?:추가\\s*요금|동반\\s*요금|\\d+\\s*마리당)\\s*"
                    + "(?::|은|는)?\\s*(\\d[\\d,]*)\\s*원"
    );

    static final Pattern PET_FEE_REQUIRED_PATTERN = Pattern.compile(
            PET_FEE_SUBJECT + "[^,.;\\n]{0,30}?"
                    + "(?:추가\\s*요금|동반\\s*요금)[^,.;\\n]{0,10}?"
                    + "(?:발생|부과|필수|필요)"
    );

    static final Pattern UNCERTAIN_FEE_PATTERN = Pattern.compile(
            "(?:발생|부과)\\s*(?:가능|할\\s*수)"
    );

    // Match full requirement clauses before their shorter equipment names.
    static final List<RequirementPattern> REQUIREMENT_PATTERNS = List.of(
            new RequirementPattern(RequirementType.VACCINATION_STATUS, VACCINATION_REQUIRED_PATTERN, VACCINATION_REQUIRED_PATTERN),
            new RequirementPattern(RequirementType.LEASH, LEASH_NEED_PATTERN, LEASH_EXPLICIT_PATTERN),
            new RequirementPattern(RequirementType.MUZZLE, MUZZLE_NEED_PATTERN, MUZZLE_EXPLICIT_PATTERN),
            new RequirementPattern(RequirementType.WASTE_BAG, WASTE_BAG_NEED_PATTERN, WASTE_BAG_EXPLICIT_PATTERN),
            new RequirementPattern(RequirementType.WASTE_DISPOSAL, WASTE_DISPOSAL_NEED_PATTERN, WASTE_DISPOSAL_EXPLICIT_PATTERN),
            new RequirementPattern(RequirementType.CARRIER, CARRIER_ENTRY_PATTERN, CARRIER_ENTRY_PATTERN),
            new RequirementPattern(RequirementType.CARRIER, CARRIER_FULL_BODY_PATTERN, CARRIER_FULL_BODY_PATTERN),
            new RequirementPattern(RequirementType.CARRIER, CARRIER_NEED_PATTERN, CARRIER_EXPLICIT_PATTERN),
            new RequirementPattern(RequirementType.STROLLER, STROLLER_NEED_PATTERN, STROLLER_EXPLICIT_PATTERN),
            new RequirementPattern(RequirementType.MANNER_BELT, MANNER_BELT_NEED_PATTERN, MANNER_BELT_EXPLICIT_PATTERN)
    );

    record RequirementPattern(RequirementType type, Pattern needMatter, Pattern explicit) {
        Pattern forSource(boolean needMatterSource) {
            return needMatterSource ? needMatter : explicit;
        }
    }
}
