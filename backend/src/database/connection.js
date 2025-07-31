const { Pool } = require("pg");
require("dotenv").config();

class Database {
  constructor() {
    this.pool = null;
    this.connected = false;
  }

  async connect() {
    try {
      // Enhanced connection configuration for Neon
      const config = {
        connectionString: process.env.DATABASE_URL,
        ssl:
          process.env.DB_SSL === "true"
            ? {
                rejectUnauthorized: false,
              }
            : false,
        max: 10,
        idleTimeoutMillis: 30000,
        connectionTimeoutMillis: 10000,
      };

      console.log("🔍 Connecting to database...");
      console.log("Host:", process.env.DB_HOST || "from connection string");
      console.log("SSL enabled:", config.ssl ? "Yes" : "No");

      this.pool = new Pool(config);

      // Test the connection
      const client = await this.pool.connect();
      console.log("✅ Connected to PostgreSQL database");

      // Check PostgreSQL version
      const result = await client.query("SELECT version()");
      console.log(
        "📊 PostgreSQL version:",
        result.rows[0].version.split(" ")[1]
      );

      client.release();
      this.connected = true;

      return this.pool;
    } catch (error) {
      console.error("❌ Database connection error:", error.message);
      console.error("🔍 Error details:", {
        code: error.code,
        host: error.hostname || process.env.DB_HOST,
        port: error.port || process.env.DB_PORT,
      });
      throw error;
    }
  }

  async disconnect() {
    if (this.pool) {
      await this.pool.end();
      console.log("🔌 Disconnected from database");
      this.connected = false;
    }
  }

  async query(text, params) {
    if (!this.pool) {
      await this.connect();
    }

    try {
      const start = Date.now();
      const result = await this.pool.query(text, params);
      const duration = Date.now() - start;

      // Only log slow queries to reduce noise
      if (duration > 100) {
        console.log("🔍 Executed query:", {
          text: text.substring(0, 100) + (text.length > 100 ? "..." : ""),
          duration: `${duration}ms`,
          rows: result.rowCount,
        });
      }
      return result;
    } catch (error) {
      console.error("❌ Query error:", error.message);
      console.error("Query text:", text.substring(0, 200));
      throw error;
    }
  }

  async getClient() {
    if (!this.pool) {
      await this.connect();
    }
    return await this.pool.connect();
  }

  // Add method to check if pool is ready
  isReady() {
    return this.pool !== null;
  }

  // Add method to get pool stats
  getPoolStats() {
    if (!this.pool) return null;
    return {
      totalCount: this.pool.totalCount,
      idleCount: this.pool.idleCount,
      waitingCount: this.pool.waitingCount,
    };
  }
}

// Export singleton instance
const database = new Database();
module.exports = database;
