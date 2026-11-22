import { cp, mkdir } from "node:fs/promises";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDirectory = resolve(fileURLToPath(new URL(".", import.meta.url)));
const repositoryRoot = resolve(scriptDirectory, "../../..");
const source = resolve(repositoryRoot, "data/stock-screener/test");
const destination = resolve(repositoryRoot, "apps/stock-screener/public/screener-data/test");

await mkdir(destination, { recursive: true });
await cp(source, destination, { recursive: true });
console.log(`Copied local test data from ${source} to ${destination}`);
