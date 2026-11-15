import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { loadConfig } from './config.js';
import { loadDotEnv } from './dotenv.js';
import { createApp } from './app.js';

loadDotEnv(join(dirname(fileURLToPath(import.meta.url)), '..', '.env'));
const config = loadConfig();
const app = createApp(config, { log: (line) => console.log(line) });

app.listen(config.port, '0.0.0.0', () => {
  console.log(`MATCHLY backend listening on :${config.port} (provider=${config.provider}, plan preset=${config.plan}, key ${config.apiKey ? 'configured' : 'NOT configured'})`);
});
