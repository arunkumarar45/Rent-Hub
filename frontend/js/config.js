/**
 * RentHub Runtime Configuration
 *
 * LOCAL DEVELOPMENT: points to http://localhost:8080
 * PRODUCTION (Vercel): This file is replaced/overridden by vercel.json rewrites
 *   OR set window.RENTHUB_CONFIG.apiBaseUrl to your Railway backend URL.
 *
 * For production, update the apiBaseUrl below to your Railway deployment URL
 * after your first Railway deploy. Vercel will serve this file as-is.
 */
window.RENTHUB_CONFIG = {
    // ← Replace this with your Railway backend URL after deployment
    // Example: 'https://renthub-backend.up.railway.app/api/v1'
    apiBaseUrl: 'http://localhost:8080/api/v1',
    version: '1.0.0',
    env: 'development'
};
