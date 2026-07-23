import { cpSync, existsSync, mkdirSync, rmSync } from "node:fs"
import { dirname, join } from "node:path"
import { fileURLToPath } from "node:url"

const projectRoot = join(dirname(fileURLToPath(import.meta.url)), "..")
const outputRoot = join(projectRoot, "src")
const sourceDirectories = ["components", "pages", "static", "store", "utils"]
const sourceFiles = ["App.vue", "main.js", "manifest.json", "pages.json", "uni.scss"]

rmSync(outputRoot, { recursive: true, force: true })
mkdirSync(outputRoot, { recursive: true })

for (const directory of sourceDirectories) {
  const source = join(projectRoot, directory)
  if (existsSync(source)) {
    cpSync(source, join(outputRoot, directory), { recursive: true })
  }
}

for (const file of sourceFiles) {
  const source = join(projectRoot, file)
  if (!existsSync(source)) {
    throw new Error(`Missing required uni-app source file: ${file}`)
  }
  cpSync(source, join(outputRoot, file))
}

console.log("Prepared HBuilderX sources for the local uni-app CLI build.")
