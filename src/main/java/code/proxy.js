const express = require("express");
const {createProxyMiddleware} = require("http-proxy-middleware");
const path = require("path");
const app = express();



// Point the HTML files location
app.use(express.static(path.join(__dirname, "frontend")))


// Log requests
app.use("/api", (req, res, next) => {
    console.log("Incoming request: ", req.method, req.originalUrl);
    next();
})


// Proxy to the backend
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


// Log requests
app.use((req, res) => {
    console.log("Unhandled request: ", req.method, req.url);
    res.status(404).send("Not found in proxy");
})




const PORT = 5500;
app.listen(PORT) 
    .on("listening", () => {
        console.log("Proxy running")
    })
    .on("error", (err) => {
        console.error("Failed to start proxy")
    })
