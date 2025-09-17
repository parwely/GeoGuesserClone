const express = require("express");
const router = express.Router();
const locationService = require("../services/locationService");
const streetViewService = require("../services/streetViewService");
const database = require("../database/connection");

// Test endpoint
router.get("/test", (req, res) => {
  res.json({
    message: "Locations route is working",
    timestamp: new Date().toISOString(),
  });
});

// Get random location(s) - Public endpoint
router.get("/random", async (req, res) => {
  try {
    console.log("🔍 Location Route: /random called with query:", req.query);

    const { count, difficulty, category, country, exclude, includeStreetView } =
      req.query;

    // Validate query parameters
    const errors = locationService.validateLocationQuery(req.query);
    if (errors.length > 0) {
      console.log("❌ Location Route: Validation failed:", errors);
      return res.status(400).json({
        error: "Invalid query parameters",
        messages: errors,
      });
    }

    const options = {
      count: count ? parseInt(count) : 1,
      difficulty: difficulty ? parseInt(difficulty) : null,
      category: category || null,
      country: country || null,
      excludeIds: exclude ? exclude.split(",").map((id) => parseInt(id)) : [],
    };

    console.log("🔍 Location Route: Processed options:", options);

    const locations = await locationService.getRandomLocations(options);
    console.log("✅ Location Route: Retrieved locations:", locations.length);

    // Apply cache headers for public endpoint
    const cacheTTL = 300; // 5 minutes
    res.set({
      "Cache-Control": `public, max-age=${cacheTTL}, s-maxage=${cacheTTL}`,
      ETag: `"random-${Date.now()}"`,
    });

    res.json({
      success: true,
      data: locations,
      count: locations.length,
      cached: false,
    });
  } catch (error) {
    console.error("❌ Location Route: /random failed:", error.message);
    res.status(500).json({
      error: "Failed to get random locations",
      message: "Internal server error",
    });
  }
});

// Get specific location by ID - Public endpoint
router.get("/:id", async (req, res) => {
  try {
    console.log(`🔍 Location Route: /:id called with ID: ${req.params.id}`);

    const locationId = parseInt(req.params.id);
    if (isNaN(locationId)) {
      return res.status(400).json({
        error: "Invalid location ID",
        message: "Location ID must be a number",
      });
    }

    const location = await locationService.getLocationById(locationId);
    console.log("✅ Location Route: Retrieved location:", location?.name);

    // Apply cache headers for public endpoint
    const cacheTTL = 3600; // 1 hour
    res.set({
      "Cache-Control": `public, max-age=${cacheTTL}, s-maxage=${cacheTTL}`,
      ETag: `"location-${locationId}-${Date.now()}"`,
    });

    res.json({
      success: true,
      data: location,
    });
  } catch (error) {
    console.error("❌ Location Route: /:id failed:", error.message);

    if (error.message === "Location not found") {
      return res.status(404).json({
        error: "Location not found",
        message: `No location found with ID ${req.params.id}`,
      });
    }

    res.status(500).json({
      error: "Failed to get location",
      message: "Internal server error",
    });
  }
});

// Get interactive Street View URL for location - Primary endpoint
router.get("/:id/streetview", async (req, res) => {
  try {
    console.log(
      `🌍 Street View Route: /:id/streetview called with ID: ${req.params.id}`
    );

    const locationId = parseInt(req.params.id);
    const heading = req.query.heading ? parseInt(req.query.heading) : 0;
    const pitch = req.query.pitch ? parseInt(req.query.pitch) : 0;
    const fov = req.query.fov ? parseInt(req.query.fov) : 90;

    console.log("🎯 Street View Route: Parameters:", {
      locationId,
      heading,
      pitch,
      fov,
    });

    if (isNaN(locationId)) {
      return res.status(400).json({
        error: "Invalid location ID",
        message: "Location ID must be a number",
      });
    }

    // Get location data
    const location = await locationService.getLocationById(locationId);

    if (!location) {
      return res.status(404).json({
        error: "Location not found",
        message: `No location found with ID ${locationId}`,
      });
    }

    // Generate interactive Street View URL
    const interactiveUrl = streetViewService.generateInteractiveStreetViewUrl(
      location.coordinates.latitude,
      location.coordinates.longitude,
      heading,
      pitch,
      fov
    );

    // Also generate static fallback URL using generateUrl method
    const staticUrl = streetViewService.generateUrl(
      location.coordinates.latitude,
      location.coordinates.longitude,
      heading
    );

    console.log("✅ Street View Route: URLs generated successfully");

    res.json({
      success: true,
      data: {
        location: {
          id: location.id,
          name: location.name,
          coordinates: location.coordinates,
        },
        streetView: {
          interactive: interactiveUrl,
          static: staticUrl,
        },
        parameters: {
          heading: heading,
          pitch: pitch,
          fov: fov,
        },
      },
    });
  } catch (error) {
    console.error(
      "❌ Street View Route: /:id/streetview failed:",
      error.message
    );

    if (error.message === "Location not found") {
      return res.status(404).json({
        error: "Location not found",
        message: `No location found with ID ${req.params.id}`,
      });
    }

    res.status(500).json({
      error: "Failed to get Street View URL",
      message: "Internal server error",
    });
  }
});

// Diagnostic endpoint for Street View API troubleshooting
router.get("/:id/streetview/diagnose", async (req, res) => {
  try {
    console.log("📊 Starting Street View API diagnostic...");

    const locationId = parseInt(req.params.id);
    const heading = req.query.heading ? parseInt(req.query.heading) : 0;
    const pitch = req.query.pitch ? parseInt(req.query.pitch) : 0;
    const fov = req.query.fov ? parseInt(req.query.fov) : 90;

    // Get location data
    const result = await database.query(
      "SELECT name, ST_X(coordinates) as longitude, ST_Y(coordinates) as latitude FROM locations WHERE id = $1",
      [locationId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        error: "Location not found",
        diagnostic: "Cannot run diagnostics without valid location",
      });
    }

    const loc = result.rows[0];
    const diagnostics = {
      location: {
        id: locationId,
        name: loc.name,
        coordinates: `${loc.latitude}, ${loc.longitude}`,
      },
      parameters: {
        heading: heading,
        pitch: pitch,
        fov: fov,
      },
      apiTests: {},
      recommendations: [],
    };

    // Check API Key Configuration
    const apiKey = process.env.GOOGLE_MAPS_API_KEY;
    diagnostics.apiKey = {
      present: !!apiKey,
      length: apiKey ? apiKey.length : 0,
      startsWithAIza: apiKey ? apiKey.startsWith("AIza") : false,
    };

    if (!apiKey) {
      diagnostics.recommendations.push({
        priority: "CRITICAL",
        issue: "Missing API Key",
        solution: "Set GOOGLE_MAPS_API_KEY environment variable",
      });
    }

    // Generate URLs using the service
    try {
      const interactiveUrl = streetViewService.generateInteractiveStreetViewUrl(
        loc.latitude,
        loc.longitude,
        heading,
        pitch,
        fov
      );

      const staticUrl = streetViewService.generateUrl(
        loc.latitude,
        loc.longitude,
        heading
      );

      diagnostics.generatedUrls = {
        interactive: interactiveUrl,
        static: staticUrl,
      };

      diagnostics.apiTests.urlGeneration = {
        status: "SUCCESS",
        message: "URLs generated successfully",
      };
    } catch (error) {
      diagnostics.apiTests.urlGeneration = {
        status: "ERROR",
        error: error.message,
      };
    }

    // Parameter Validation
    const paramValidation = {
      heading: {
        value: heading,
        valid: heading >= 0 && heading <= 360,
        type: typeof heading,
      },
      pitch: {
        value: pitch,
        valid: pitch >= -90 && pitch <= 90,
        type: typeof pitch,
      },
      fov: {
        value: fov,
        valid: fov >= 10 && fov <= 120,
        type: typeof fov,
      },
    };

    diagnostics.parameterValidation = paramValidation;

    // HTTP Test for Static API (if API key is present)
    if (apiKey) {
      const testUrl = `https://maps.googleapis.com/maps/api/streetview?size=640x640&location=${loc.latitude},${loc.longitude}&heading=${heading}&pitch=${pitch}&fov=${fov}&key=${apiKey}`;

      diagnostics.apiTests.staticApiTest = {
        status: "URL_READY",
        testUrl: testUrl,
        instructions:
          "Test this URL in browser - should return image or error message",
      };
    }

    // Generate recommendations based on findings
    if (diagnostics.apiKey.present && !diagnostics.apiKey.startsWithAIza) {
      diagnostics.recommendations.push({
        priority: "HIGH",
        issue: "Invalid API Key Format",
        solution:
          "Google Maps API keys should start with AIza. Verify your API key.",
      });
    }

    if (!paramValidation.heading.valid) {
      diagnostics.recommendations.push({
        priority: "MEDIUM",
        issue: "Invalid heading parameter",
        solution: `Heading should be 0-360, got: ${heading}`,
      });
    }

    if (!paramValidation.pitch.valid) {
      diagnostics.recommendations.push({
        priority: "MEDIUM",
        issue: "Invalid pitch parameter",
        solution: `Pitch should be -90 to 90, got: ${pitch}`,
      });
    }

    if (!paramValidation.fov.valid) {
      diagnostics.recommendations.push({
        priority: "MEDIUM",
        issue: "Invalid FOV parameter",
        solution: `FOV should be 10-120, got: ${fov}`,
      });
    }

    // Configuration help
    diagnostics.configurationHelp = {
      googleCloudConsole: {
        steps: [
          "1. Go to https://console.cloud.google.com/",
          "2. Select or create a project",
          "3. Enable Street View Static API and Maps Embed API",
          "4. Go to Credentials → Create Credentials → API Key",
          "5. Restrict the API key to your domain/app",
          "6. Enable billing (required for Google Maps APIs)",
        ],
        apiRestrictions: {
          required: ["Street View Static API", "Maps Embed API"],
          optional: ["Maps JavaScript API", "Places API"],
        },
      },
      commonIssues: {
        http400:
          "Usually caused by: Invalid API key, missing billing, or disabled APIs",
        http403: "API key restrictions or quota exceeded",
        http404: "Invalid coordinates or no Street View data available",
      },
    };

    console.log("✅ Street View diagnostic completed");
    res.json(diagnostics);
  } catch (error) {
    console.error("❌ Diagnostic failed:", error);
    res.status(500).json({
      error: "Diagnostic failed",
      message: error.message,
      help: "Check server logs for detailed error information",
    });
  }
});

module.exports = router;
