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

  /**
   * NEW: Generiert eine interaktive Street View URL für WebView/Iframe
   * @param {number} lat - Latitude
   * @param {number} lng - Longitude
   * @param {number} heading - Heading in Grad (0-359)
   * @param {number} pitch - Pitch in Grad (-90 bis 90)
   * @param {number} fov - Field of View (10-100)
   * @returns {string} Interaktive Street View URL
   */
  generateInteractiveStreetViewUrl(lat, lng, heading = 0, pitch = 0, fov = 90) {
    if (!this.apiKey) {
      throw new Error("Google Street View API key not configured");
    }

    const params = new URLSearchParams({
      key: this.apiKey,
      location: `${lat},${lng}`,
      heading: heading.toString(),
      pitch: pitch.toString(),
      fov: fov.toString(),
      // Wichtig: Diese Parameter ermöglichen Navigation
      navigation: "1", // Ermöglicht Bewegung zwischen Street View-Punkten
      controls: "1", // Zeigt Navigationskontrollen
      zoom: "1", // Ermöglicht Zoom
      fullscreen: "1", // Vollbild-Option
    });

    return `${this.embedUrl}?${params.toString()}`;
  }

  /**
   * NEW: Generiere Street View Konfiguration für native Integration
   */
  generateStreetViewConfig(lat, lng, heading = 0, pitch = 0, fov = 90) {
    return {
      type: "interactive_streetview",
      config: {
        position: { lat, lng },
        pov: {
          heading: heading,
          pitch: pitch,
        },
        zoom: fov <= 30 ? 3 : fov <= 60 ? 2 : 1,
        enableCloseButton: false,
        addressControl: false,
        linksControl: true, // Ermöglicht Navigation zu verbundenen Punkten
        panControl: true, // Pan-Kontrollen
        zoomControl: true, // Zoom-Kontrollen
        fullscreenControl: true,
        motionTracking: false,
        motionTrackingControl: false,
      },
      apiKey: this.apiKey,
    };
  }

  /**
   * NEW: Hybrid-Ansatz: Sowohl Embed-URL als auch Config bereitstellen
   */
  generateStreetViewResponse(
    lat,
    lng,
    heading = 0,
    pitch = 0,
    fov = 90,
    responsive = true
  ) {
    const embedUrl = this.generateInteractiveStreetViewUrl(
      lat,
      lng,
      heading,
      pitch,
      fov
    );
    const config = this.generateStreetViewConfig(lat, lng, heading, pitch, fov);

    if (responsive) {
      return {
        // Für WebView/Iframe (einfachste Lösung)
        embedUrl: embedUrl,

        // Für native Google Maps Integration (erweiterte Lösung)
        nativeConfig: config,

        // Responsive URLs für verschiedene Geräte
        responsive: {
          mobile: this.generateInteractiveStreetViewUrl(
            lat,
            lng,
            heading,
            pitch,
            fov
          ),
          tablet: this.generateInteractiveStreetViewUrl(
            lat,
            lng,
            heading,
            pitch,
            fov
          ),
          desktop: this.generateInteractiveStreetViewUrl(
            lat,
            lng,
            heading,
            pitch,
            fov
          ),
        },

        // Fallback für statische Bilder (falls Interactive fehlschlägt)
        fallback: `${this.baseUrl}?size=640x640&location=${lat},${lng}&heading=${heading}&pitch=${pitch}&fov=${fov}&key=${this.apiKey}`,
      };
    }

    return {
      embedUrl: embedUrl,
      nativeConfig: config,
    };
  }

  /**
   * NEW: Prüft ob Street View an einem Ort verfügbar ist
   */
  async checkStreetViewAvailability(lat, lng) {
    try {
      if (!this.apiKey) {
        throw new Error("Google Street View API key not configured");
      }

      // Verwende Street View Service API für Verfügbarkeitsprüfung
      const url = `https://maps.googleapis.com/maps/api/streetview/metadata?location=${lat},${lng}&key=${this.apiKey}`;
      const response = await fetch(url);
      const data = await response.json();

      return {
        available: data.status === "OK",
        location: data.location,
        panoId: data.pano_id,
        date: data.date,
      };
    } catch (error) {
      console.error("Street View availability check failed:", error);
      return { available: false };
    }
  }

  /**
   * NEW: Navigation helper: Calculate new position based on direction
   */
  calculateNavigationPosition(
    currentLat,
    currentLng,
    direction,
    heading,
    stepSize = 25
  ) {
    let newHeading = heading;
    let newLat = currentLat;
    let newLng = currentLng;

    switch (direction) {
      case "forward":
        // Bewege in aktuelle Blickrichtung
        const forwardRad = (heading * Math.PI) / 180;
        newLat += (Math.cos(forwardRad) * stepSize) / 111000;
        newLng +=
          (Math.sin(forwardRad) * stepSize) /
          (111000 * Math.cos((currentLat * Math.PI) / 180));
        break;

      case "backward":
        // Bewege rückwärts
        const backwardRad = ((heading + 180) * Math.PI) / 180;
        newLat += (Math.cos(backwardRad) * stepSize) / 111000;
        newLng +=
          (Math.sin(backwardRad) * stepSize) /
          (111000 * Math.cos((currentLat * Math.PI) / 180));
        break;

      case "left":
        newHeading = (heading - 90 + 360) % 360;
        break;

      case "right":
        newHeading = (heading + 90) % 360;
        break;
    }

    return {
      latitude: newLat,
      longitude: newLng,
      heading: newHeading,
    };
  }

  // ========================
  // LEGACY METHODS - für Rückwärtskompatibilität
  // ========================

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

  // Validate if Street View is available at location (legacy method)
  async checkAvailability(latitude, longitude) {
    const availability = await this.checkStreetViewAvailability(
      latitude,
      longitude
    );
    return availability.available;
  }

  // Generate clean responsive URLs for Kotlin/Android compatibility (Map<String, String>)
  generateCleanResponsiveUrls(
    latitude,
    longitude,
    heading = null,
    requestContext = {}
  ) {
    const { userAgent, deviceType, preferHighQuality = false } = requestContext;

    const urls = this.generateResponsiveUrls(
      latitude,
      longitude,
      heading,
      userAgent
    );

    // Additional fallback strategies
    if (preferHighQuality || deviceType === "tablet") {
      console.log(`🔧 High quality requested for ${latitude},${longitude}`);
      // For tablets or when high quality is preferred, always use tablet+ resolution
      urls.mobile = urls.tablet;
    }

    // Return clean URLs without metadata for Kotlin compatibility
    const cleanUrls = {};
    for (const [key, value] of Object.entries(urls)) {
      if (typeof value === "string" || value === null) {
        cleanUrls[key] = value;
      }
      // Skip any non-string, non-null values to prevent serialization issues
    }

    return cleanUrls;
  }

  // Generate optimized URLs for different device sizes with mobile fallback
  generateResponsiveUrls(latitude, longitude, heading = null, userAgent = "") {
    const sizes = [
      { name: "mobile", size: "400x400" },
      { name: "tablet", size: "640x640" },
      { name: "desktop", size: "800x800" },
    ];

    const actualHeading = heading !== null ? heading : this.getRandomHeading();

    // Detect if we should use higher quality for mobile due to reliability issues
    const isMobileRequest = this.detectMobileUserAgent(userAgent);
    const shouldUseFallback = this.shouldUseMobileFallback(latitude, longitude);

    const urls = sizes.reduce((urlMap, sizeConfig) => {
      const params = new URLSearchParams({
        ...this.defaultParams,
        size: sizeConfig.size,
        location: `${latitude},${longitude}`,
        heading: actualHeading.toString(),
        key: this.apiKey,
      });

      urlMap[sizeConfig.name] = `${this.baseUrl}?${params.toString()}`;
      return urlMap;
    }, {});

    // If mobile URLs are unreliable, return tablet/desktop URLs for mobile requests
    if (isMobileRequest && shouldUseFallback) {
      console.log(
        `📱 Mobile URL fallback activated for ${latitude},${longitude} - using tablet quality`
      );
      urls.mobile = urls.tablet; // Use tablet quality for mobile
      urls.mobileFallback = true;
    }

    return urls;
  }

  // Detect mobile user agent
  detectMobileUserAgent(userAgent) {
    if (!userAgent) return false;

    const mobilePatterns = [
      /Mobile/i,
      /Android/i,
      /iPhone/i,
      /iPad/i,
      /iPod/i,
      /BlackBerry/i,
      /Windows Phone/i,
    ];

    return mobilePatterns.some((pattern) => pattern.test(userAgent));
  }

  // Check if mobile URLs should use fallback for this location
  shouldUseMobileFallback(latitude, longitude) {
    // Define problematic coordinate ranges where mobile URLs are unreliable
    const problematicRanges = [
      // European regions with frequent mobile URL issues
      { latMin: 50.0, latMax: 60.0, lngMin: 5.0, lngMax: 15.0 },
      // Additional ranges can be added based on testing
    ];

    return problematicRanges.some(
      (range) =>
        latitude >= range.latMin &&
        latitude <= range.latMax &&
        longitude >= range.lngMin &&
        longitude <= range.lngMax
    );
  }

  /**
   * NEW: Generate both interactive and static URLs for hybrid approach
   */
  generateHybridUrls(latitude, longitude, heading = null, requestContext = {}) {
    const actualHeading = heading !== null ? heading : this.getRandomHeading();

    // Generate interactive URLs (primary)
    const interactiveResponse = this.generateStreetViewResponse(
      latitude,
      longitude,
      actualHeading,
      0,
      90,
      true
    );

    // Generate static URLs (fallback)
    const staticUrls = this.generateCleanResponsiveUrls(
      latitude,
      longitude,
      actualHeading,
      requestContext
    );

    return {
      // Primary: Interactive embed URLs
      interactive: interactiveResponse,

      // Fallback: Static image URLs
      static: staticUrls,

      // Metadata
      heading: actualHeading,
      location: { latitude, longitude },
      hasInteractive: true,
      hasStatic: true,
    };
  }

  /**
   * Validate Street View coverage at coordinates
   */
  async validateStreetViewCoverage(lat, lng) {
    const availability = await this.checkStreetViewAvailability(lat, lng);

    return {
      hasCoverage: availability.available,
      location: availability.location,
      panoId: availability.panoId,
      date: availability.date,
      source: "google_streetview",
    };
  }
}

module.exports = new StreetViewService();
