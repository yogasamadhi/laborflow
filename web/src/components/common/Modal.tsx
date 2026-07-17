import { X } from "lucide-react";
import { useEffect, useRef, type PropsWithChildren, type ReactNode } from "react";
import { Button } from "../ui/Button";

type ModalProps = PropsWithChildren<{
  open: boolean;
  title: string;
  footer?: ReactNode;
  onClose: () => void;
}>;

/**
 * Modal dialog with Esc-to-close and a basic focus trap.
 *
 * Accessibility additions over the previous version:
 * - Esc closes the dialog.
 * - On open, focus moves into the dialog (the close button) and Tab/Shift-Tab
 *   cycle within the dialog instead of escaping to the page behind it.
 * - On close, focus returns to the previously-focused element.
 *
 * The component still renders nothing when closed (returns null before hooks
 * would be illegal); the a11y behaviour runs only while `open` is true.
 */
export function Modal({ open, title, footer, onClose, children }: ModalProps) {
  const dialogRef = useRef<HTMLDivElement>(null);
  const previouslyFocused = useRef<HTMLElement | null>(null);

  useEffect(() => {
    if (!open) return;
    previouslyFocused.current = document.activeElement as HTMLElement | null;

    // Move focus into the dialog.
    const dialog = dialogRef.current;
    const initialFocus = dialog?.querySelector<HTMLElement>(
      "[autofocus], button, [href], input, select, textarea, [tabindex]:not([tabindex='-1'])"
    );
    initialFocus?.focus();

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.stopPropagation();
        onClose();
        return;
      }
      if (event.key !== "Tab" || !dialog) return;

      // Trap Tab within the dialog's focusable elements.
      const focusable = Array.from(
        dialog.querySelectorAll<HTMLElement>(
          "button, [href], input, select, textarea, [tabindex]:not([tabindex='-1'])"
        )
      ).filter((el) => !el.hasAttribute("disabled") && el.offsetParent !== null);
      if (focusable.length === 0) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      const active = document.activeElement as HTMLElement | null;
      if (event.shiftKey && active === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && active === last) {
        event.preventDefault();
        first.focus();
      }
    };

    document.addEventListener("keydown", onKeyDown);
    // Prevent the background from scrolling while the modal is open.
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = previousOverflow;
      previouslyFocused.current?.focus?.();
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-label={title}
        ref={dialogRef}
        onMouseDown={(event) => event.stopPropagation()}
      >
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
