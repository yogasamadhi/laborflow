import { useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { getData, PageResult } from "../../lib/axios";
import { TextInput, SelectInput } from "./Field";

export type LookupOption = {
  id: string;
  name?: string;
  nickname?: string;
  username?: string;
  code?: string;
  status?: string;
};

type Props = {
  endpoint: string;
  value?: string;
  onChange: (value: string, option?: LookupOption) => void;
  placeholder?: string;
  params?: Record<string, unknown>;
  allowEmpty?: boolean;
  emptyLabel?: string;
  disabled?: boolean;
  labelKeys?: Array<"name" | "nickname" | "username" | "code">;
};

export function AsyncLookupSelect({
  endpoint,
  value = "",
  onChange,
  placeholder = "输入名称查询",
  params,
  allowEmpty = true,
  emptyLabel = "请选择",
  disabled,
  labelKeys = ["name", "nickname", "username", "code"]
}: Props) {
  const [keyword, setKeyword] = useState("");
  const [debounced, setDebounced] = useState("");
  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(keyword.trim()), 250);
    return () => window.clearTimeout(timer);
  }, [keyword]);
  const query = useQuery({
    queryKey: ["async-lookup", endpoint, debounced, params],
    queryFn: () => getData<PageResult<LookupOption>>(endpoint, {
      page: 1,
      pageSize: 20,
      keyword: debounced || undefined,
      ...params
    }),
    enabled: !disabled
  });
  const options = query.data?.records ?? [];
  const labelOf = (option: LookupOption) => {
    for (const key of labelKeys) {
      const value = option[key];
      if (value) return value;
    }
    return "名称缺失";
  };
  return (
    <div className="lookup-select">
      <TextInput
        value={keyword}
        onChange={event => setKeyword(event.target.value)}
        placeholder={placeholder}
        disabled={disabled}
        aria-label={placeholder}
      />
      <SelectInput
        value={value}
        disabled={disabled || query.isPending}
        onChange={event => onChange(event.target.value, options.find(item => item.id === event.target.value))}
      >
        {allowEmpty ? <option value="">{emptyLabel}</option> : null}
        {options.map(option => <option key={option.id} value={option.id}>{labelOf(option)}</option>)}
      </SelectInput>
    </div>
  );
}
