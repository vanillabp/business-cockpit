import React, { useEffect, useState } from 'react';
import { Anchor, Box, Text, Select } from 'grommet';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';

interface User {
    id: string;
    email?: string;
    firstName?: string;
    lastName?: string;
    groups?: string[];
    attributes?: Record<string, string[]> | null;
}

const Main = ({
                  additionalComponents
              }: {
    additionalComponents: Array<string>
}) => {

    const { t } = useTranslation('app');
    const navigate = useNavigate();
    const [users, setUsers] = useState<User[]>([]);
    const [currentUser, setCurrentUser] = useState<string>('---');
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
                setCurrentUser('---');
            });
    }, []);

    const changeUser = (selectedUserId: string) => {
        fetch(`${baseUrl}/user/${selectedUserId}`, {
            method: 'POST',
            credentials: 'include',
        }).then(() => window.location.reload());
    };

    const selectOptions = [
        { label: '---', value: '---' },
        ...users.map(user => ({
            label: `${user.id}, ${user.firstName} ${user.lastName}`,
            value: user.id
        }))
    ];

    return (
        <Box margin="large" gap="medium">
            <Text weight='bold'>{t('title.long')}</Text>

            <Box direction="row" gap="small" align="center">
                <Text>User:</Text>
                <Select
                    size="medium"
                    placeholder="Select user"
                    options={selectOptions}
                    labelKey="label"
                    valueKey={{ key: 'value', reduce: true }}
                    value={currentUser}
                    onChange={({ value }) => {
                        setCurrentUser(value);
                        changeUser(value);
                    }}
                />
            </Box>

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

        </Box>
    );
};

export { Main };
