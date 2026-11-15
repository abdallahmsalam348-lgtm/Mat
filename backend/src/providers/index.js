import { createApiFootballProvider } from './apiFootball.js';

const registry = {
  'api-football': createApiFootballProvider,
  // 'sportmonks': createSportmonksProvider,        // add new providers here (see providers/README.md)
  // 'football-data': createFootballDataProvider,
};

export function createProvider(name, upstream) {
  const factory = registry[name];
  if (!factory) {
    throw new Error(`Unknown PROVIDER "${name}". Available: ${Object.keys(registry).join(', ')}`);
  }
  return factory(upstream);
}
