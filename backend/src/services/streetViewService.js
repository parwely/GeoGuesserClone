class StreetViewService {
  constructor() {
    this.baseUrl = "https://maps.googleapis.com/maps/api/streetview";
    this.apiKey = process.env.GOOGLE_STREETVIEW_API_KEY;
    this.defaultParams = {
      size: "640x640",
      pitch: "0",
      fov: "90",
    };

    console.log("🌍 Street View service initialized");
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
}

module.exports = new StreetViewService();