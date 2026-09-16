/* 브라우저 전역 카카오 SDK 객체를 TypeScript에서 참조하기 위한 타입 선언이다. */
export {};

declare global {
    interface Window {
        kakao: any;
        daum: any;
    }
}
