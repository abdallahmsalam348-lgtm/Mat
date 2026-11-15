/** Error with an HTTP status and a stable machine-readable code (the Android app maps these codes). */
export class ApiError extends Error {
  constructor(status, code, message, extra = {}) {
    super(message || code);
    this.status = status;
    this.code = code;
    this.extra = extra;
  }
}
