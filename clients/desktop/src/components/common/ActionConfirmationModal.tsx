import type { PropsWithChildren, ReactNode } from "react";
import { CircleAlert, LoaderCircle } from "lucide-react";
import { Modal } from "./Modal";
import { Button } from "../ui/Button";

type ActionConfirmationModalProps = PropsWithChildren<{
  open: boolean;
  title: string;
  description: string;
  confirmLabel: string;
  onClose: () => void;
  onConfirm: () => void;
  pending?: boolean;
  tone?: "primary" | "danger";
  summary?: ReactNode;
}>;

export function ActionConfirmationModal({
  open,
  title,
  description,
  confirmLabel,
  onClose,
  onConfirm,
  pending = false,
  tone = "primary",
  summary,
  children
}: ActionConfirmationModalProps) {
  return (
    <Modal
      open={open}
      title={title}
      onClose={onClose}
      footer={
        <>
          <Button type="button" variant="secondary" onClick={onClose} disabled={pending}>取消</Button>
          <Button type="button" variant={tone} onClick={onConfirm} disabled={pending}>
            {pending ? <LoaderCircle className="spin" size={16} /> : null}
            {pending ? "处理中" : confirmLabel}
          </Button>
        </>
      }
    >
      <div className="action-confirmation">
        <CircleAlert className={`action-confirmation-icon ${tone === "danger" ? "is-danger" : ""}`} size={22} aria-hidden="true" />
        <div>
          <p className="action-confirmation-description">{description}</p>
          {summary ? <div className="action-confirmation-summary">{summary}</div> : null}
        </div>
      </div>
      {children ? <div className="action-confirmation-body">{children}</div> : null}
    </Modal>
  );
}
