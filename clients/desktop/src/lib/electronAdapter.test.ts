import { describe, expect, it } from "vitest";
import { decodeDesktopResponse, serializeDesktopBody } from "./electronAdapter";

describe("Electron HTTP adapter", () => {
  it("serializes JSON request payloads after Axios transformation", async () => {
    await expect(serializeDesktopBody('{"name":"LaborFlow"}')).resolves.toEqual({
      type: "text",
      value: '{"name":"LaborFlow"}'
    });
  });

  it("serializes multipart text fields and files", async () => {
    const formData = new FormData();
    formData.append("purpose", "attachment");
    formData.append("file", new File(["content"], "evidence.txt", { type: "text/plain" }));
    const result = await serializeDesktopBody(formData);
    expect(result).toMatchObject({
      type: "form-data",
      entries: [
        { type: "text", name: "purpose", value: "attachment" },
        { type: "file", name: "file", filename: "evidence.txt", contentType: "text/plain" }
      ]
    });
  });

  it("decodes text and blob responses", async () => {
    const response = {
      status: 200,
      statusText: "OK",
      headers: { "content-type": "text/plain" },
      body: new TextEncoder().encode("完成")
    };
    expect(decodeDesktopResponse(response)).toBe("完成");
    const blob = decodeDesktopResponse(response, "blob") as Blob;
    expect(blob.type).toBe("text/plain");
    await expect(blob.text()).resolves.toBe("完成");
  });
});
