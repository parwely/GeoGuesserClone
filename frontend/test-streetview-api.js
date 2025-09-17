// Test-Script für Google Street View APIs
// Führen Sie dieses in der Browser-Konsole aus, um die APIs zu testen

const API_KEY = "AIzaSyD4C5oyZ4ya-sYGKIDqoRa1C3Mqjl22eUc";

// Test 1: Street View Static API
console.log("🧪 Teste Street View Static API...");
const staticUrl = `https://maps.googleapis.com/maps/api/streetview?size=640x640&location=37.7459,-119.5936&heading=248&pitch=0&fov=90&key=${API_KEY}`;
console.log("Static URL:", staticUrl);

// Test 2: Maps Embed API
console.log("🧪 Teste Maps Embed API...");
const embedUrl = `https://www.google.com/maps/embed/v1/streetview?key=${API_KEY}&location=37.7459,-119.5936&heading=248&pitch=0&fov=90`;
console.log("Embed URL:", embedUrl);

// Test 3: Prüfe API-Beschränkungen
console.log("🧪 Teste API-Beschränkungen...");
fetch(staticUrl, { method: 'HEAD' })
  .then(response => {
    console.log("✅ Static API Status:", response.status);
    if (response.status === 403) {
      console.log("❌ API-Schlüssel hat keine Berechtigung für diese API");
    } else if (response.status === 400) {
      console.log("❌ Ungültige Parameter oder Location");
    }
  })
  .catch(error => {
    console.log("❌ Netzwerk-Fehler:", error);
  });

// Test 4: Alternative bekannte Location
console.log("🧪 Teste bekannte Location (Times Square)...");
const knownLocationUrl = `https://maps.googleapis.com/maps/api/streetview?size=640x640&location=40.7580,-73.9855&key=${API_KEY}`;
console.log("Times Square URL:", knownLocationUrl);

// Test 5: Prüfe Quotas
console.log("📊 Prüfe Google Cloud Console für:");
console.log("- API-Quotas und Limits");
console.log("- Billing Status");
console.log("- API-Beschränkungen (HTTP Referrer, IP-Adressen)");
