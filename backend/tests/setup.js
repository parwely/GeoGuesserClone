// Global test setup
process.env.NODE_ENV = 'test';

// Increase timeout for database operations
jest.setTimeout(30000);

// Global teardown to ensure clean exit
afterAll(async () => {
  // Give time for async operations to complete
  await new Promise(resolve => setTimeout(resolve, 100));
});