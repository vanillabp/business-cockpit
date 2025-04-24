import { Box, Menu, Select, Text} from 'grommet';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router-dom';
import React, { useState, useEffect } from 'react';
import { useResponsiveScreen } from '@vanillabp/bc-shared';
import { appNs } from '../app/DevShellApp.js';
import i18n from '../i18n.js';
import { ButtonExtendedProps } from 'grommet/components/Button';
import {UserTasksRequest, UserTaskRetrieveMode, UserTask} from '@vanillabp/bc-official-gui-client';
import { TaskToggle } from '../components/ToggleComponent.js';

i18n.addResources('en', 'usertask-header', {
    'views-label': 'View',
    'view-form': 'form',
    'view-icon': 'icon',
    'view-list': 'list',
});
i18n.addResources('de', 'usertask-header', {
    'views-label': 'Ansicht',
    'view-form': 'Formular',
    'view-icon': 'Symbol',
    'view-list': 'Liste',
});

const Header = () => {
    const { isPhone } = useResponsiveScreen();
    const navigate = useNavigate();
    const { t: tApp } = useTranslation(appNs);
    const { t } = useTranslation('usertask-header');

    const userTaskIdParam: string | undefined = useParams()['userTaskId'];
    const [taskId, setTaskId] = useState(userTaskIdParam);
    const [userTasks, setUserTasks] = useState<UserTask[]>([]);
    const [searchText, setSearchText] = useState('');
    const [taskFilter, setTaskFilter] = useState<'all' | 'open' | 'closed'>('all');
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);

    const selectOptions = userTasks.map(task => ({
        label: `${task.businessId || ''} | ${task.taskDefinition}/${task.bpmnProcessId} | (${task.id})`,
        value: task.id
    }));

    const filteredOptions = selectOptions.filter(option => {
        const escapedText = searchText.replace(/[-\\^$*+?.()|[\]{}]/g, '\\$&');
        const regex = new RegExp(escapedText, 'i');
        return regex.test(option.label);
    });

    const fetchTasks = async () => {
        let mode;
        if (taskFilter === 'open') {
            mode = UserTaskRetrieveMode.OpenTasks;
        } else if (taskFilter === 'closed') {
            mode = UserTaskRetrieveMode.ClosedTasksOnly;
        } else {
            mode = UserTaskRetrieveMode.All;
        }

        try {
            const response = await fetch('/official-api/v1/usertask', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    pageNumber: page,
                    pageSize: 20,
                    sort: 'createdAt',
                    sortAscending: false,
                    mode: mode,
                } as UserTasksRequest),
            });

            if (!response.ok) {
                throw new Error('Failed to fetch tasks');
            }

            const data = await response.json();

            setUserTasks(prev => [...prev, ...data.userTasks]);

            const totalPages = data.page?.totalPages ?? 0;
            if (page + 1 >= totalPages) {
                setHasMore(false);
            }
        } catch (error) {
            console.error('Error fetching tasks:', error);
            setUserTasks([]);
        }
    };

    useEffect(() => {
        // Reset everything when filter changes
        setUserTasks([]);
        setPage(0);
        setHasMore(true);
    }, [taskFilter]);

    useEffect(() => {
        fetchTasks();
    }, [page, taskFilter]);

    const onMore = () => {
        if (hasMore) {
            setPage(prev => prev + 1);
        }
    };

    const loadUserTask = (selectedTaskId?: string) =>
        navigate(`/${tApp('url-usertask')}/${selectedTaskId ?? taskId}`, { replace: true });

    const viewMenuItems: ButtonExtendedProps[] = [
        { label: t('view-form'), onClick: () => navigate(`/${tApp('url-usertask')}/${taskId}`) },
        { label: t('view-list'), onClick: () => navigate(`/${tApp('url-usertask')}/${taskId}/${tApp('url-list')}`) },
        { label: t('view-icon'), onClick: () => navigate(`/${tApp('url-usertask')}/${taskId}/${tApp('url-icon')}`) },
    ];

    const renderOption = (option: { label: string, value: string }) => {
        const [businessId, rest] = option.label.split(' | ', 2);
        return (
            <Box direction="row" gap="xsmall">
                <Text weight="bold">{businessId}</Text>
                <Text weight="normal"> | {rest}</Text>
            </Box>
        );
    };

    return (
        <Box
            fill
            direction="row"
            justify="between"
            gap="small">
            <Box direction="row" gap="small" align="center" fill>
                <Box width={isPhone ? '15rem' : '26rem'} fill>
                    <Select
                        size="medium"
                        placeholder="Select user task"
                        labelKey="label"
                        valueKey={{ key: 'value', reduce: true }}
                        value={taskId}
                        valueLabel={
                            (() => {
                                const selected = selectOptions.find(o => o.value === taskId);
                                if (!selected) return undefined;
                                const [businessId, rest] = selected.label.split(' | ', 2);
                                return (
                                    <Box direction="row" gap="xsmall" pad={{ vertical: 'xsmall', horizontal: 'small' }}>
                                        <Text weight="bold">{businessId}</Text>
                                        <Text weight="normal"> | {rest}</Text>
                                    </Box>
                                );
                            })()
                        }
                        options={filteredOptions}
                        onChange={({ value }) => {
                            setTaskId(value);
                            loadUserTask(value);
                        }}
                        onMore={hasMore ? onMore : undefined}
                        onSearch={text => setSearchText(text)}
                    >
                        {(option, state) => (
                            <Box pad="small" background={state.active ? 'active' : undefined}>
                                {renderOption(option)}
                            </Box>
                        )}
                    </Select>
                </Box>
                <TaskToggle value={taskFilter} onChange={setTaskFilter} />
            </Box>
            <Box>
                <Menu
                    disabled={!Boolean(taskId)}
                    label={t('views-label')}
                    items={viewMenuItems}
                />
            </Box>
        </Box>
    );
};

export { Header };
