import { Box, Menu, Select } from 'grommet';
import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router-dom';
import { useResponsiveScreen } from '@vanillabp/bc-shared';
import { appNs } from '../app/DevShellApp.js';
import i18n from '../i18n.js';
import { Workflow } from '@vanillabp/bc-official-gui-client';
import { TaskToggle } from '../components/ToggleComponent.js';

i18n.addResources('en', 'workflow-header', {
    "views-label": "View",
    "view-page": "page",
    "view-icon": "icon",
    "view-list": "list",
});
i18n.addResources('de', 'workflow-header', {
    "views-label": "Ansicht",
    "view-page": "Seite",
    "view-icon": "Symbol",
    "view-list": "Liste",
});

const Header = () => {
    const navigate = useNavigate();
    const { t: tApp } = useTranslation(appNs);
    const { t } = useTranslation('workflow-header');

    const workflowIdParam: string | undefined = useParams()['workflowId'];
    const [workflowId, setWorkflowId] = useState(workflowIdParam);
    const [options, setOptions] = useState<string[]>([]);
    const [page, setPage] = useState(0);
    const [allWorkflows, setAllWorkflows] = useState<string[]>([]);
    const [taskFilter, setTaskFilter] = useState<'all' | 'open' | 'closed'>('all');

    useEffect(() => {
        const fetchWorkflows = async () => {
            try {
                const response = await fetch('/official-api/v1/workflow', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        pageSize: 20,
                        sort: 'updatedAt',
                        sortAscending: false,
                    }),
                });

                if (!response.ok) {
                    throw new Error('Failed to fetch workflows');
                }

                const data = await response.json();
                const formattedWorkflows = data.workflows.map((workflow: Workflow) =>
                    `${workflow.businessId || ''} ${workflow.bpmnProcessId} (${workflow.id})`
                );
                setAllWorkflows(formattedWorkflows);
                setOptions(formattedWorkflows.slice(0, 20));
            } catch (error) {
                console.error('Error fetching workflows:', error);
                setOptions([]);
                setAllWorkflows([]);
            }
        };

        fetchWorkflows();
    }, []);

    const onMore = () => {
        const nextPage = page + 1;
        const newOptions = allWorkflows.slice(0, (nextPage + 1) * 20);
        setOptions(newOptions);
        setPage(nextPage);
    };

    const loadWorkflow = (selectedWorkflowId?: string) =>
        navigate(`/${tApp('url-workflow')}/${selectedWorkflowId ?? workflowId}`, { replace: true });

    const viewMenuItems = [
        {
            label: t('view-page'),
            onClick: () => navigate(`/${tApp('url-workflow')}/${workflowId}`),
        },
        {
            label: t('view-list'),
            onClick: () =>
                navigate(`/${tApp('url-workflow')}/${workflowId}/${tApp('url-list')}`),
        },
        {
            label: t('view-icon'),
            onClick: () =>
                navigate(`/${tApp('url-workflow')}/${workflowId}/${tApp('url-icon')}`),
        },
    ];

    return (
        <Box
            fill="horizontal"
            direction="row"
            align="center"
            justify="between"
            pad={{ horizontal: 'medium', vertical: 'small' }}
        >
            <Box direction="row" gap="small" align="center">
                <Box width="20rem">
                    <Select
                        size="small"
                        placeholder="Select workflow"
                        value={workflowId}
                        options={options}
                        onChange={({ option }) => {
                            const workflowIdMatch = option.match(/\(([^)]+)\)$/);
                            const extractedWorkflowId = workflowIdMatch ? workflowIdMatch[1] : option;
                            setWorkflowId(extractedWorkflowId);
                            loadWorkflow(extractedWorkflowId);
                        }}
                        onClose={() => setOptions(options)}
                        onMore={onMore}
                        onSearch={(text) => {
                            const escapedText = text.replace(/[-\\^$*+?.()|[\]{}]/g, '\\$&');
                            const exp = new RegExp(escapedText, 'i');
                            setOptions(allWorkflows.filter((o) => exp.test(o)));
                        }}
                    />
                </Box>
                <TaskToggle value={taskFilter} onChange={setTaskFilter} />
            </Box>
            <Box>
                <Menu
                    disabled={!Boolean(workflowId)}
                    label={t('views-label')}
                    items={viewMenuItems}
                    size="small"
                />
            </Box>
        </Box>
    );
};

export { Header };
