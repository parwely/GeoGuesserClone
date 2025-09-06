class StreetViewService {
  constructor() {
    this.baseUrl = "https://maps.googleapis.com/maps/api/streetview";
    this.interactiveUrl = "https://www.google.com/maps/@";
    this.embedUrl = "https://www.google.com/maps/embed/v1/streetview";
    this.mapillaryUrl = "https://www.mapillary.com/embed";
    this.apiKey = process.env.GOOGLE_STREETVIEW_API_KEY;
    this.mapillaryClientId = process.env.MAPILLARY_CLIENT_ID;
    this.defaultParams = {
      size: "640x640",
      pitch: "0",
      fov: "90",
    };

    console.log("🌍 Street View service initialized with interactive support");
  }

  generateUrl(latitude, longitude, heading = null) {
    if (!this.apiKey) {
      throw new Error("Google Street View API key not configured");
    }

    const params = new URLSearchParams({
      ...this.defaultParams,
      location: `${latitude},${longitude}`,
      heading:
        heading !== null
          ? heading.toString()
          : this.getRandomHeading().toString(),
      key: this.apiKey,
    });

    return `${this.baseUrl}?${params.toString()}`;
  }

  getRandomHeading() {
    return Math.floor(Math.random() * 360);
  }

  generateMultipleUrls(latitude, longitude, count = 4) {
    const urls = [];
    const angleStep = 360 / count;

    for (let i = 0; i < count; i++) {
      const heading = Math.floor(i * angleStep);
      urls.push({
        heading,
        url: this.generateUrl(latitude, longitude, heading),
      });
    }

    return urls;
  }

  // Generate URLs for different view angles
  generateViewAngles(latitude, longitude) {
    const angles = [0, 90, 180, 270]; // N, E, S, W

    return angles.map((heading) => ({
      direction: this.getDirectionName(heading),
      heading,
      url: this.generateUrl(latitude, longitude, heading),
    }));
  }

  getDirectionName(heading) {
    const directions = [
      "North",
      "Northeast",
      "East",
      "Southeast",
      "South",
      "Southwest",
      "West",
      "Northwest",
    ];
    const index = Math.round(heading / 45) % 8;
    return directions[index];
  }

  // Validate if Street View is available at location (requires additional API call)
  async checkAvailability(latitude, longitude) {
    // Note: This would require the Street View Static API metadata endpoint
    // For now, we'll assume all locations have Street View
    return true;
  }

  // Generate optimized URLs for different device sizes
  generateResponsiveUrls(latitude, longitude, heading = null) {
    const sizes = [
      { name: "mobile", size: "400x400" },
      { name: "tablet", size: "640x640" },
      { name: "desktop", size: "800x600" },
    ];

    const actualHeading = heading !== null ? heading : this.getRandomHeading();

    return sizes.reduce((urls, sizeConfig) => {
      const params = new URLSearchParams({
        ...this.defaultParams,
        size: sizeConfig.size,
        location: `${latitude},${longitude}`,
        heading: actualHeading.toString(),
        key: this.apiKey,
      });

      urls[sizeConfig.name] = `${this.baseUrl}?${params.toString()}`;
      return urls;
    }, {});
  }

  // ========================
  // INTERACTIVE STREET VIEW
  // ========================

  // Generate interactive Google Maps Street View URL (opens in Maps app)
  generateInteractiveUrl(latitude, longitude, heading = null, zoom = 1) {
    const actualHeading = heading !== null ? heading : this.getRandomHeading();

    // Google Maps Street View URL format
    return `https://www.google.com/maps/@${latitude},${longitude},3a,${
      zoom * 60
    }y,${actualHeading}h,90t/data=!3m1!1e3`;
  }

  // Generate embeddable Street View URL for WebView
  generateEmbedUrl(latitude, longitude, heading = null, fov = 90) {
    if (!this.apiKey) {
      console.warn(
        "Google Maps API key not configured, falling back to Mapillary"
      );
      return this.generateMapillaryEmbedUrl(latitude, longitude, heading);
    }

    const actualHeading = heading !== null ? heading : this.getRandomHeading();

    const params = new URLSearchParams({
      key: this.apiKey,
      location: `${latitude},${longitude}`,
      heading: actualHeading.toString(),
      fov: fov.toString(),
      pitch: "0",
    });

    return `${this.embedUrl}?${params.toString()}`;
  }

  // Mapillary fallback for Street View
  generateMapillaryEmbedUrl(latitude, longitude, heading = null) {
    const actualHeading = heading !== null ? heading : this.getRandomHeading();

    // Mapillary embed URL (approximation - requires actual image key in production)
    const params = new URLSearchParams({
      map_filter: "all",
      map_style: "streets",
      image_key: "", // Would need to be resolved via Mapillary API
      x: longitude.toString(),
      y: latitude.toString(),
      z: "17",
      bearing: actualHeading.toString(),
    });

    return `${this.mapillaryUrl}?${params.toString()}`;
  }

  // Get Mapillary images near coordinates (requires Mapillary API)
  async getMapillaryImages(latitude, longitude, radius = 50) {
    if (
      !this.mapillaryClientId ||
      this.mapillaryClientId === "your_mapillary_client_id_here"
    ) {
      console.warn("Mapillary Client ID not configured");
      return null;
    }

    try {
      // In production, implement Mapillary API call
      // For now, return mock data
      return {
        type: "FeatureCollection",
        features: [
          {
            type: "Feature",
            properties: {
              id: "mock_image_id",
              compass_angle: this.getRandomHeading(),
              captured_at: new Date().toISOString(),
            },
            geometry: {
              type: "Point",
              coordinates: [longitude, latitude],
            },
          },
        ],
      };
    } catch (error) {
      console.error("Mapillary API error:", error);
      return null;
    }
  }

  // Generate comprehensive interactive Street View data with fallbacks
  async generateInteractiveStreetView(latitude, longitude, heading = null) {
    const actualHeading = heading !== null ? heading : this.getRandomHeading();

    const result = {
      // Primary: Google Street View
      google: {
        embedUrl: null,
        mapsUrl: null,
        available: false,
      },

      // Fallback: Mapillary
      mapillary: {
        embedUrl: null,
        available: false,
        images: null,
      },

      // Always available: Static image fallback
      static: {
        url: null,
        available: true,
      },

      // Metadata
      coordinates: { latitude, longitude },
      heading: actualHeading,
      fallbackUsed: false,
    };

    // Try Google Street View first
    if (this.apiKey && this.apiKey !== "your_api_key_here") {
      try {
        result.google.embedUrl = this.generateEmbedUrl(
          latitude,
          longitude,
          actualHeading
        );
        result.google.mapsUrl = this.generateInteractiveUrl(
          latitude,
          longitude,
          actualHeading
        );
        result.google.available = true;

        console.log(
          `✅ Google Street View generated for ${latitude}, ${longitude}`
        );
      } catch (error) {
        console.warn("Google Street View generation failed:", error.message);
      }
    }

    // Try Mapillary fallback
    if (!result.google.available) {
      try {
        result.mapillary.embedUrl = this.generateMapillaryEmbedUrl(
          latitude,
          longitude,
          actualHeading
        );
        result.mapillary.images = await this.getMapillaryImages(
          latitude,
          longitude
        );
        result.mapillary.available = true;
        result.fallbackUsed = true;

        console.log(
          `⚠️ Using Mapillary fallback for ${latitude}, ${longitude}`
        );
      } catch (error) {
        console.warn("Mapillary fallback failed:", error.message);
      }
    }

    // Always provide static fallback
    try {
      result.static.url = this.generateUrl(latitude, longitude, actualHeading);
      console.log(
        `📸 Static Street View fallback ready for ${latitude}, ${longitude}`
      );
    } catch (error) {
      console.error("Static Street View generation failed:", error.message);
      result.static.available = false;
    }

    return result;
  }

  // Quick check if interactive Street View is available
  isInteractiveAvailable() {
    return (
      (this.apiKey && this.apiKey !== "your_api_key_here") ||
      (this.mapillaryClientId &&
        this.mapillaryClientId !== "your_mapillary_client_id_here")
    );
  }
}

module.exports = new StreetViewService();
