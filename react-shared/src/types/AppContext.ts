export interface Action<T> {
  type: T
};

export type Dispatch<T, A extends Action<T>> = (action: A) => void;
