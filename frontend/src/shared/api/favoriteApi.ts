import { apiFetch } from "./client";
import { API_URL } from "./config";
export type Favorite = { id:number; targetType:string; targetId:string; title:string; address?:string; thumbnailUrl?:string; createdAt:string };
export type FavoriteRequest = { targetType:string; targetId:string; title:string; address?:string|null; thumbnailUrl?:string|null };
export async function fetchFavorites(): Promise<Favorite[]> { const r=await apiFetch(`${API_URL}/api/favorites`,{credentials:"include"}); if(!r.ok) throw new Error("즐겨찾기를 불러오지 못했습니다."); return r.json(); }
export async function toggleFavorite(value: FavoriteRequest): Promise<Favorite|null> { const r=await apiFetch(`${API_URL}/api/favorites`,{method:"POST",headers:{"Content-Type":"application/json"},credentials:"include",body:JSON.stringify(value)}); if(r.status===204)return null; if(!r.ok)throw new Error("즐겨찾기 저장에 실패했습니다."); return r.json(); }
