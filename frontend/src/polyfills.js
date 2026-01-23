// Polyfill for SockJS compatibility in browser environment
// Fixes: Uncaught ReferenceError: global is not defined

if (typeof global === 'undefined') {
  window.global = window;
}

// Additional polyfills for Node.js modules used in browser
if (typeof process === 'undefined') {
  window.process = {
    env: { NODE_ENV: import.meta.env.MODE }
  };
}

export {};
