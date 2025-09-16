// KORRIGIERTER Backend-Code für Street View URL-Generierung
// Problem: [object Object] in URLs statt echte Heading-Werte

// Vorher (DEFEKT):
const streetViewResponse = {
  embedUrl: `https://www.google.com/maps/embed/v1/streetview?key=${API_KEY}&location=${lat}%2C${lng}&heading=${heading}&pitch=0&fov=90&navigation=1&controls=1&zoom=1&fullscreen=1`,
  nativeConfig: {
    type: "interactive_streetview",
    config: {
      position: { lat, lng },
      pov: {
        heading: { heading, zoom: null, fallbackType: "static" }, // ❌ FEHLER: Objekt statt Nummer
        pitch: 0
      },
      // ...
    }
  }
};

// KORRIGIERT (FUNKTIONAL):
app.get('/api/locations/:id/streetview', async (req, res) => {
  try {
    const { id } = req.params;
    const { heading = 0, multiple = false, responsive = false } = req.query;

    // KORREKTUR 1: Heading als Nummer extrahieren
    const numericHeading = parseInt(heading, 10) || 0;

    // KORREKTUR 2: Sichere URL-Generierung
    const generateStreetViewUrl = (lat, lng, heading, params = {}) => {
      const baseUrl = 'https://www.google.com/maps/embed/v1/streetview';
      const urlParams = new URLSearchParams({
        key: process.env.GOOGLE_MAPS_API_KEY || 'AIzaSyD4C5oyZ4ya-sYGKIDqoRa1C3Mqjl22eUc',
        location: `${lat},${lng}`,
        heading: heading.toString(), // ✅ KORRIGIERT: Explizite String-Konvertierung
        pitch: '0',
        fov: '90',
        navigation: '1',
        controls: '1',
        zoom: '1',
        fullscreen: '1',
        ...params
      });
      return `${baseUrl}?${urlParams.toString()}`;
    };

    const generateStaticUrl = (lat, lng, heading, size = '640x640') => {
      const baseUrl = 'https://maps.googleapis.com/maps/api/streetview';
      const urlParams = new URLSearchParams({
        size,
        location: `${lat},${lng}`,
        heading: heading.toString(), // ✅ KORRIGIERT
        pitch: '0',
        fov: '90',
        key: process.env.GOOGLE_MAPS_API_KEY || 'AIzaSyD4C5oyZ4ya-sYGKIDqoRa1C3Mqjl22eUc'
      });
      return `${baseUrl}?${urlParams.toString()}`;
    };

    // Location aus Database laden
    const location = await Location.findById(id);
    if (!location) {
      return res.status(404).json({
        success: false,
        error: 'Location not found'
      });
    }

    const { latitude, longitude } = location.coordinates;

    // KORREKTUR 3: Saubere Response-Struktur
    const streetViewResponse = {
      success: true,
      data: {
        location: {
          id: location.id,
          name: location.name,
          coordinates: {
            latitude,
            longitude
          }
        },
        streetView: {
          embedUrl: generateStreetViewUrl(latitude, longitude, numericHeading),

          // KORREKTUR 4: nativeConfig als String serialisieren (nicht Objekt)
          nativeConfig: JSON.stringify({
            type: "interactive_streetview",
            config: {
              position: { lat: latitude, lng: longitude },
              pov: {
                heading: numericHeading, // ✅ KORRIGIERT: Direkt die Nummer, nicht Objekt
                pitch: 0
              },
              zoom: 1,
              enableCloseButton: false,
              addressControl: false,
              linksControl: true,
              panControl: true,
              zoomControl: true,
              fullscreenControl: true,
              motionTracking: false,
              motionTrackingControl: false
            },
            apiKey: process.env.GOOGLE_MAPS_API_KEY || 'AIzaSyD4C5oyZ4ya-sYGKIDqoRa1C3Mqjl22eUc'
          }),

          // KORREKTUR 5: Responsive URLs mit korrekten Headings
          responsive: responsive ? {
            mobile: generateStreetViewUrl(latitude, longitude, numericHeading, { fov: '110' }),
            tablet: generateStreetViewUrl(latitude, longitude, numericHeading, { fov: '100' }),
            desktop: generateStreetViewUrl(latitude, longitude, numericHeading, { fov: '90' })
          } : undefined,

          // KORREKTUR 6: Fallback Static URL
          fallback: generateStaticUrl(latitude, longitude, numericHeading)
        }
      }
    };

    res.json(streetViewResponse);

  } catch (error) {
    console.error('Street View API Error:', error);
    res.status(500).json({
      success: false,
      error: 'Internal server error',
      details: error.message
    });
  }
});

// ZUSÄTZLICHE KORREKTUR: Fehlerbehandlung für korrupte Heading-Werte
const sanitizeHeading = (heading) => {
  if (typeof heading === 'object') {
    // Falls ein Objekt übergeben wird, versuche heading-Property zu extrahieren
    return parseInt(heading.heading, 10) || 0;
  }
  if (typeof heading === 'string') {
    // Parse String zu Number
    const parsed = parseInt(heading, 10);
    return isNaN(parsed) ? 0 : parsed;
  }
  if (typeof heading === 'number') {
    return Math.round(heading) % 360;
  }
  return 0; // Default fallback
};

// VALIDATE & TEST URLs:
const testUrl = generateStreetViewUrl(-33.890542, 151.274856, 233);
console.log('Test URL:', testUrl);
// Sollte sein: https://www.google.com/maps/embed/v1/streetview?key=...&location=-33.890542%2C151.274856&heading=233&...
// NICHT: ...&heading=%5Bobject+Object%5D&...
