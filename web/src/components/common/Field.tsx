import { cloneElement, isValidElement, useId, type InputHTMLAttributes, type ReactElement, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes } from "react";

type FieldProps = {
  label: string;
  /**
   * The control(s) the label describes. When given a single form element
   * (input/select/textarea) without an explicit id, Field injects a generated
   * id and pairs it with <label htmlFor>. Multiple/complex children are left
   * untouched so existing call sites (e.g. Field wrapping a fragment) keep
   * working; in that case the label is associated by DOM contiguity as before.
   */
  children: ReactNode;
};

export function Field({ label, children }: FieldProps) {
  const generatedId = useId();
  // If the single child is a form control without an explicit id, inject ours.
  const onlyChild = Array.isArray(children) ? null : children;
  const injectable =
    isValidElement(onlyChild) &&
    typeof onlyChild.props === "object" &&
    onlyChild.props !== null &&
    !("id" in onlyChild.props && (onlyChild.props as { id?: unknown }).id);

  const id = injectable ? generatedId : undefined;
  const wrapped = injectable
    ? cloneElement(onlyChild as ReactElement<{ id?: string }>, { id: generatedId })
    : children;

  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {wrapped}
    </div>
  );
}

export function TextInput(props: InputHTMLAttributes<HTMLInputElement>) {
  return <input className="input" {...props} />;
}

export function TextArea(props: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className="textarea" {...props} />;
}

export function SelectInput(props: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select className="select" {...props} />;
}
