import React, {useEffect, useState} from 'react';
import { Anchor, Box, Text} from 'grommet';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';

interface User {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    groups: string[];
    attributes: Record<string, string[]> | null;
}

const Main = ({
  additionalComponents
}: {
  additionalComponents: Array<string>
}) => {
  
  const { t } = useTranslation('app');
  const navigate = useNavigate();
  const [users, setUsers] = useState<User[]>([]);
  const [currentUser, setCurrentUser] = useState<string | undefined>(undefined);
  const baseUrl = "http://localhost:9080";


    useEffect(() => {
        fetch(`${baseUrl}/user/all`)
            .then(response => response.json())
            .then((allUsers: User[]) => {
                setUsers(allUsers);
                return fetch(`${baseUrl}/user/`, {
                    headers: { 'Accept': 'text/plain' },
                    credentials: 'include',
                });
            })
            .then(response => response.text())
            .then((currentUser: string) => {
                setCurrentUser(currentUser);
            })
            .catch(() => {
                setCurrentUser(undefined);
            });
    }, []);

    const changeUser = (event: React.ChangeEvent<HTMLSelectElement>) => {
        const userId = event.target.value;
        fetch(`${baseUrl}/user/${userId}`, {
            method: 'POST',
            credentials: 'include',
        }).then(() => window.location.reload());
    };


    return (
        <Box margin="large" gap="medium">
            <Text weight='bold'>{t('title.long')}</Text>
            <Anchor color='accent-2' onClick={() => navigate(t('url-usertask') as string)}>
                {t('link-usertask')}
            </Anchor>
            <Anchor color='accent-2' onClick={() => navigate(t('url-workflow') as string)}>
                {t('link-workflow')}
            </Anchor>
            {additionalComponents.map(componentName => (
                <Anchor key={componentName} color='accent-3' onClick={() => navigate(`/${componentName}`)}>
                    {componentName}
                </Anchor>
            ))}
            <div>
                User:&nbsp;
                <select onChange={changeUser} value={currentUser || ''}>
                    <option value="---">---</option>
                    {users.map(user => (
                        <option key={user.id} value={user.id}>
                            {user.id + ", " + user.firstName + " " + user.lastName}
                        </option>
                    ))}
                </select>
            </div>
        </Box>
    );
};

export { Main };
