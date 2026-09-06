import { Outlet } from 'react-router-dom';
import { Login } from './Login';
import { useCurrentUserGroups } from "../utils/roleUtils";

interface ProtectedRouteProps {
  groups?: Array<string>;
};

const ProtectedRoute = ({
  groups = undefined,
}: ProtectedRouteProps) => {

  const { hasOneOfGroups, currentUser } = useCurrentUserGroups();
  const hasOneOfRequestGroups = hasOneOfGroups(groups);
  if (currentUser && !hasOneOfRequestGroups) {
    console.error(`Try to fetch route protected by ${groups ? groups : 'any group'} but user has ${currentUser?.groups?.length === 0 ? 'none' : currentUser.groups.map(g => g.id)}!`);
  }

  return hasOneOfRequestGroups
      ? <Outlet />
      : <Login />;
};

export { ProtectedRoute };
