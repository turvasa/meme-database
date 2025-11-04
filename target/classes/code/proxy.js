const express = require("express");
const {createProxyMiddleware} = require("http-proxy-middleware");
const path = require("path");



const app = express();

app.use(express.static(path.join(__dirname, "frontend")))

app.use("/api", (req, res, next) => {
    console.log("API request: ", req.originalUrl);
    next();
})

app.use("/api", createProxyMiddleware({
    target: "https://localhost:8001",
    changeOrigin: true,
    secure: false,
    onProxyReq: (proxyReq, req, res) => {
        // proxyReq.path is the path that will be sent to the backend
        console.log("Forwarding:", req.originalUrl, "→", proxyReq.path);
    },
    onError: (err, req, res) => {
        console.error("Proxy error:", err);
        if (!res.headersSent) res.status(502).send("Bad gateway");
    }
}));



const PORT = 5500;
app.listen(PORT, () => {
    console.log('Frontend with proxy running.')
})
