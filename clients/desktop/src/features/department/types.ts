export type Department = {
  id: string;
  name: string;
  parentId?: string | null;
  sortOrder?: number;
  status: string;
  createdAt?: string;
};
