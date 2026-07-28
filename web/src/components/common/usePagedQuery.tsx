import { useQuery } from "@tanstack/react-query";
import { ReactNode, useEffect, useMemo, useState } from "react";
import { getData, PageResult } from "../../lib/axios";
import { Pagination } from "./Pagination";

export function usePagedQuery<T>(
  queryKey: readonly unknown[],
  endpoint: string,
  params: Record<string, unknown> = {}
) {
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const paramsKey = JSON.stringify(params);
  useEffect(() => setPage(1), [paramsKey]);
  const query = useQuery({
    queryKey: [...queryKey, page, pageSize, paramsKey],
    queryFn: () => getData<PageResult<T>>(endpoint, { ...params, page, pageSize })
  });
  const total = Number(query.data?.total ?? 0);
  const pagination: ReactNode = useMemo(() => (
    <Pagination
      page={page}
      pageSize={pageSize}
      total={total}
      onPageChange={setPage}
      onPageSizeChange={size => {
        setPageSize(size);
        setPage(1);
      }}
    />
  ), [page, pageSize, total]);
  return { ...query, pagination, page, pageSize, setPage };
}
