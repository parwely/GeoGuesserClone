const database = require('./connection');
const fs = require('fs').promises;
const path = require('path');

class MigrationRunner {
  constructor() {
    this.migrationsPath = path.join(__dirname, 'migrations');
  }

  async createMigrationsTable() {
    await database.query(`
      CREATE TABLE IF NOT EXISTS schema_migrations (
        id SERIAL PRIMARY KEY,
        migration_name VARCHAR(255) NOT NULL UNIQUE,
        applied_at TIMESTAMP DEFAULT NOW()
      );
    `);
  }

  async getAppliedMigrations() {
    const result = await database.query(
      'SELECT migration_name FROM schema_migrations ORDER BY migration_name'
    );
    return result.rows.map(row => row.migration_name);
  }

  async getMigrationFiles() {
    try {
      const files = await fs.readdir(this.migrationsPath);
      return files
        .filter(file => file.endsWith('.js'))
        .sort();
    } catch (error) {
      console.error('❌ Could not read migrations directory:', error.message);
      return [];
    }
  }

  async runMigrations() {
    try {
      console.log('🚀 Starting database migrations...');
      
      await database.connect();
      await this.createMigrationsTable();
      
      const appliedMigrations = await this.getAppliedMigrations();
      const migrationFiles = await this.getMigrationFiles();
      
      console.log(`📂 Found ${migrationFiles.length} migration files`);
      console.log(`✅ ${appliedMigrations.length} migrations already applied`);
      
      for (const file of migrationFiles) {
        const migrationName = path.basename(file, '.js');
        
        if (!appliedMigrations.includes(migrationName)) {
          console.log(`🔄 Running migration: ${migrationName}`);
          
          const migrationPath = path.join(this.migrationsPath, file);
          const migration = require(migrationPath);
          
          await migration.up();
          
          await database.query(
            'INSERT INTO schema_migrations (migration_name) VALUES ($1)',
            [migrationName]
          );
          
          console.log(`✅ Migration completed: ${migrationName}`);
        } else {
          console.log(`⏭️  Skipping applied migration: ${migrationName}`);
        }
      }
      
      console.log('🎉 All migrations completed successfully!');
      
    } catch (error) {
      console.error('❌ Migration failed:', error.message);
      throw error;
    }
  }

  async rollbackLastMigration() {
    try {
      await database.connect();
      
      const result = await database.query(
        'SELECT migration_name FROM schema_migrations ORDER BY applied_at DESC LIMIT 1'
      );
      
      if (result.rows.length === 0) {
        console.log('ℹ️  No migrations to rollback');
        return;
      }
      
      const migrationName = result.rows[0].migration_name;
      console.log(`🔄 Rolling back migration: ${migrationName}`);
      
      const migrationPath = path.join(this.migrationsPath, `${migrationName}.js`);
      const migration = require(migrationPath);
      
      await migration.down();
      
      await database.query(
        'DELETE FROM schema_migrations WHERE migration_name = $1',
        [migrationName]
      );
      
      console.log(`✅ Rollback completed: ${migrationName}`);
      
    } catch (error) {
      console.error('❌ Rollback failed:', error.message);
      throw error;
    }
  }
}

// CLI interface
if (require.main === module) {
  const runner = new MigrationRunner();
  
  const command = process.argv[2];
  
  switch (command) {
    case 'up':
      runner.runMigrations()
        .then(() => process.exit(0))
        .catch(() => process.exit(1));
      break;
      
    case 'down':
      runner.rollbackLastMigration()
        .then(() => process.exit(0))
        .catch(() => process.exit(1));
      break;
      
    default:
      console.log('Usage: node migrate.js [up|down]');
      process.exit(1);
  }
}

module.exports = MigrationRunner;