const database = require("../database/connection");
const cacheService = require("./cacheService");

class LocationService {
  constructor() {
    this.difficulties = {
      1: "Very Easy",
      2: "Easy",
      3: "Medium",
      4: "Hard",
      5: "Very Hard",
    };

    this.categories = [
      "landmark",
      "urban", 
      "rural",
      "desert",
      "mountain",
      "coastal",
      "forest",
      "arctic",
      "nature"
    ];
  }

  // Get random location(s) based on criteria
  async getRandomLocations(options = {}) {
    try {
      // Check cache first
      const cacheKey = cacheService.generateLocationKey(options);
      const cached = cacheService.get(cacheKey);
      if (cached) {
        return cached;
      }

      console.log(
        "🔍 LocationService: Starting getRandomLocations with options:",
        options
      );

      // ❌ Remove this line - pool handles connections automatically
      // await database.connect();

      const {
        count = 1,
        difficulty = null,
        category = null,
        excludeIds = [],
        country = null,
      } = options;

      // Optimized query with fewer columns for better performance
      let query = `
      SELECT 
        id,
        name,
        country,
        city,
        ST_X(coordinates) as longitude,
        ST_Y(coordinates) as latitude,
        difficulty,
        category,
        view_count
      FROM locations
      WHERE 1=1
    `;

      const params = [];
      let paramCount = 0;

      // Add filters
      if (difficulty) {
        paramCount++;
        query += ` AND difficulty = $${paramCount}`;
        params.push(difficulty);
      }

      if (category) {
        paramCount++;
        query += ` AND category = $${paramCount}`;
        params.push(category);
      }

      if (country) {
        paramCount++;
        query += ` AND country ILIKE $${paramCount}`;
        params.push(`%${country}%`);
      }

      if (excludeIds.length > 0) {
        paramCount++;
        query += ` AND id != ALL($${paramCount})`;
        params.push(excludeIds);
      }

      // Random selection with limit
      query += ` ORDER BY RANDOM() LIMIT ${count}`;

      const result = await database.query(query, params);

      // Don't update view count for cached results
      const locations = result.rows.map((row) => this.formatLocation(row));

      // Cache results for 5 minutes
      cacheService.set(cacheKey, locations, 300);

      // Update view count asynchronously (don't wait)
      if (result.rows.length > 0) {
        const locationIds = result.rows.map((row) => row.id);
        database
          .query(
            "UPDATE locations SET view_count = view_count + 1 WHERE id = ANY($1)",
            [locationIds]
          )
          .catch((err) => console.error("View count update failed:", err));
      }

      return locations;
    } catch (error) {
      console.error("❌ LocationService: getRandomLocations failed:", error);
      throw error;
    }
  }

  // Get location by ID
  async getLocationById(id) {
    try {
      // Check cache first
      const cacheKey = cacheService.generateLocationByIdKey(id);
      const cached = cacheService.get(cacheKey);
      if (cached) {
        return cached;
      }

      // ❌ Remove this line - pool handles connections automatically
      // await database.connect();

      const result = await database.query(
        `
        SELECT 
          id,
          name,
          country,
          city,
          ST_X(coordinates) as longitude,
          ST_Y(coordinates) as latitude,
          difficulty,
          category,
          image_urls,
          hints,
          view_count,
          created_at
        FROM locations
        WHERE id = $1
      `,
        [id]
      );

      if (result.rows.length === 0) {
        throw new Error("Location not found");
      }

      const location = this.formatLocation(result.rows[0]);

      // Cache the location for 10 minutes
      cacheService.set(cacheKey, location, 600);

      // Update view count asynchronously (don't wait)
      database
        .query(
          "UPDATE locations SET view_count = view_count + 1 WHERE id = $1",
          [id]
        )
        .catch((err) => console.error("View count update failed:", err));

      return location;
    } catch (error) {
      console.error("❌ LocationService: getLocationById failed:", error);
      throw error;
    }
  }

  // Get locations by difficulty
  async getLocationsByDifficulty(difficulty, limit = 10) {
    try {
      await database.connect();

      if (difficulty < 1 || difficulty > 5) {
        throw new Error("Difficulty must be between 1 and 5");
      }

      const result = await database.query(
        `
        SELECT 
          id,
          name,
          country,
          city,
          ST_X(coordinates) as longitude,
          ST_Y(coordinates) as latitude,
          difficulty,
          category,
          image_urls,
          hints,
          view_count
        FROM locations
        WHERE difficulty = $1
        ORDER BY RANDOM()
        LIMIT $2
      `,
        [difficulty, limit]
      );

      return {
        difficulty,
        difficultyName: this.difficulties[difficulty],
        count: result.rows.length,
        locations: result.rows.map((row) => this.formatLocation(row)),
      };
    } catch (error) {
      console.error(
        "❌ LocationService: getLocationsByDifficulty failed:",
        error
      );
      throw error;
    }
  }

  // Get locations by category
  async getLocationsByCategory(category, limit = 10) {
    try {
      await database.connect();

      if (!this.categories.includes(category)) {
        throw new Error(
          `Invalid category. Must be one of: ${this.categories.join(", ")}`
        );
      }

      const result = await database.query(
        `
        SELECT 
          id,
          name,
          country,
          city,
          ST_X(coordinates) as longitude,
          ST_Y(coordinates) as latitude,
          difficulty,
          category,
          image_urls,
          hints,
          view_count
        FROM locations
        WHERE category = $1
        ORDER BY RANDOM()
        LIMIT $2
      `,
        [category, limit]
      );

      return {
        category,
        count: result.rows.length,
        locations: result.rows.map((row) => this.formatLocation(row)),
      };
    } catch (error) {
      console.error(
        "❌ LocationService: getLocationsByCategory failed:",
        error
      );
      throw error;
    }
  }

  // Search locations near coordinates
  async getLocationsNear(latitude, longitude, radiusKm = 100, limit = 10) {
    try {
      await database.connect();

      const result = await database.query(
        `
        SELECT 
          id,
          name,
          country,
          city,
          ST_X(coordinates) as longitude,
          ST_Y(coordinates) as latitude,
          difficulty,
          category,
          image_urls,
          hints,
          view_count,
          ST_Distance(
            coordinates,
            ST_SetSRID(ST_MakePoint($1, $2), 4326)
          ) / 1000 as distance_km
        FROM locations
        WHERE ST_DWithin(
          coordinates,
          ST_SetSRID(ST_MakePoint($1, $2), 4326),
          $3 * 1000
        )
        ORDER BY distance_km
        LIMIT $4
      `,
        [longitude, latitude, radiusKm, limit]
      );

      return {
        searchCenter: { latitude, longitude },
        radiusKm,
        count: result.rows.length,
        locations: result.rows.map((row) => ({
          ...this.formatLocation(row),
          distanceKm: parseFloat(row.distance_km.toFixed(2)),
        })),
      };
    } catch (error) {
      console.error("❌ LocationService: getLocationsNear failed:", error);
      throw error;
    }
  }

  // Get location statistics
  async getLocationStats() {
    try {
      // Check cache first - stats change infrequently
      const cacheKey = cacheService.generateStatsKey();
      const cached = cacheService.get(cacheKey);
      if (cached) {
        console.log("✅ Cache hit for stats");
        return cached;
      }
      await database.connect();

      const [
        totalResult,
        difficultyResult,
        categoryResult,
        countryResult,
        popularResult,
      ] = await Promise.all([
        // Total count
        database.query("SELECT COUNT(*) as total FROM locations"),

        // By difficulty
        database.query(`
          SELECT difficulty, COUNT(*) as count
          FROM locations 
          GROUP BY difficulty 
          ORDER BY difficulty
        `),

        // By category
        database.query(`
          SELECT category, COUNT(*) as count
          FROM locations 
          GROUP BY category 
          ORDER BY count DESC
        `),

        // By country
        database.query(`
          SELECT country, COUNT(*) as count
          FROM locations 
          GROUP BY country 
          ORDER BY count DESC
          LIMIT 10
        `),

        // Most viewed
        database.query(`
          SELECT 
            id,
            name,
            country,
            category,
            difficulty,
            view_count
          FROM locations 
          ORDER BY view_count DESC
          LIMIT 5
        `),
      ]);

      return {
        total: parseInt(totalResult.rows[0].total),
        byDifficulty: difficultyResult.rows.map((row) => ({
          difficulty: row.difficulty,
          difficultyName: this.difficulties[row.difficulty],
          count: parseInt(row.count),
        })),
        byCategory: categoryResult.rows.map((row) => ({
          category: row.category,
          count: parseInt(row.count),
        })),
        byCountry: countryResult.rows.map((row) => ({
          country: row.country,
          count: parseInt(row.count),
        })),
        mostPopular: popularResult.rows,
      };
    } catch (error) {
      console.error("❌ LocationService: getLocationStats failed:", error);
      throw error;
    }
  }

  // Calculate distance between two locations
  async calculateDistance(location1Id, location2Id) {
    try {
      await database.connect();

      const result = await database.query(
        `
        SELECT 
          l1.name as location1_name,
          l1.country as location1_country,
          l2.name as location2_name,
          l2.country as location2_country,
          ST_Distance(l1.coordinates, l2.coordinates) / 1000 as distance_km
        FROM locations l1, locations l2
        WHERE l1.id = $1 AND l2.id = $2
      `,
        [location1Id, location2Id]
      );

      if (result.rows.length === 0) {
        throw new Error("One or both locations not found");
      }

      const row = result.rows[0];
      return {
        location1: `${row.location1_name}, ${row.location1_country}`,
        location2: `${row.location2_name}, ${row.location2_country}`,
        distanceKm: parseFloat(row.distance_km.toFixed(2)),
      };
    } catch (error) {
      console.error("❌ LocationService: calculateDistance failed:", error);
      throw error;
    }
  }

  // Format location object for API response
  formatLocation(row) {
    return {
      id: row.id,
      name: row.name,
      country: row.country,
      city: row.city,
      coordinates: {
        latitude: parseFloat(row.latitude),
        longitude: parseFloat(row.longitude),
      },
      difficulty: row.difficulty,
      difficultyName: this.difficulties[row.difficulty],
      category: row.category,
      imageUrls: row.image_urls,
      hints: row.hints,
      viewCount: row.view_count,
      createdAt: row.created_at,
    };
  }

  // Test database connection and table existence
  async testConnection() {
    try {
      await database.connect();
      const result = await database.query(
        "SELECT COUNT(*) as count FROM locations"
      );
      return true;
    } catch (error) {
      throw error;
    }
  }

  // Validate location query parameters
  validateLocationQuery(query) {
    const errors = [];

    if (
      query.difficulty &&
      (isNaN(query.difficulty) || query.difficulty < 1 || query.difficulty > 5)
    ) {
      errors.push("Difficulty must be a number between 1 and 5");
    }

    if (query.category && !this.categories.includes(query.category)) {
      errors.push(`Category must be one of: ${this.categories.join(", ")}`);
    }

    if (
      query.count &&
      (isNaN(query.count) || query.count < 1 || query.count > 50)
    ) {
      errors.push("Count must be a number between 1 and 50");
    }

    return errors;
  }
}

module.exports = new LocationService();
