// Einfacher Diagnose-Test für Street View Endpoint
console.log("🔍 Diagnose: Street View Check Endpoint");
console.log("Teste http://localhost:3000/api/game/streetview/check/1");

const http = require("http");

// Einfacher HTTP GET Request
const req = http
  .get("http://localhost:3000/api/game/streetview/check/1", (res) => {
    console.log(`\n✅ Verbindung erfolgreich!`);
    console.log(`Status: ${res.statusCode}`);
    console.log(`Headers:`, res.headers);

    let body = "";
    res.on("data", (chunk) => {
      body += chunk;
    });

    res.on("end", () => {
      console.log(`\nResponse Body:`);
      try {
        const data = JSON.parse(body);
        console.log(JSON.stringify(data, null, 2));
      } catch (e) {
        console.log(body);
      }

      console.log("\n🎉 Test erfolgreich abgeschlossen!");
    });
  })
  .on("error", (err) => {
    console.log(`\n❌ Verbindungsfehler: ${err.message}`);
    console.log("Mögliche Ursachen:");
    console.log("- Server läuft nicht auf Port 3000");
    console.log("- Firewall blockiert die Verbindung");
    console.log("- Endpoint noch nicht geladen");
  });
