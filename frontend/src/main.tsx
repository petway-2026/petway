/* React 앱을 마운트하고 라우터·인증 상태 공급자를 최상위에 연결한다. */
import {
    StrictMode,
} from "react";

import {
    createRoot,
} from "react-dom/client";

import "./index.css";

import App from "./app/App";
import "./index.css";
import "./shared/styles/variables.css";
import "./shared/styles/common.css";
createRoot(
    document.getElementById(
        "root"
    )!
).render(
    <StrictMode>
        <App />
    </StrictMode>
);