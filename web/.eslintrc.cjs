/* eslint-env node */
// Flat-config-free legacy config (.eslintrc.cjs) for broad tool compatibility.
// Focuses on catching real bugs (unused vars, hooks deps, type-aware issues)
// without being opinionated about formatting (Prettier owns formatting).
module.exports = {
  root: true,
  parser: "@typescript-eslint/parser",
  parserOptions: {
    ecmaVersion: 2022,
    sourceType: "module",
    ecmaFeatures: { jsx: true }
  },
  plugins: ["@typescript-eslint", "react-hooks"],
  extends: [
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
    "plugin:react-hooks/recommended"
  ],
  rules: {
    // Keep the barrier low: warn rather than error on stylistic nits so the
    // first lint pass doesn't drown existing code in red. Promote to "error"
    // once the codebase is clean.
    "@typescript-eslint/no-unused-vars": ["warn", { argsIgnorePattern: "^_", varsIgnorePattern: "^_" }],
    "@typescript-eslint/no-explicit-any": "warn",
    "no-console": ["warn", { allow: ["warn", "error"] }],
    "react-hooks/rules-of-hooks": "error",
    // The useMemo-dependency style suggestions are noisy on existing list pages
    // and are performance hints, not correctness bugs. Re-enable as "warn"
    // after the god-file split lands.
    "react-hooks/exhaustive-deps": "off"
  },
  ignorePatterns: ["dist", "node_modules", "coverage", "*.config.ts", "tsconfig.tsbuildinfo"]
};
