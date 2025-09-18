// Simple connectivity test
const http = require("http");

const req = http.get("http://localhost:3000/", (res) => {
  console.log(`✅ Server responding - Status: ${res.statusCode}`);
  let data = "";
  res.on("data", (chunk) => (data += chunk));
  res.on("end", () => {
    console.log("Response:", data.substring(0, 100) + "...");
  });
});

req.on("error", (error) => {
  console.error("❌ Connection failed:", error.message);
});

req.setTimeout(5000, () => {
  console.error("❌ Request timeout");
  req.destroy();
});
