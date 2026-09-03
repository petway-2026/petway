import { useEffect, useState } from "react";
import { fetchFavorites, toggleFavorite, type FavoriteRequest } from "../api/favoriteApi";
export default function FavoriteButton(props: FavoriteRequest){
 const [saved,setSaved]=useState(false); const [busy,setBusy]=useState(false);
 useEffect(()=>{let alive=true; fetchFavorites().then(xs=>{if(alive)setSaved(xs.some(x=>x.targetType===props.targetType&&x.targetId===props.targetId));}).catch(()=>{}); return()=>{alive=false};},[props.targetId,props.targetType]);
 const click=async(e: React.MouseEvent)=>{e.preventDefault();e.stopPropagation();if(busy)return;setBusy(true);try{await toggleFavorite(props);setSaved(v=>!v);}catch(error){console.error("favorite toggle failed",error);window.alert("로그인이 필요하거나 즐겨찾기 저장에 실패했습니다.");}finally{setBusy(false)}};
 return <button type="button" className={`favorite-button${saved?" saved":""}`} onClick={click} disabled={busy} aria-label={saved?"즐겨찾기 해제":"즐겨찾기 저장"}>{saved?"♥":"♡"}</button>;
}
