require("dotenv").config();
const streetViewService = require("./src/services/streetViewService");

async function debugApiKey() {
  console.log("Debug API Key Information:");
  console.log("this.apiKey:", streetViewService.apiKey);
  console.log("typeof this.apiKey:", typeof streetViewService.apiKey);
  console.log(
    "this.apiKey length:",
    streetViewService.apiKey ? streetViewService.apiKey.length : "undefined"
  );
  console.log("!this.apiKey:", !streetViewService.apiKey);
  console.log("Boolean evaluation:", Boolean(streetViewService.apiKey));

  // Try the validation manually
  try {
    const result = await streetViewService.validateStreetViewCoverage(
      40.758896,
      -73.98513
    );
    console.log("Validation result:", result);
  } catch (error) {
    console.error("Validation error:", error);
  }
}

debugApiKey();
