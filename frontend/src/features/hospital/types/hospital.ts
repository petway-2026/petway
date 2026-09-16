/* 병원·약국 표시 데이터와 검색 페이지 응답의 타입을 정의한다. */
export interface Hospital {
    id: string;
    name: string;
    address: string | null;
    roadAddress: string | null;
    lotNumberAddress: string | null;
    telephone: string | null;
    businessStatus: string | null;
    detailedStatus: string | null;
    licenseDate: string | null;
    dataUpdatedAt: string | null;
    latitude: number | null;
    longitude: number | null;
    operatingHours?: string | null;
    closedDays?: string | null;
    homepage?: string | null;
    parking?: string | null;
    description?: string | null;
    source?: string | null;
}

export interface HospitalPageResponse {
    hospitals: Hospital[];
    page: number;
    size: number;
    totalCount: number;
}
