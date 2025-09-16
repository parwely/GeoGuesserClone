// Backend Street View Service - Interaktive URLs statt statische Bilder
// Ersetze die bestehende streetViewService.js Implementierung

class InteractiveStreetViewService {
    constructor(apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Generiert eine interaktive Street View URL für WebView/Iframe
     * @param {number} lat - Latitude
     * @param {number} lng - Longitude
     * @param {number} heading - Heading in Grad (0-359)
     * @param {number} pitch - Pitch in Grad (-90 bis 90)
     * @param {number} fov - Field of View (10-100)
     * @returns {string} Interaktive Street View URL
     */
    generateInteractiveStreetViewUrl(lat, lng, heading = 0, pitch = 0, fov = 90) {
        // LÖSUNG: Verwende Google Street View Embed API statt Static API
        const baseUrl = 'https://www.google.com/maps/embed/v1/streetview';

        const params = new URLSearchParams({
            key: this.apiKey,
            location: `${lat},${lng}`,
            heading: heading.toString(),
            pitch: pitch.toString(),
            fov: fov.toString(),
            // Wichtig: Diese Parameter ermöglichen Navigation
            navigation: '1',  // Ermöglicht Bewegung zwischen Street View-Punkten
            controls: '1',    // Zeigt Navigationskontrollen
            zoom: '1',        // Ermöglicht Zoom
            fullscreen: '1'   // Vollbild-Option
        });

        return `${baseUrl}?${params.toString()}`;
    }

    /**
     * Alternative: Generiere JavaScript-Code für native Google Maps Street View
     * Für erweiterte Integration
     */
    generateStreetViewConfig(lat, lng, heading = 0, pitch = 0, fov = 90) {
        return {
            type: 'interactive_streetview',
            config: {
                position: { lat, lng },
                pov: {
                    heading: heading,
                    pitch: pitch
                },
                zoom: fov <= 30 ? 3 : (fov <= 60 ? 2 : 1),
                enableCloseButton: false,
                addressControl: false,
                linksControl: true,     // Ermöglicht Navigation zu verbundenen Punkten
                panControl: true,       // Pan-Kontrollen
                zoomControl: true,      // Zoom-Kontrollen
                fullscreenControl: true,
                motionTracking: false,
                motionTrackingControl: false
            },
            apiKey: this.apiKey
        };
    }

    /**
     * Hybrid-Ansatz: Sowohl Embed-URL als auch Config bereitstellen
     */
    generateStreetViewResponse(lat, lng, heading = 0, pitch = 0, fov = 90, responsive = true) {
        const embedUrl = this.generateInteractiveStreetViewUrl(lat, lng, heading, pitch, fov);
        const config = this.generateStreetViewConfig(lat, lng, heading, pitch, fov);

        if (responsive) {
            return {
                // Für WebView/Iframe (einfachste Lösung)
                embedUrl: embedUrl,

                // Für native Google Maps Integration (erweiterte Lösung)
                nativeConfig: config,

                // Responsive URLs für verschiedene Geräte
                responsive: {
                    mobile: this.generateInteractiveStreetViewUrl(lat, lng, heading, pitch, fov),
                    tablet: this.generateInteractiveStreetViewUrl(lat, lng, heading, pitch, fov),
                    desktop: this.generateInteractiveStreetViewUrl(lat, lng, heading, pitch, fov)
                },

                // Fallback für statische Bilder (falls Interactive fehlschlägt)
                fallback: `https://maps.googleapis.com/maps/api/streetview?size=640x640&location=${lat},${lng}&heading=${heading}&pitch=${pitch}&fov=${fov}&key=${this.apiKey}`
            };
        }

        return {
            embedUrl: embedUrl,
            nativeConfig: config
        };
    }

    /**
     * Prüft ob Street View an einem Ort verfügbar ist
     */
    async checkStreetViewAvailability(lat, lng) {
        try {
            // Verwende Street View Service API für Verfügbarkeitsprüfung
            const url = `https://maps.googleapis.com/maps/api/streetview/metadata?location=${lat},${lng}&key=${this.apiKey}`;
            const response = await fetch(url);
            const data = await response.json();

            return {
                available: data.status === 'OK',
                location: data.location,
                panoId: data.pano_id,
                date: data.date
            };
        } catch (error) {
            console.error('Street View availability check failed:', error);
            return { available: false };
        }
    }
}

// Integration in bestehende API-Routen
const streetViewService = new InteractiveStreetViewService(process.env.GOOGLE_MAPS_API_KEY);

// Aktualisierte API-Endpunkte
app.get('/api/locations/:id/streetview', async (req, res) => {
    try {
        const { id } = req.params;
        const {
            heading = Math.floor(Math.random() * 360),
            pitch = 0,
            fov = 90,
            multiple = false,
            responsive = true
        } = req.query;

        // Hole Location aus Datenbank
        const location = await getLocationById(id);

        if (!location) {
            return res.status(404).json({
                success: false,
                error: 'Location nicht gefunden'
            });
        }

        // Prüfe Street View Verfügbarkeit
        const availability = await streetViewService.checkStreetViewAvailability(
            location.coordinates.latitude,
            location.coordinates.longitude
        );

        if (!availability.available) {
            return res.status(404).json({
                success: false,
                error: 'Street View nicht verfügbar',
                fallback: true
            });
        }

        // Generiere interaktive Street View Response
        const streetViewData = streetViewService.generateStreetViewResponse(
            location.coordinates.latitude,
            location.coordinates.longitude,
            parseInt(heading),
            parseInt(pitch),
            parseInt(fov),
            responsive === 'true'
        );

        res.json({
            success: true,
            data: {
                location: {
                    id: location.id,
                    coordinates: location.coordinates
                },
                // NEUE STRUKTUR: Interaktive URLs statt statische
                interactiveStreetView: streetViewData,

                // Legacy-Support für bestehende Clients
                streetViewUrls: streetViewData.responsive || {
                    mobile: streetViewData.embedUrl,
                    tablet: streetViewData.embedUrl,
                    desktop: streetViewData.embedUrl
                }
            }
        });

    } catch (error) {
        console.error('Street View API Error:', error);
        res.status(500).json({
            success: false,
            error: 'Interner Server-Fehler'
        });
    }
});

// Neuer Endpunkt für dynamische Navigation
app.post('/api/streetview/navigate', async (req, res) => {
    try {
        const {
            currentLat,
            currentLng,
            direction, // 'forward', 'backward', 'left', 'right'
            heading,
            stepSize = 25
        } = req.body;

        // Berechne neue Position basierend auf Richtung
        let newHeading = heading;
        let newLat = currentLat;
        let newLng = currentLng;

        switch (direction) {
            case 'forward':
                // Bewege in aktuelle Blickrichtung
                const forwardRad = (heading * Math.PI) / 180;
                newLat += (Math.cos(forwardRad) * stepSize) / 111000;
                newLng += (Math.sin(forwardRad) * stepSize) / (111000 * Math.cos((currentLat * Math.PI) / 180));
                break;

            case 'backward':
                // Bewege rückwärts
                const backwardRad = ((heading + 180) * Math.PI) / 180;
                newLat += (Math.cos(backwardRad) * stepSize) / 111000;
                newLng += (Math.sin(backwardRad) * stepSize) / (111000 * Math.cos((currentLat * Math.PI) / 180));
                break;

            case 'left':
                newHeading = (heading - 90 + 360) % 360;
                break;

            case 'right':
                newHeading = (heading + 90) % 360;
                break;
        }

        // Prüfe ob neue Position gültig ist
        const availability = await streetViewService.checkStreetViewAvailability(newLat, newLng);

        if (!availability.available) {
            return res.status(404).json({
                success: false,
                error: 'Street View an neuer Position nicht verfügbar'
            });
        }

        // Generiere neue interaktive Street View URL
        const streetViewData = streetViewService.generateStreetViewResponse(
            newLat,
            newLng,
            newHeading
        );

        res.json({
            success: true,
            data: {
                newLocation: {
                    latitude: newLat,
                    longitude: newLng
                },
                heading: newHeading,
                interactiveStreetView: streetViewData,
                available: true
            }
        });

    } catch (error) {
        console.error('Street View Navigation Error:', error);
        res.status(500).json({
            success: false,
            error: 'Navigation fehlgeschlagen'
        });
    }
});

module.exports = { InteractiveStreetViewService, streetViewService };
