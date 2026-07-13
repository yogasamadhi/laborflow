import type { PropsWithChildren, ReactNode } from "react";

export function Page({
  title,
  description,
  actions,
  children
}: PropsWithChildren<{ title: string; description?: string; actions?: ReactNode }>) {
  return (
    <div className="page">
      <div className="page-header">
        <div className="page-heading">
          <h2 className="page-title">{title}</h2>
          {description ? <p className="page-description">{description}</p> : null}
        </div>
        {actions ? <div className="page-actions">{actions}</div> : null}
      </div>
      {children}
    </div>
  );
}
