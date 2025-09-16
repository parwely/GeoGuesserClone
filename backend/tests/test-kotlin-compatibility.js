const http = require("http");

function makeRequest(path) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: "localhost",
      port: 3000,
      path: path,
      method: "GET",
    };

    const req = http.request(options, (res) => {
      let data = "";
      res.on("data", (chunk) => (data += chunk));
      res.on("end", () => {
        try {
          resolve(JSON.parse(data));
        } catch (e) {
          resolve(data);
        }
      });
    });

    req.on("error", reject);
    req.end();
  });
}

async function testKotlinCompatibility() {
  try {
    console.log("🔍 Testing Kotlin Map<String, String> compatibility...\n");

    // Test responsive Street View endpoint
    console.log("1. Testing responsive Street View URLs...");
    const response = await makeRequest(
      "/api/locations/1/streetview?responsive=true"
    );

    if (response.data && response.data.streetViewUrls) {
      const streetViewUrls = response.data.streetViewUrls;
      console.log("✅ Response received");
      console.log("streetViewUrls structure:");

      for (const [key, value] of Object.entries(streetViewUrls)) {
        const valueType = typeof value;
        const isString = valueType === "string";
        const isNull = value === null;
        const status = isString || isNull ? "✅" : "❌";

        console.log(
          `  ${status} ${key}: ${valueType} ${
            isString
              ? "(Kotlin compatible)"
              : isNull
              ? "(null - acceptable)"
              : "(INCOMPATIBLE!)"
          }`
        );

        if (!isString && !isNull) {
          console.log(`      Value: ${JSON.stringify(value)}`);
        }
      }

      // Check for _metadata (should be removed)
      if ("_metadata" in streetViewUrls) {
        console.log(
          "❌ Found _metadata - this will break Kotlin serialization!"
        );
      } else {
        console.log("✅ No _metadata found - Kotlin serialization will work");
      }

      // Verify all URLs are valid
      const validUrls = Object.entries(streetViewUrls)
        .filter(([key, value]) => value && typeof value === "string")
        .every(([key, url]) => url.startsWith("http"));

      console.log(`✅ All URLs are valid HTTP URLs: ${validUrls}`);
    } else {
      console.log("❌ No streetViewUrls in response");
    }

    console.log("\n🎉 Kotlin compatibility test completed!");
  } catch (error) {
    console.error("❌ Test failed:", error.message);
  }
}

// Wait a moment for server to be ready, then test
setTimeout(testKotlinCompatibility, 2000);
