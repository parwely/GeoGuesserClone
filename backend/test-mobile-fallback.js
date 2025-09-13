// Test script for mobile fallback functionality
require('dotenv').config();

const streetViewService = require('./src/services/streetViewService');

// Test coordinates (Sydney Opera House from our database)
const testCoordinates = {
  latitude: -33.8568,
  longitude: 151.2153
};

console.log('🧪 Testing Street View mobile fallback functionality\n');

// Test 1: Desktop user agent (no fallback expected)
console.log('=== TEST 1: Desktop User Agent ===');
const desktopUrls = streetViewService.generateResponsiveUrlsWithContext(
  testCoordinates.latitude,
  testCoordinates.longitude,
  90,
  {
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    deviceType: 'desktop',
    preferHighQuality: false
  }
);

console.log('Desktop URLs:');
console.log('- Mobile fallback used:', !!desktopUrls.mobileFallback);
console.log('- Mobile URL size:', desktopUrls.mobile.includes('400x400') ? '400x400' : 'other');
console.log('- Tablet URL size:', desktopUrls.tablet.includes('640x640') ? '640x640' : 'other');

// Test 2: Mobile user agent with non-problematic location
console.log('\n=== TEST 2: Mobile User Agent - Good Location ===');
const mobileGoodUrls = streetViewService.generateResponsiveUrlsWithContext(
  testCoordinates.latitude,
  testCoordinates.longitude,
  90,
  {
    userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15',
    deviceType: 'mobile',
    preferHighQuality: false
  }
);

console.log('Mobile (Good Location) URLs:');
console.log('- Mobile fallback used:', !!mobileGoodUrls.mobileFallback);
console.log('- Mobile URL size:', mobileGoodUrls.mobile.includes('400x400') ? '400x400' : (mobileGoodUrls.mobile.includes('640x640') ? '640x640' : 'other'));

// Test 3: Mobile user agent with problematic location (Sahara Desert)
console.log('\n=== TEST 3: Mobile User Agent - Problematic Location (Sahara) ===');
const mobileProblematicUrls = streetViewService.generateResponsiveUrlsWithContext(
  25.0, // Sahara Desert latitude
  0.0,  // Sahara Desert longitude
  90,
  {
    userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15',
    deviceType: 'mobile',
    preferHighQuality: false
  }
);

console.log('Mobile (Problematic Location) URLs:');
console.log('- Mobile fallback used:', !!mobileProblematicUrls.mobileFallback);
console.log('- Mobile URL size:', mobileProblematicUrls.mobile.includes('400x400') ? '400x400' : (mobileProblematicUrls.mobile.includes('640x640') ? '640x640' : 'other'));
console.log('- Fallback applied due to location:', streetViewService.shouldUseMobileFallback(25.0, 0.0));

// Test 4: High quality preference
console.log('\n=== TEST 4: Mobile with High Quality Preference ===');
const mobileHighQualityUrls = streetViewService.generateResponsiveUrlsWithContext(
  testCoordinates.latitude,
  testCoordinates.longitude,
  90,
  {
    userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15',
    deviceType: 'mobile',
    preferHighQuality: true
  }
);

console.log('Mobile (High Quality) URLs:');
console.log('- Mobile URL size:', mobileHighQualityUrls.mobile.includes('400x400') ? '400x400' : (mobileHighQualityUrls.mobile.includes('640x640') ? '640x640' : 'other'));
console.log('- High quality applied:', mobileHighQualityUrls.mobile === mobileHighQualityUrls.tablet);

console.log('\n✅ Mobile fallback tests completed!');