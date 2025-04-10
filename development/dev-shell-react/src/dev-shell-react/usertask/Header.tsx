import {Box, Button, Menu, Select} from 'grommet';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router-dom';
import React, { useState, useEffect } from 'react';
import { useResponsiveScreen } from '@vanillabp/bc-shared';
import { appNs } from '../app/DevShellApp.js';
import i18n from '../i18n.js';
import { ButtonExtendedProps } from "grommet/components/Button";
import { BcUserTask } from '@vanillabp/bc-shared';
import { UserTasksRequest } from '@vanillabp/bc-official-gui-client';
import { TaskToggle } from "../components/ToggleComponent.js";

i18n.addResources('en', 'usertask-header', {
    "views-label": "View",
    "view-form": "form",
    "view-icon": "icon",
    "view-list": "list",
});
i18n.addResources('de', 'usertask-header', {
    "views-label": "Ansicht",
    "view-form": "Formular",
    "view-icon": "Symbol",
    "view-list": "Liste",
});

const Header = () => {
    const { isPhone } = useResponsiveScreen();
    const navigate = useNavigate();
    const { t: tApp } = useTranslation(appNs);
    const { t } = useTranslation('usertask-header');

    const userTaskId: string | undefined = useParams()['userTaskId'];
    const [taskId, setTaskId] = useState(userTaskId);
    const [options, setOptions] = useState<string[]>([]);
    const [page, setPage] = useState(0);
    const [allTasks, setAllTasks] = useState<string[]>([]);
    const [taskFilter, setTaskFilter] = useState<'all' | 'open' | 'closed'>('all');


    useEffect(() => {
        const fetchTasks = async () => {
            try {
                const response = await fetch('/official-api/v1/usertask', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        pageSize: 20,
                        sort: 'updatedAt',
                        sortAscending: false
                    } as UserTasksRequest)
                });

                if (!response.ok) {
                    throw new Error('Failed to fetch tasks');
                }

                const data = await response.json();
                const formattedTasks = data.userTasks.map((task: BcUserTask) => 
                    `${task.businessId || ''} ${task.taskDefinition}/${task.bpmnProcessId} (${task.id})`
                );
                setAllTasks(formattedTasks);
                setOptions(formattedTasks.slice(0, 20));
            } catch (error) {
                console.error('Error fetching tasks:', error);
                setOptions([]);
                setAllTasks([]);
            }
        };

        fetchTasks();
    }, []);

    const onMore = () => {
        const nextPage = page + 1;
        const newOptions = allTasks.slice(0, (nextPage + 1) * 20);
        setOptions(newOptions);
        setPage(nextPage);
    };

    const loadUserTask = (selectedTaskId?: string) =>
        navigate(`/${tApp('url-usertask')}/${selectedTaskId ?? taskId}`, { replace: true });

    const viewMenuItems: ButtonExtendedProps[] = [
        { label: t('view-form'), onClick: () => navigate(`/${tApp('url-usertask')}/${taskId}`) },
        { label: t('view-list'), onClick: () => navigate(`/${tApp('url-usertask')}/${taskId}/${tApp('url-list')}`) },
        { label: t('view-icon'), onClick: () => navigate(`/${tApp('url-usertask')}/${taskId}/${tApp('url-icon')}`) },
    ];

    return (
        <Box
            fill
            direction="row"
            justify="between"
            pad="small"
        >
            <Box direction="row" gap="small" align="center">
                <Box width={isPhone ? '15rem' : '26rem'}>
                    <Select
                        size="medium"
                        placeholder="Select user task"
                        value={taskId}
                        options={options}
                        onChange={({ option }) => {
                            const taskIdMatch = option.match(/\(([^)]+)\)$/);
                            const extractedTaskId = taskIdMatch ? taskIdMatch[1] : option;
                            setTaskId(extractedTaskId);
                            loadUserTask(extractedTaskId);
                        }}
                        onClose={() => setOptions(options)}
                        onMore={onMore}
                        onSearch={(text) => {
                            const escapedText = text.replace(/[-\\^$*+?.()|[\]{}]/g, '\\$&');
                            const exp = new RegExp(escapedText, 'i');
                            setOptions(allTasks.filter((o) => exp.test(o)));
                        }}
                    />
                </Box>
                <TaskToggle value={taskFilter} onChange={setTaskFilter} />
            </Box>
            <Box>
                <Menu
                    disabled={!Boolean(userTaskId)}
                    label={t('views-label')}
                    items={viewMenuItems}
                />
            </Box>
        </Box>

    );
};

export { Header };
