import { Role, User } from 'client/gui';
import { useAppContext } from '../AppContext';

interface CurrentUserRoles {
  isReadOnly: boolean;
  currentUser: User | null | undefined;
  hasOneOfRoles: (roles: Array<Role> | null | undefined) => boolean;
}

const useCurrentUserRoles = (): CurrentUserRoles => {
  
  const { state } = useAppContext();
  
  if (!state
      || !state.currentUser
      || !state.currentUser.roles) {
    return {
        isReadOnly: false,
        currentUser: state?.currentUser,
        hasOneOfRoles: _roles => false
      };
  }
  
  return {
    isReadOnly: !state.currentUser.roles || (state.currentUser.roles.length === 0),
    currentUser: state.currentUser,
    hasOneOfRoles: (roles: Array<Role> | null | undefined): boolean => {
          return (roles === null) || (roles === undefined)
              ? true
              : roles.length === 0
              // @ts-ignore
              ? state.currentUser.roles.length > 0
              // @ts-ignore
              : roles.reduce((result: boolean, role: Role) => result || state.currentUser.roles.includes(role), false);
      }
  };
  
}

export { useCurrentUserRoles };
