import { useMutation, type UseMutationOptions, type UseMutationResult } from "@tanstack/react-query";
import { ApiError, REQUEST_ERROR_EVENT, type RequestErrorDetail } from "./axios";

/**
 * `useMutation` wrapper that ensures the global request-error toast fires on
 * failure, without forcing every page to repeat the same onError boilerplate.
 *
 * Why this exists: axios's response interceptor already emits
 * `laborflow:request-error` for HTTP failures, but mutation rejections frequently
 * come from `unwrapResult` (business `code !== 0`) which throws an `ApiError`
 * without triggering the interceptor. Pages that omit `onError` silently swallow
 * those failures — the user sees no feedback.
 *
 * Caller-supplied `onError` still runs and receives the raw error.
 */
type ToastableOptions<TData, TError, TVariables, TContext> = UseMutationOptions<TData, TError, TVariables, TContext>;

function emit(detail: RequestErrorDetail) {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new CustomEvent<RequestErrorDetail>(REQUEST_ERROR_EVENT, { detail }));
  }
}

/**
 * Maps a thrown value into the shape consumed by the request-error toast.
 * Exported for unit testing (the surrounding hook touches `window`).
 */
export function describeError(error: unknown): RequestErrorDetail {
  if (error instanceof ApiError) {
    return {
      status: error.status,
      message: error.message,
      ...(error.network ? { network: true } : {})
    };
  }
  if (error instanceof Error) {
    return { message: error.message };
  }
  return { message: "操作失败，请稍后重试" };
}

export function useToastMutation<TData, TError = Error, TVariables = void, TContext = unknown>(
  options: ToastableOptions<TData, TError, TVariables, TContext>
): UseMutationResult<TData, TError, TVariables, TContext> {
  const { onError: callerOnError, ...rest } = options;
  return useMutation<TData, TError, TVariables, TContext>({
    ...rest,
    onError: (error, ...args) => {
      // Always surface the failure via the global toast channel.
      emit(describeError(error));
      // Then hand control back to the page if it provided its own handler.
      (callerOnError as ((e: TError, ...a: unknown[]) => unknown) | undefined)?.(error, ...args);
    }
  });
}
