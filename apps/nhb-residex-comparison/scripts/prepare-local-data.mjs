import { cp, mkdir } from 'node:fs/promises';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDirectory = resolve(fileURLToPath(new URL('.', import.meta.url)));
const repositoryRoot = resolve(scriptDirectory, '../../..');
const source = resolve(repositoryRoot, 'data/nhb-residex/test');
const destination = resolve(repositoryRoot, 'apps/nhb-residex-comparison/public/nhb-residex');

await mkdir(destination, { recursive: true });
await cp(source, destination, { recursive: true });
console.log(`Copied local NHB test data from ${source} to ${destination}`);
