import { ArrowLeft, CircleAlert, LoaderCircle, MapPinOff, RefreshCw, ShieldAlert } from "lucide-react";
import { Button } from "../ui/Button";

export function AuthLoadingState() {
  return (
    <div className="access-page" role="status" aria-live="polite">
      <LoaderCircle className="access-state-icon spin" size={28} />
      <h1>正在验证登录状态</h1>
      <p>请稍候，系统正在确认当前账号的访问权限。</p>
    </div>
  );
}

export function RouteLoadingState() {
  return (
    <section className="route-loading" role="status" aria-live="polite">
      <div className="route-loading-heading">
        <span className="skeleton skeleton-title" />
        <span className="skeleton skeleton-copy" />
      </div>
      <div className="route-loading-grid" aria-hidden="true">
        <span className="skeleton skeleton-card" />
        <span className="skeleton skeleton-card" />
        <span className="skeleton skeleton-card" />
      </div>
      <span className="sr-only">正在加载页面</span>
    </section>
  );
}

export function DataErrorState({ message, onRetry }: { message?: string; onRetry?: () => void }) {
  return (
    <section className="state-card state-card-error" role="alert">
      <CircleAlert className="access-state-icon" size={24} />
      <h3>暂时无法加载数据</h3>
      <p>{message || "请检查网络后重试。"}</p>
      {onRetry ? (
        <Button type="button" variant="secondary" onClick={onRetry}>
          <RefreshCw size={16} />
          重试
        </Button>
      ) : null}
    </section>
  );
}

export function AccessDeniedState({ onBack }: { onBack: () => void }) {
  return (
    <section className="access-state" role="alert" aria-labelledby="access-denied-title">
      <ShieldAlert className="access-state-icon" size={28} />
      <h2 id="access-denied-title">当前账号无权访问此页面</h2>
      <p>已隐藏无权限入口。请返回可访问的工作台，或联系管理员确认角色与数据范围。</p>
      <Button type="button" variant="secondary" onClick={onBack}>
        <ArrowLeft size={16} />
        返回仪表盘
      </Button>
    </section>
  );
}

export function NotFoundPage() {
  return (
    <section className="access-state" aria-labelledby="not-found-title">
      <MapPinOff className="access-state-icon" size={28} />
      <h2 id="not-found-title">页面不存在</h2>
      <p>该地址不存在或已被移动，请从左侧菜单进入可用功能。</p>
    </section>
  );
}
