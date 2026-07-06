const isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
window.RENTHUB_CONFIG = {
    apiBaseUrl: isLocal 
        ? 'http://localhost:8080/api/v1' 
        : 'https://rent-hub-production-b097.up.railway.app/api/v1',
    version: '1.0.0',
    env: isLocal ? 'development' : 'production'
};
