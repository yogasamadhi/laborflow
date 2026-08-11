const test = require("node:test");
const assert = require("node:assert/strict");

// wx.* and console.* are real in node:test, so we can capture console output.
const logger = require("../utils/logger");

test("warn writes a scoped message to console.warn", () => {
  const original = console.warn;
  const captured = [];
  console.warn = (...args) => captured.push(args.join(" "));
  try {
    logger.warn("salary", "boom");
  } finally {
    console.warn = original;
  }
  assert.equal(captured.length, 1);
  assert.match(captured[0], /\[laborflow:salary\] boom/);
});

test("warn serializes Error objects to their message", () => {
  const original = console.warn;
  const captured = [];
  console.warn = (...args) => captured.push(args.join(" "));
  try {
    logger.warn("scope", new Error("network down"));
  } finally {
    console.warn = original;
  }
  assert.match(captured[0], /network down/);
});

test("error writes to console.error with a stack when available", () => {
  const original = console.error;
  const captured = [];
  console.error = (...args) => captured.push(args.join(" "));
  try {
    logger.error("report", new Error("upload failed"));
  } finally {
    console.error = original;
  }
  assert.equal(captured.length, 1);
  assert.match(captured[0], /\[laborflow:report\]/);
});

test("warn coerces unknown shapes to a JSON string without throwing", () => {
  const original = console.warn;
  const captured = [];
  console.warn = (...args) => captured.push(args.join(" "));
  const circular = {};
  circular.self = circular;
  try {
    logger.warn("scope", circular);
  } finally {
    console.warn = original;
  }
  // circular JSON.stringify fails -> falls back to String(value), no throw.
  assert.equal(captured.length, 1);
});
