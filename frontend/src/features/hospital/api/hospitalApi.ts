import type { HospitalPageResponse } from "../types/hospital";

const API_URL =
    import.meta.env.VITE_API_URL ??
    "http://localhost:8080";

export async function getHospitals(
    address?: string,
    name?: string
): Promise<HospitalPageResponse> {
    const query = new URLSearchParams({
        page: "1",
        size: "100",
    });

    if (address) {
        query.set("address", address);
    }
    if (name) {
        query.set("name", name);
    }

    const response = await fetch(
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
