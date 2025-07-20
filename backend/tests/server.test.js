const request = require('supertest');
const app = require('../src/server');

describe('Server Setup', () => {
  test('should respond to root endpoint', async () => {
    const response = await request(app)
      .get('/')
      .expect(200);
    
    expect(response.body.message).toBe('GeoGuessr Clone API');
    expect(response.body.status).toBe('active');
  });

  test('should handle 404 for unknown routes', async () => {
    const response = await request(app)
      .get('/unknown-route')
      .expect(404);
    
    expect(response.body.error).toBe('Route not found');
  });
});