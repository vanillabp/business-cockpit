export interface Person {
  id: string;
  display?: string;
  displayShort?: string;
  email?: string;
  avatar?: number;
  details?: { [key: string]: unknown };
}
