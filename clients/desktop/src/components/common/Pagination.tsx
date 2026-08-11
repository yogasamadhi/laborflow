import { Button } from "../ui/Button";

type Props = {
  page: number;
  pageSize: number;
  total: number;
  onPageChange: (page: number) => void;
  onPageSizeChange?: (pageSize: number) => void;
};

export function Pagination({ page, pageSize, total, onPageChange, onPageSizeChange }: Props) {
  const pageCount = Math.max(1, Math.ceil(total / pageSize));
  return (
    <div className="panel toolbar" aria-label="分页">
      <span className="muted">共 {total} 条，第 {page}/{pageCount} 页</span>
      {onPageSizeChange ? (
        <select
          className="select"
          value={pageSize}
          aria-label="每页条数"
          onChange={event => onPageSizeChange(Number(event.target.value))}
        >
          {[20, 50, 100].map(size => <option key={size} value={size}>{size} 条/页</option>)}
        </select>
      ) : null}
      <Button variant="secondary" disabled={page <= 1} onClick={() => onPageChange(page - 1)}>上一页</Button>
      <Button variant="secondary" disabled={page >= pageCount} onClick={() => onPageChange(page + 1)}>下一页</Button>
    </div>
  );
}
