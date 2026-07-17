import { describe, expect, it } from "vitest";
import { nameOf } from "./nameOf";

describe("nameOf", () => {
  const rows = [
    { id: "1", name: "Alpha" },
    { id: "2", name: "  Beta  " },
    { id: 3, name: "Gamma" }
  ];

  it("resolves a string id to its name", () => {
    expect(nameOf(rows, "1")).toBe("Alpha");
  });

  it("matches numeric ids against string ids", () => {
    expect(nameOf(rows, 3)).toBe("Gamma");
  });

  it("returns '-' when the id is missing or not found", () => {
    expect(nameOf(rows, undefined)).toBe("-");
    expect(nameOf(rows, "")).toBe("-");
    expect(nameOf(rows, "999")).toBe("-");
  });

  it("returns '-' for blank-only names instead of whitespace", () => {
    expect(nameOf(rows, "2")).toBe("Beta");
    expect(nameOf([{ id: "x", name: "   " }], "x")).toBe("-");
    expect(nameOf([{ id: "x", name: "" }], "x")).toBe("-");
  });

  it("handles undefined rows gracefully", () => {
    expect(nameOf(undefined, "1")).toBe("-");
  });
});
