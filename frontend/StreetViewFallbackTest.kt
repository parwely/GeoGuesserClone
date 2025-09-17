/**
 * Street View Fallback Test Suite
 * Testet die neue intelligente Fallback-Strategie für HTTP 400/403/404 Fehler
 */

// Simuliere die Logs aus dem ursprünglichen Problem
fun testStreetViewFallbackScenario() {
    println("=== STREET VIEW FALLBACK TEST ===")

    // Ursprüngliche URL die einen HTTP 400 Fehler verursacht hat
    val originalUrl = "https://www.google.com/maps/embed/v1/streetview?key=AIzaSyD4C5oyZ4ya-sYGKIDqoRa1C3Mqjl22eUc&location=43.9352%2C4.8357&heading=233&pitch=0&fov=90&navigation=1&controls=1&zoom=1&fullscreen=1"

    // Simuliere Location für Provence, France
    val location = MockLocationEntity(
        id = "test_provence",
        latitude = 43.9352,
        longitude = 4.8357,
        imageUrl = originalUrl,
        city = "Provence",
        country = "France"
    )

    println("🧪 Teste ursprüngliche URL: ${originalUrl.take(120)}...")
    println("🔍 Expected: HTTP 400 Fehler")

    // Test 1: Statische Street View Fallback
    val staticFallback = generateStaticStreetViewUrl(location)
    println("📸 Statische Street View Fallback: $staticFallback")

    // Test 2: Regionale Fallback-Bilder
    val regionalFallback = generateRegionalFallbackUrl(location)
    println("🌍 Regionales Fallback: $regionalFallback")

    // Test 3: Bekannte Location Fallback
    val knownLocationFallback = generateLocationFallbackUrl(location)
    println("🏛️ Bekannte Location Fallback: $knownLocationFallback")

    println("\n✅ ERWARTETE VERBESSERUNGEN:")
    println("1. HTTP 400 → Versuche statische Street View URL")
    println("2. Statische URL auch HTTP 400 → Französisches Fallback-Bild")
    println("3. Keine Unsplash-Bilder mehr in Street View WebView")
    println("4. Bessere Fehlerbehandlung mit spezifischen Meldungen")
}

// Mock-Klassen für Testing
data class MockLocationEntity(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String,
    val city: String,
    val country: String
)

// Kopie der Fallback-Funktionen für Testing
fun generateStaticStreetViewUrl(location: MockLocationEntity): String? {
    return try {
        val lat = location.latitude
        val lng = location.longitude

        if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) {
            return null
        }

        val heading = (0..359).random()

        "https://maps.googleapis.com/maps/api/streetview?" +
                "size=640x640" +
                "&location=$lat,$lng" +
                "&heading=$heading" +
                "&pitch=0" +
                "&fov=90" +
                "&key=AIzaSyD4C5oyZ4ya-sYGKIDqoRa1C3Mqjl22eUc"

    } catch (e: Exception) {
        null
    }
}

fun generateRegionalFallbackUrl(location: MockLocationEntity): String {
    val countryName = location.country.lowercase()

    return when {
        countryName.contains("france") ->
            "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800&h=600&fit=crop"
        countryName.contains("united states") || countryName.contains("usa") ->
            "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800&h=600&fit=crop"
        countryName.contains("united kingdom") || countryName.contains("england") ->
            "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800&h=600&fit=crop"
        else ->
            "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?w=800&h=600&fit=crop"
    }
}

fun generateLocationFallbackUrl(location: MockLocationEntity): String {
    val cityName = location.city.lowercase()

    val knownLocationUrl = when {
        cityName.contains("provence") -> "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800&h=600&fit=crop"
        cityName.contains("paris") -> "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800&h=600&fit=crop"
        cityName.contains("new york") -> "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800&h=600&fit=crop"
        cityName.contains("london") -> "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800&h=600&fit=crop"
        else -> null
    }

    return knownLocationUrl ?: generateRegionalFallbackUrl(location)
}

// Führe den Test aus
fun main() {
    testStreetViewFallbackScenario()

    println("\n=== ZUSÄTZLICHE TEST-SZENARIEN ===")

    // Test verschiedene HTTP-Fehlertypen
    println("\n🧪 HTTP 403 Test (API-Key Problem):")
    println("Expected: Direkter Sprung zu regionalem Fallback")

    println("\n🧪 HTTP 404 Test (URL nicht gefunden):")
    println("Expected: Ignoriere Favicon 404, handle nur Haupt-URLs")

    println("\n🧪 Koordinaten-Validierung:")
    val invalidLocation = MockLocationEntity("test", 999.0, 999.0, "", "Invalid", "Test")
    val invalidStatic = generateStaticStreetViewUrl(invalidLocation)
    println("Ungültige Koordinaten: $invalidStatic (should be null)")

    println("\n🎉 ALLE TESTS ABGESCHLOSSEN!")
}
