const express = require("express");
const router = express.Router();
const locationService = require("../services/locationService");
const streetViewService = require("../services/streetViewService");
const cacheService = require("../services/cacheService");

// Get random location(s) - Public endpoint
router.get("/random", async (req, res) => {
  try {
    console.log("🔍 Location Route: /random called with query:", req.query);

    const { count, difficulty, category, country, exclude } = req.query;

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

    res.json({
      success: true,
      data: {
        count: locations.length,
        locations,
        filters: {
          difficulty: options.difficulty,
          category: options.category,
          country: options.country,
          excluded: options.excludeIds.length,
        },
      },
    });
  } catch (error) {
    console.error("❌ Location Route: /random failed:", error.message);
    console.error("❌ Location Route: Full error:", error);
    res.status(500).json({
      error: "Failed to get random locations",
      message: "Internal server error",
      debug: process.env.NODE_ENV === "development" ? error.message : undefined,
    });
  }
});

// Get location statistics - Public endpoint
router.get("/stats/overview", async (req, res) => {
  try {
    console.log("🔍 Location Route: /stats/overview called");

    const stats = await locationService.getLocationStats();
    console.log("✅ Location Route: Stats retrieved successfully");

    res.json({
      success: true,
      data: { stats },
    });
  } catch (error) {
    console.error("❌ Location Route: /stats/overview failed:", error.message);
    res.status(500).json({
      error: "Failed to get location statistics",
      message: "Internal server error",
    });
  }
});

// Get locations by difficulty - Public endpoint
router.get("/difficulty/:level", async (req, res) => {
  try {
    const { level } = req.params;
    const { limit } = req.query;

    console.log(`🔍 Location Route: /difficulty/${level} called`);

    if (isNaN(level) || level < 1 || level > 5) {
      return res.status(400).json({
        error: "Invalid difficulty level",
        message: "Difficulty must be a number between 1 and 5",
      });
    }

    const limitValue = limit ? parseInt(limit) : 10;
    if (isNaN(limitValue) || limitValue < 1 || limitValue > 50) {
      return res.status(400).json({
        error: "Invalid limit",
        message: "Limit must be a number between 1 and 50",
      });
    }

    const result = await locationService.getLocationsByDifficulty(
      parseInt(level),
      limitValue
    );
    console.log("✅ Location Route: Difficulty locations retrieved");

    res.json({
      success: true,
      data: result,
    });
  } catch (error) {
    console.error("❌ Location Route: /difficulty failed:", error.message);
    res.status(500).json({
      error: "Failed to get locations by difficulty",
      message: "Internal server error",
    });
  }
});

// Get locations by category - Public endpoint
router.get("/category/:name", async (req, res) => {
  try {
    const { name } = req.params;
    const { limit } = req.query;

    console.log(`🔍 Location Route: /category/${name} called`);

    const limitValue = limit ? parseInt(limit) : 10;
    if (isNaN(limitValue) || limitValue < 1 || limitValue > 50) {
      return res.status(400).json({
        error: "Invalid limit",
        message: "Limit must be a number between 1 and 50",
      });
    }

    const result = await locationService.getLocationsByCategory(
      name,
      limitValue
    );
    console.log("✅ Location Route: Category locations retrieved");

    res.json({
      success: true,
      data: result,
    });
  } catch (error) {
    console.error("❌ Location Route: /category failed:", error.message);

    if (error.message.includes("Invalid category")) {
      return res.status(400).json({
        error: "Invalid category",
        message: error.message,
      });
    }

    res.status(500).json({
      error: "Failed to get locations by category",
      message: "Internal server error",
    });
  }
});

// Search locations near coordinates - Public endpoint
router.get("/near/:lat/:lng", async (req, res) => {
  try {
    const { lat, lng } = req.params;
    const { radius, limit } = req.query;

    console.log(`🔍 Location Route: /near/${lat}/${lng} called`);

    const latitude = parseFloat(lat);
    const longitude = parseFloat(lng);

    if (isNaN(latitude) || latitude < -90 || latitude > 90) {
      return res.status(400).json({
        error: "Invalid latitude",
        message: "Latitude must be a number between -90 and 90",
      });
    }

    if (isNaN(longitude) || longitude < -180 || longitude > 180) {
      return res.status(400).json({
        error: "Invalid longitude",
        message: "Longitude must be a number between -180 and 180",
      });
    }

    const radiusKm = radius ? parseFloat(radius) : 100;
    const limitValue = limit ? parseInt(limit) : 10;

    if (isNaN(radiusKm) || radiusKm < 1 || radiusKm > 20000) {
      return res.status(400).json({
        error: "Invalid radius",
        message: "Radius must be between 1 and 20000 kilometers",
      });
    }

    const result = await locationService.getLocationsNear(
      latitude,
      longitude,
      radiusKm,
      limitValue
    );
    console.log("✅ Location Route: Nearby locations retrieved");

    res.json({
      success: true,
      data: result,
    });
  } catch (error) {
    console.error("❌ Location Route: /near failed:", error.message);
    res.status(500).json({
      error: "Failed to search locations",
      message: "Internal server error",
    });
  }
});

// Calculate distance between two locations - Public endpoint
router.get("/distance/:id1/:id2", async (req, res) => {
  try {
    const { id1, id2 } = req.params;

    console.log(`🔍 Location Route: /distance/${id1}/${id2} called`);

    if (isNaN(id1) || isNaN(id2)) {
      return res.status(400).json({
        error: "Invalid location IDs",
        message: "Location IDs must be numbers",
      });
    }

    const distance = await locationService.calculateDistance(
      parseInt(id1),
      parseInt(id2)
    );
    console.log("✅ Location Route: Distance calculated");

    res.json({
      success: true,
      data: { distance },
    });
  } catch (error) {
    console.error("❌ Location Route: /distance failed:", error.message);

    if (error.message.includes("not found")) {
      return res.status(404).json({
        error: "Location not found",
        message: error.message,
      });
    }

    res.status(500).json({
      error: "Failed to calculate distance",
      message: "Internal server error",
    });
  }
});

// Health check for locations
router.get("/health", async (req, res) => {
  try {
    await locationService.testConnection();
    res.json({
      success: true,
      message: "Location service is healthy",
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: "Location service is unhealthy",
      message: error.message,
    });
  }
});

// Get Street View image for location
router.get("/:id/streetview", async (req, res) => {
  try {
    const { id } = req.params;
    const {
      heading,
      multiple = false,
      responsive = false,
      preferHighQuality = false,
    } = req.query;

    const location = await locationService.getLocationById(parseInt(id));

    const response = {
      success: true,
      data: {
        location: {
          id: location.id,
          coordinates: location.coordinates,
        },
      },
    };

    if (responsive === "true") {
      // Generate responsive URLs with enhanced fallback logic
      const requestContext = {
        userAgent: req.get("User-Agent"),
        deviceType: req.get("X-Device-Type"), // Custom header if frontend sends it
        preferHighQuality: preferHighQuality === "true",
      };

      response.data.streetViewUrls =
        streetViewService.generateResponsiveUrlsWithContext(
          location.coordinates.latitude,
          location.coordinates.longitude,
          heading ? parseInt(heading) : null,
          requestContext
        );

      console.log(`StreetView responsive URLs for location ${id}:`, {
        mobile: response.data.streetViewUrls.mobile.substring(0, 100) + "...",
        tablet: response.data.streetViewUrls.tablet.substring(0, 100) + "...",
        desktop: response.data.streetViewUrls.desktop.substring(0, 100) + "...",
        fallbackUsed: response.data.streetViewUrls.mobileFallback || false,
      });
    } else if (multiple === "true") {
      // Generate multiple view angles
      response.data.streetViewUrls = streetViewService.generateViewAngles(
        location.coordinates.latitude,
        location.coordinates.longitude
      );
      console.log(
        `StreetView multiple URLs for location ${id}:`,
        response.data.streetViewUrls
      );
    } else {
      // Single Street View URL
      response.data.streetViewUrl = streetViewService.generateUrl(
        location.coordinates.latitude,
        location.coordinates.longitude,
        heading ? parseInt(heading) : null
      );
      console.log(
        `StreetView URL for location ${id}:`,
        response.data.streetViewUrl
      );
    }

    res.json(response);
  } catch (error) {
    console.error("❌ Street View request failed:", error.message);
    res.status(500).json({
      error: "Failed to get Street View image",
      message: "Internal server error",
    });
  }
});

// Get reliable Street View URLs with automatic fallback - Public endpoint
router.get("/:id/streetview/reliable", async (req, res) => {
  try {
    const { id } = req.params;
    const { heading } = req.query;

    console.log(`🔧 Reliable Street View request for location ${id}`);

    const location = await locationService.getLocationById(parseInt(id));

    // Always use enhanced context for reliable URLs
    const requestContext = {
      userAgent: req.get("User-Agent"),
      deviceType: req.get("X-Device-Type"),
      preferHighQuality: true, // Always prefer high quality for reliable endpoint
    };

    const reliableUrls = streetViewService.generateResponsiveUrlsWithContext(
      location.coordinates.latitude,
      location.coordinates.longitude,
      heading ? parseInt(heading) : null,
      requestContext
    );

    // Additional reliability checks
    const isMobile = streetViewService.detectMobileUserAgent(
      requestContext.userAgent
    );
    const shouldFallback = streetViewService.shouldUseMobileFallback(
      location.coordinates.latitude,
      location.coordinates.longitude
    );

    const response = {
      success: true,
      data: {
        location: {
          id: location.id,
          name: location.name,
          coordinates: location.coordinates,
        },
        streetViewUrls: reliableUrls,
        reliability: {
          isMobileDevice: isMobile,
          fallbackApplied: shouldFallback,
          recommendedUrl:
            isMobile && shouldFallback
              ? reliableUrls.tablet
              : reliableUrls.mobile,
          qualityLevel:
            isMobile && shouldFallback
              ? "tablet"
              : isMobile
              ? "mobile"
              : "desktop",
        },
      },
    };

    console.log(`✅ Reliable Street View URLs generated for location ${id}:`, {
      fallbackApplied: shouldFallback,
      isMobile: isMobile,
      recommendedQuality: response.data.reliability.qualityLevel,
    });

    res.json(response);
  } catch (error) {
    console.error("❌ Reliable Street View request failed:", error.message);
    res.status(500).json({
      error: "Failed to get reliable Street View URLs",
      message: "Internal server error",
    });
  }
});

// Get interactive Street View with fallbacks - Public endpoint
router.get("/:id/streetview/interactive", async (req, res) => {
  try {
    const { id } = req.params;
    const { heading } = req.query;

    console.log(`🌍 Interactive Street View request for location ${id}`);

    const location = await locationService.getLocationById(parseInt(id));

    const interactiveData =
      await streetViewService.generateInteractiveStreetView(
        location.coordinates.latitude,
        location.coordinates.longitude,
        heading ? parseInt(heading) : null
      );

    res.json({
      success: true,
      data: {
        location: {
          id: location.id,
          name: location.name,
          coordinates: location.coordinates,
        },
        interactive: interactiveData,
      },
    });
  } catch (error) {
    console.error("❌ Interactive Street View request failed:", error.message);
    res.status(500).json({
      error: "Failed to get interactive Street View",
      message: "Internal server error",
    });
  }
});

// Check Street View availability - Public endpoint
router.get("/:id/streetview/check", async (req, res) => {
  try {
    const { id } = req.params;

    console.log(`🔍 Checking Street View availability for location ${id}`);

    const location = await locationService.getLocationById(parseInt(id));

    const availability = {
      locationId: location.id,
      coordinates: location.coordinates,
      streetViewAvailable: streetViewService.isInteractiveAvailable(),
      googleMapsAvailable: !!(
        streetViewService.apiKey &&
        streetViewService.apiKey !== "your_api_key_here"
      ),
      mapillaryAvailable: !!(
        streetViewService.mapillaryClientId &&
        streetViewService.mapillaryClientId !== "your_mapillary_client_id_here"
      ),
      staticFallbackAvailable: true,
    };

    res.json({
      success: true,
      data: availability,
    });
  } catch (error) {
    console.error("❌ Street View availability check failed:", error.message);
    res.status(500).json({
      error: "Failed to check Street View availability",
      message: "Internal server error",
    });
  }
});

// Get location by ID - Public endpoint (IMPORTANT: This must be LAST)
router.get("/:id", async (req, res) => {
  try {
    const { id } = req.params;

    console.log(`🔍 Location Route: /${id} called`);

    if (isNaN(id)) {
      return res.status(400).json({
        error: "Invalid location ID",
        message: "Location ID must be a number",
      });
    }

    const location = await locationService.getLocationById(parseInt(id));
    console.log("✅ Location Route: Location by ID retrieved");

    res.json({
      success: true,
      data: { location },
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

module.exports = router;
