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