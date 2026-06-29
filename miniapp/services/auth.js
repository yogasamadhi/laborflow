const request = require("../utils/request");

function login(data) {
  return request.post("/auth/login", data);
}

function me() {
  return request.get("/auth/me");
}

module.exports = {
  login,
  me
};
