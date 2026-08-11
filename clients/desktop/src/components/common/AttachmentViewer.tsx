import { useEffect, useState } from "react";
import { Button } from "../ui/Button";
import { downloadFile, http } from "../../lib/axios";
import { Modal } from "./Modal";

export type FileMetadata = {
  id: string;
  originalName: string;
  contentType: string;
  fileSize: string | number;
  createdAt?: string;
  downloadUrl: string;
  previewUrl: string;
};

type Props = {
  file?: FileMetadata | null;
  onRemove?: () => void;
  removable?: boolean;
};

function fileSize(value?: string | number) {
  const bytes = Number(value ?? 0);
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export function AttachmentViewer({ file, onRemove, removable }: Props) {
  const [pending, setPending] = useState(false);
  const [previewUrl, setPreviewUrl] = useState("");
  useEffect(() => () => {
    if (previewUrl) URL.revokeObjectURL(previewUrl);
  }, [previewUrl]);
  if (!file) return <span className="muted">附件记录不可用</span>;
  const currentFile = file;
  const isImage = currentFile.contentType?.startsWith("image/");
  async function preview() {
    setPending(true);
    try {
      const blob = await http.get<unknown, Blob>(currentFile.previewUrl, { responseType: "blob" });
      const url = URL.createObjectURL(blob);
      setPreviewUrl(url);
    } finally {
      setPending(false);
    }
  }
  return (
    <>
      <div className="attachment-row">
        <div>
          <strong>{currentFile.originalName}</strong>
          <div className="muted">{currentFile.contentType || "未知类型"} · {fileSize(currentFile.fileSize)} · {currentFile.createdAt?.replace("T", " ").slice(0, 19) ?? "-"}</div>
        </div>
        <div className="table-actions">
          {isImage ? <Button variant="secondary" disabled={pending} onClick={() => void preview()}>预览</Button> : null}
          <Button variant="secondary" disabled={pending} onClick={() => void downloadFile(currentFile.downloadUrl, currentFile.originalName)}>下载</Button>
          {removable && onRemove ? <Button variant="danger" disabled={pending} onClick={onRemove}>移除</Button> : null}
        </div>
      </div>
      <Modal open={Boolean(previewUrl)} title={currentFile.originalName} onClose={() => setPreviewUrl("")}>
        {previewUrl ? <img className="attachment-preview-image" src={previewUrl} alt={currentFile.originalName} /> : null}
      </Modal>
    </>
  );
}
