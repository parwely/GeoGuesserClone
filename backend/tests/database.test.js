const database = require('../src/database/connection');

describe('Database Connection', () => {
  beforeAll(async () => {
    await database.connect();
  });

  afterAll(async () => {
    await database.disconnect();
  });

  test('should connect to database', async () => {
    const result = await database.query('SELECT 1 as test');
    expect(result.rows[0].test).toBe(1);
  });

  test('should have PostGIS extension', async () => {
    const result = await database.query("SELECT PostGIS_Version()");
    expect(result.rows[0].postgis_version).toBeDefined();
  });

  test('should have sample locations', async () => {
    const result = await database.query('SELECT COUNT(*) as count FROM locations');
    expect(parseInt(result.rows[0].count)).toBeGreaterThan(0);
  });

  test('should perform spatial query', async () => {
    const result = await database.query(`
      SELECT name, country, 
             ST_X(coordinates) as longitude, 
             ST_Y(coordinates) as latitude
      FROM locations 
      LIMIT 1
    `);
    
    expect(result.rows).toHaveLength(1);
    expect(result.rows[0].longitude).toBeDefined();
    expect(result.rows[0].latitude).toBeDefined();
  });
});