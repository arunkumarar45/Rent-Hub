/**
 * RentHub Runtime Configuration
 *
 * LOCAL DEVELOPMENT: change apiBaseUrl to http://localhost:8080/api/v1
 * PRODUCTION: points to Railway backend deployment
 */
window.RENTHUB_CONFIG = {
    apiBaseUrl: 'https://rent-hub-production-b097.up.railway.app/api/v1',
    version: '1.0.0',
    env: 'production'
};
