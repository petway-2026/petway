import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', 'VITE_')
  const apiUrl = env.VITE_API_URL?.trim()
  if (!apiUrl) {
    throw new Error('VITE_API_URL 환경변수에 백엔드 주소를 설정해 주세요.')
  }
  if (!/^https?:\/\/(?:[a-z0-9.-]+|\[[a-f0-9:]+\])(?::\d{1,5})?\/?$/i.test(apiUrl)) {
    throw new Error('VITE_API_URL은 경로가 없는 HTTP(S) origin이어야 합니다.')
  }
  return { plugins: [react()] }
})
