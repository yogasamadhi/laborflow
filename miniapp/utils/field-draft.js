const DRAFT_VERSION = 1;
const PREFIX = "laborflow_field_draft_";

function storageKey(name, scope) {
  return PREFIX + name + (scope ? "_" + scope : "");
}

function load(name, scope) {
  const entry = wx.getStorageSync(storageKey(name, scope));
  if (!entry || entry.version !== DRAFT_VERSION || !entry.data) return null;
  return entry.data;
}

function save(name, data, scope) {
  wx.setStorageSync(storageKey(name, scope), {
    version: DRAFT_VERSION,
    updatedAt: Date.now(),
    data
  });
}

function clear(name, scope) {
  wx.removeStorageSync(storageKey(name, scope));
}

module.exports = {
  load,
  save,
  clear
};
