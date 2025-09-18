const request = require("supertest");
const app = require("../src/server");

describe("Street View Diagnostics", () => {
  test("Diagnostic endpoint should provide comprehensive analysis", async () => {
    console.log("🧪 Testing Street View diagnostic endpoint...");

    const response = await request(app)
      .get("/api/locations/1/streetview/diagnose?heading=90&pitch=10&fov=100")
      .expect(200);

    console.log(
      "📊 Diagnostic Response:",
      JSON.stringify(response.body, null, 2)
    );

    // Verify diagnostic structure
    expect(response.body).toHaveProperty("location");
    expect(response.body).toHaveProperty("parameters");
    expect(response.body).toHaveProperty("apiTests");
    expect(response.body).toHaveProperty("recommendations");
    expect(response.body).toHaveProperty("apiKey");
    expect(response.body).toHaveProperty("configurationHelp");

    // Verify location data
    expect(response.body.location).toHaveProperty("id", 1);
    expect(response.body.location).toHaveProperty("name");
    expect(response.body.location).toHaveProperty("coordinates");

    // Verify parameters
    expect(response.body.parameters.heading).toBe(90);
    expect(response.body.parameters.pitch).toBe(10);
    expect(response.body.parameters.fov).toBe(100);

    // Verify API key analysis
    expect(response.body.apiKey).toHaveProperty("present");
    expect(response.body.apiKey).toHaveProperty("length");
    expect(response.body.apiKey).toHaveProperty("startsWithAIza");

    // Verify configuration help
    expect(response.body.configurationHelp).toHaveProperty(
      "googleCloudConsole"
    );
    expect(response.body.configurationHelp).toHaveProperty("commonIssues");
    expect(response.body.configurationHelp.googleCloudConsole).toHaveProperty(
      "steps"
    );
    expect(response.body.configurationHelp.commonIssues).toHaveProperty(
      "http400"
    );

    console.log("✅ Diagnostic endpoint test passed");
  });

  test("Diagnostic should handle invalid location gracefully", async () => {
    console.log("🧪 Testing diagnostic with invalid location...");

    const response = await request(app)
      .get("/api/locations/999/streetview/diagnose")
      .expect(404);

    expect(response.body).toHaveProperty("error", "Location not found");
    expect(response.body).toHaveProperty("diagnostic");

    console.log("✅ Invalid location diagnostic test passed");
  });

  test("Diagnostic should validate parameters correctly", async () => {
    console.log("🧪 Testing parameter validation...");

    const response = await request(app)
      .get("/api/locations/1/streetview/diagnose?heading=400&pitch=100&fov=5")
      .expect(200);

    // Check parameter validation flags invalid values
    expect(response.body.parameterValidation.heading.valid).toBe(false);
    expect(response.body.parameterValidation.pitch.valid).toBe(false);
    expect(response.body.parameterValidation.fov.valid).toBe(false);

    // Should have recommendations for invalid parameters
    const recommendations = response.body.recommendations;
    const hasHeadingRec = recommendations.some((r) =>
      r.issue.includes("heading")
    );
    const hasPitchRec = recommendations.some((r) => r.issue.includes("pitch"));
    const hasFovRec = recommendations.some((r) => r.issue.includes("FOV"));

    expect(hasHeadingRec || hasPitchRec || hasFovRec).toBe(true);

    console.log("✅ Parameter validation test passed");
  });
});
