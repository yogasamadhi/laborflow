import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import { resolve, dirname, relative, sep } from "node:path";

const repositoryRoot = resolve(import.meta.dirname, "../..");
const ignoredDirectories = new Set([".git", "dist", "node_modules", "target"]);
const markdownFiles = walk(repositoryRoot).filter((path) => path.endsWith(".md"));
const errors = [];

for (const markdownFile of markdownFiles) {
  const content = readFileSync(markdownFile, "utf8");
  for (const target of markdownTargets(content)) {
    const localTarget = localTargetPath(target);
    if (!localTarget) continue;

    const resolvedTarget = localTarget.startsWith("/")
      ? resolve(repositoryRoot, "." + localTarget)
      : resolve(dirname(markdownFile), localTarget);
    if (!isInsideRepository(resolvedTarget) || !existsSync(resolvedTarget)) {
      errors.push(`Broken local Markdown link in ${relative(repositoryRoot, markdownFile)}: ${target}`);
    }
  }
}

const migrationDirectory = resolve(repositoryRoot, "server/src/main/resources/db/migration");
const migrationVersions = new Set();
for (const fileName of readdirSync(migrationDirectory).filter((name) => name.endsWith(".sql"))) {
  const match = /^V([1-9]\d*)__([a-z0-9]+(?:_[a-z0-9]+)*)\.sql$/.exec(fileName);
  if (!match) {
    errors.push(`Invalid Flyway migration filename: ${fileName}`);
    continue;
  }
  if (migrationVersions.has(match[1])) {
    errors.push(`Duplicate Flyway migration version: V${match[1]}`);
  }
  migrationVersions.add(match[1]);
}

// Ensure .env.example documents every environment variable the server reads.
// Catches the recurring drift where application.yml references ${NEW_VAR} but
// the example template is not updated, leaving operators unaware of the
// required setting.
const applicationYaml = resolve(repositoryRoot, "server/src/main/resources/application.yml");
const envExample = resolve(repositoryRoot, ".env.example");
if (existsSync(applicationYaml) && existsSync(envExample)) {
  const referencedVars = new Set(
    Array.from(
      readFileSync(applicationYaml, "utf8").matchAll(/\$\{([A-Z][A-Z0-9_]+)/g),
      (match) => match[1]
    )
  );
  const documented = new Set(
    Array.from(
      readFileSync(envExample, "utf8").matchAll(/^([A-Z][A-Z0-9_]+)=/gm),
      (match) => match[1]
    )
  );
  for (const variable of referencedVars) {
    if (!documented.has(variable)) {
      errors.push(
        `application.yml references \${${variable}} but .env.example does not document it. ` +
          `Add '${variable}=' to .env.example.`
      );
    }
  }
}

if (errors.length > 0) {
  console.error(errors.join("\n"));
  process.exit(1);
}

console.log(
  `Docs and migrations check passed (${markdownFiles.length} Markdown files, ${migrationVersions.size} migrations, .env.example synced).`
);

function walk(directory) {
  return readdirSync(directory).flatMap((entry) => {
    const path = resolve(directory, entry);
    return statSync(path).isDirectory()
      ? ignoredDirectories.has(entry) ? [] : walk(path)
      : [path];
  });
}

function markdownTargets(content) {
  return Array.from(content.matchAll(/\[[^\]]*]\(([^)\s]+)(?:\s+[^)]*)?\)/g), (match) => match[1]);
}

function localTargetPath(target) {
  const path = target.replace(/^<|>$/g, "").split("#", 1)[0].split("?", 1)[0];
  return path && !/^(?:[a-z][a-z+.-]*:|#)/i.test(path) ? decodeURIComponent(path) : null;
}

function isInsideRepository(path) {
  return path === repositoryRoot || path.startsWith(repositoryRoot + sep);
}
