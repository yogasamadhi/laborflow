import { describe, expect, it } from "vitest";
import { ApiError } from "./axios";
import { describeError } from "./useToastMutation";

describe("describeError", () => {
  it("keeps the status and message of an ApiError", () => {
    const error = new ApiError("余额不足", 400);
    expect(describeError(error)).toEqual({ status: 400, message: "余额不足" });
  });

  it("keeps the network marker of a transport ApiError", () => {
    const error = new ApiError("无法连接服务端", undefined, true);
    expect(describeError(error)).toEqual({ status: undefined, message: "无法连接服务端", network: true });
  });

  it("uses the message of a generic Error without a status", () => {
    expect(describeError(new Error("网络中断"))).toEqual({ message: "网络中断", status: undefined });
  });

  it("falls back to a generic message for non-Error throws", () => {
    expect(describeError(undefined)).toEqual({ message: "操作失败，请稍后重试" });
    expect(describeError("string thrown")).toEqual({ message: "操作失败，请稍后重试" });
    expect(describeError({ random: true })).toEqual({ message: "操作失败，请稍后重试" });
  });
});
