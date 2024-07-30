import { User } from 'client/gui';
import { useAppContext } from '../AppContext';

interface CurrentUserGroups {
  isReadOnly: boolean;
  currentUser: User | null | undefined;
  hasOneOfGroups: (groups: Array<string>) => boolean;
}

const useCurrentUserGroups = (): CurrentUserGroups => {
  
  const { state } = useAppContext();
  
  if (!state
      || !state.currentUser
      || !state.currentUser.groups) {
    return {
        isReadOnly: false,
        currentUser: state?.currentUser,
        hasOneOfGroups: _groups => false
      };
  }
  
  return {
    isReadOnly: !state.currentUser.groups || (state.currentUser.groups.length === 0),
    currentUser: state.currentUser,
    hasOneOfGroups: groups => !Boolean(groups)
        || groups.reduce((result, group) => result
            || state.currentUser?.groups?.find(g => g.id === group) !== undefined, false)
  };
  
}

export { useCurrentUserGroups };
