/* 병원·약국의 지역·이름·지도 조회 조건을 쿼리로 변환해 검색 API를 호출한다. */
import { apiFetch } from "../../../shared/api/client";
import type { HospitalPageResponse } from "../types/hospital";

import { API_URL } from "../../../shared/api/config";

export async function getHospitals(
    address?: string,
    name?: string,
    facilityType: "HOSPITAL" | "PHARMACY" = "HOSPITAL",
    page = 1,
    size = 20
): Promise<HospitalPageResponse> {
    const query = new URLSearchParams({
        page: String(page),
        size: String(size),
        facilityType,
    });

    if (address) {
        query.set("address", address);
    }
    if (name) {
        query.set("name", name);
    }

    const response = await apiFetch(
        `${API_URL}/api/hospitals?${query.toString()}`,
        {
            method: "GET",
            credentials: "include",
        }
    );

    if (!response.ok) {
        let message = "동물병원 정보를 불러오지 못했습니다.";

        try {
            const data = await response.json();
            message = data.message ?? message;
        } catch {
            // Keep the user-safe default message.
        }

        throw new Error(message);
    }

    return response.json();
}
