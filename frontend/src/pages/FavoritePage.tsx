import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { fetchFavorites, toggleFavorite, type Favorite } from "../shared/api/favoriteApi";
import "./styles/mypage.css";
import ReviewButton from "../shared/components/ReviewButton";
export default function FavoritePage(){
 const [items,setItems]=useState<Favorite[]>([]); const [loading,setLoading]=useState(true);
 useEffect(()=>{fetchFavorites().then(setItems).finally(()=>setLoading(false));},[]);
 const remove=async (f:Favorite)=>{await toggleFavorite({targetType:f.targetType,targetId:f.targetId,title:f.title,address:f.address,thumbnailUrl:f.thumbnailUrl});setItems(v=>v.filter(x=>x.id!==f.id));};
 return <main className="mypage-page"><div className="mypage-container"><div className="mypage-section-heading"><span className="mypage-eyebrow">MY PAGE</span><h1>즐겨찾기</h1><p>장소, 동물병원·약국, 여행 코스를 한 곳에서 확인하세요.</p></div>{loading?<p>불러오는 중...</p>:items.length===0?<p>아직 저장한 즐겨찾기가 없습니다.</p>:<div className="mypage-menu-grid">{items.map(f=><article className="mypage-menu-card favorite-item-card" key={f.id}><div><small>{f.targetType}</small><h3>{f.title}</h3><p>{f.address}</p><ReviewButton targetType={f.targetType} targetId={f.targetId} title={f.title}/></div><button type="button" onClick={()=>remove(f)} aria-label="즐겨찾기 삭제">♥</button></article>)}</div>}<Link to="/mypage">마이페이지로 돌아가기</Link></div></main>;
}
