import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './styles.css'

function App() {
  return (
    <main className="app-shell">
      <section className="status-card" aria-labelledby="page-title">
        <p className="eyebrow">PETWAY</p>
        <h1 id="page-title">프로젝트 초기화 완료</h1>
        <p>반려동물 동반 여행 서비스를 위한 기본 실행 환경입니다.</p>
      </section>
    </main>
  )
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
