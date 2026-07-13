import { describe, expect, it } from "vitest";
import { ApiError, toApiError } from "./axios";

describe("API error normalization", () => {
  it("preserves standard error messages", () => {
    const error = new Error("业务校验失败");
    expect(toApiError(error)).toMatchObject({ message: "业务校验失败", status: undefined });
  });

  it("creates a readable error for unknown failures", () => {
    const error = toApiError({});
    expect(error).toBeInstanceOf(ApiError);
    expect(error.message).toBe("请求失败");
  });
});
