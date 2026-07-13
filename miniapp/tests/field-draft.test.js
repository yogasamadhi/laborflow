const assert = require("node:assert/strict");
const path = require("node:path");
const test = require("node:test");

const fieldDraftPath = path.resolve(__dirname, "../utils/field-draft.js");

test("field drafts persist complete snapshots and ignore malformed entries", () => {
  const storage = new Map();
  global.wx = {
    getStorageSync(key) {
      return storage.get(key);
    },
    setStorageSync(key, value) {
      storage.set(key, value);
    },
    removeStorageSync(key) {
      storage.delete(key);
    }
  };
  delete require.cache[fieldDraftPath];
  const fieldDraft = require(fieldDraftPath);
  const snapshot = {
    editId: 18,
    form: { projectId: 6, reason: "临时支护" },
    uploadedFiles: [{ id: 44 }],
    attachedFileIds: [44]
  };

  fieldDraft.save("variation-order", snapshot);
  assert.deepEqual(fieldDraft.load("variation-order"), snapshot);

  storage.set("laborflow_field_draft_daily-report", { version: 0, data: snapshot });
  assert.equal(fieldDraft.load("daily-report"), null);

  fieldDraft.clear("variation-order");
  assert.equal(fieldDraft.load("variation-order"), null);

  fieldDraft.save("daily-report", { form: { projectId: 3 } }, "user-1");
  fieldDraft.save("daily-report", { form: { projectId: 4 } }, "user-2");
  assert.deepEqual(fieldDraft.load("daily-report", "user-1"), { form: { projectId: 3 } });
  assert.deepEqual(fieldDraft.load("daily-report", "user-2"), { form: { projectId: 4 } });
});
