import React, { PropsWithChildren } from 'react';
import { useAppContext } from '../AppContext';
import { User } from '../client/gui';

interface Props {};

let promise: Promise<User | null> | undefined = undefined;
let inprogress = false;

const CurrentUser = ({ children }: PropsWithChildren<Props>) => {
  
  const { fetchCurrentUser } = useAppContext();
  
  if (promise === undefined) {
    inprogress = true;
    promise = new Promise<User | null>((resolveUser, rejectUser) => {
      new Promise<User | null>((resolveLoading, rejectLoading) => {
          fetchCurrentUser(resolveLoading, rejectLoading);
        })
        .then((value: User | null) => {
          inprogress = false;
          resolveUser(value);
        }).catch((error: any) => {
          inprogress = false;
          rejectUser(error);
        });
    });
  }
  // return a promise to trigger <Suspend> element
  if (inprogress) {
    throw promise;
  }
  return (<>{children}</>);
};

export { CurrentUser };
