import type { PropsWithChildren, ReactNode } from "react";

export function Page({ title, actions, children }: PropsWithChildren<{ title: string; actions?: ReactNode }>) {
  return (
    <div className="page">
      <div className="page-header">
        <h2 className="page-title">{title}</h2>
        <div>{actions}</div>
      </div>
      {children}
    </div>
  );
}

