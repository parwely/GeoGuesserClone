const { Pool } = require('pg');
require('dotenv').config();

class Database {
  constructor() {
    this.pool = null;
    this.connected = false;
  }

  async connect() {
    try {
      // Connection configuration
      const config = {
        connectionString: process.env.DATABASE_URL,
        ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false,
        max: 10, // Maximum number of connections in pool
        idleTimeoutMillis: 30000, // Close idle connections after 30 seconds
        connectionTimeoutMillis: 10000, // Return error after 10 seconds if connection could not be established
      };

      this.pool = new Pool(config);

      // Test the connection
      const client = await this.pool.connect();
      console.log('✅ Connected to Neon PostgreSQL database');
      
      // Check PostgreSQL version
      const result = await client.query('SELECT version()');
      console.log('📊 PostgreSQL version:', result.rows[0].version.split(' ')[1]);
      
      client.release();
      this.connected = true;
      
      return this.pool;
    } catch (error) {
      console.error('❌ Database connection error:', error.message);
      throw error;
    }
  }

  async disconnect() {
    if (this.pool) {
      await this.pool.end();
      console.log('🔌 Disconnected from database');
      this.connected = false;
    }
  }

  async query(text, params) {
    if (!this.connected) {
      throw new Error('Database not connected');
    }
    
    try {
      const start = Date.now();
      const result = await this.pool.query(text, params);
      const duration = Date.now() - start;
      
      console.log('🔍 Executed query:', { text, duration: `${duration}ms`, rows: result.rowCount });
      return result;
    } catch (error) {
      console.error('❌ Query error:', error.message);
      throw error;
    }
  }

  async getClient() {
    if (!this.connected) {
      throw new Error('Database not connected');
    }
    return await this.pool.connect();
  }
}

// Export singleton instance
const database = new Database();
module.exports = database;