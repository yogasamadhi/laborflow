/* eslint-env node */
// Minimal lint for the WeChat miniapp (plain CommonJS JS, no framework).
// Focuses on real bug classes (undeclared vars, unused, no-debugger) without
// fighting the existing style.
module.exports = {
  root: true,
  env: {
    node: true,
    browser: true,
    es2022: true
  },
  parserOptions: {
    ecmaVersion: 2022,
    sourceType: "script"
  },
  // wx is a global injected by the WeChat runtime.
  globals: {
    wx: "readonly",
    Page: "readonly",
    App: "readonly",
    Component: "readonly",
    Behavior: "readonly",
    getApp: "readonly",
    getCurrentPages: "readonly"
  },
  extends: ["eslint:recommended"],
  rules: {
    "no-unused-vars": ["warn", { argsIgnorePattern: "^_" }],
    "no-undef": "error",
    "no-debugger": "error",
    "no-empty": ["warn", { allowEmptyCatch: true }],
    // Many pages intentionally use function hoisting (Page({}) at top, helpers
    // below); allow it.
    "no-use-before-define": "off",
    // catch handlers commonly ignore the error object shape.
    "no-unused-private-class-members": "off"
  },
  ignorePatterns: ["node_modules", "miniprogram_npm"]
};
