import type { PropsWithChildren, ReactNode } from "react";
import { X } from "lucide-react";
import { Button } from "../ui/Button";

type ModalProps = PropsWithChildren<{
  open: boolean;
  title: string;
  footer?: ReactNode;
  onClose: () => void;
}>;

export function Modal({ open, title, footer, onClose, children }: ModalProps) {
  if (!open) return null;

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={title} onMouseDown={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <h3>{title}</h3>
          <Button type="button" variant="secondary" className="icon-btn" onClick={onClose} aria-label="关闭">
            <X size={16} />
          </Button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-footer">{footer}</div>}
      </div>
    </div>
  );
}

