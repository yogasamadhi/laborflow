import axios from "axios";
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

  it("marks transport failures as network errors", () => {
    const error = toApiError(new axios.AxiosError("net::ERR_CONNECTION_REFUSED", axios.AxiosError.ERR_NETWORK));
    expect(error).toMatchObject({
      message: "net::ERR_CONNECTION_REFUSED",
      status: undefined,
      network: true
    });
  });
});
