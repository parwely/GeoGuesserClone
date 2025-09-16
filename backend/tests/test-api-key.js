require("dotenv").config();

console.log("Environment variable check:");
console.log(
  "GOOGLE_STREETVIEW_API_KEY:",
  process.env.GOOGLE_STREETVIEW_API_KEY
);
console.log("GOOGLE_MAPS_API_KEY:", process.env.GOOGLE_MAPS_API_KEY);

const streetViewService = require("../src/services/streetViewService");

// Check if the service constructor properly loads the API key
console.log("Service API key:", streetViewService.apiKey);
console.log(
  "Key length:",
  streetViewService.apiKey ? streetViewService.apiKey.length : "undefined"
);
