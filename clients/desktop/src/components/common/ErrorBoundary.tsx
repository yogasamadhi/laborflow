import { Component, type ErrorInfo, type PropsWithChildren, type ReactNode } from "react";

/**
 * Catches render errors in the subtree and shows a fallback instead of
 * crashing the whole app to a blank screen.
 *
 * Previously the app had no error boundary at all, so any throw inside a lazy
 * route (e.g. a malformed API response hitting an unguarded `.asText()`) would
 * blank the entire SPA. This boundary wraps the router so a single broken page
 * degrades gracefully.
 *
 * On error it logs to the console (so devtools still show it) and renders a
 * small retry card. State is intentionally minimal — no analytics wiring yet.
 */
type ErrorBoundaryState = { error: Error | null };

export class ErrorBoundary extends Component<PropsWithChildren<{ fallback?: (error: Error, reset: () => void) => ReactNode }>, ErrorBoundaryState> {
  state: ErrorBoundaryState = { error: null };

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    // Keep the trace visible in devtools without coupling to a logging service.
    console.error("[ErrorBoundary] render error:", error, info.componentStack);
  }

  reset = () => this.setState({ error: null });

  render() {
    const { error } = this.state;
    if (!error) return this.props.children;
    if (this.props.fallback) return this.props.fallback(error, this.reset);
    return (
      <div className="panel" style={{ margin: 24, textAlign: "center" }}>
        <h3>页面渲染出错</h3>
        <p className="muted">该页面遇到了问题，可以尝试刷新或返回上一页。</p>
        <pre className="muted" style={{ whiteSpace: "pre-wrap", fontSize: 12, marginTop: 8 }}>
          {error.message}
        </pre>
        <div style={{ marginTop: 12, display: "flex", gap: 8, justifyContent: "center" }}>
          <button className="btn" type="button" onClick={this.reset}>重试</button>
          <button className="btn btn-secondary" type="button" onClick={() => window.location.reload()}>刷新页面</button>
        </div>
      </div>
    );
  }
}
