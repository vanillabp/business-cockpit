import { Box, Menu, Select } from 'grommet';
import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router-dom';
import { useResponsiveScreen } from '@vanillabp/bc-shared';
import { appNs } from '../app/DevShellApp.js';
import i18n from '../i18n.js';
import { Workflow } from '@vanillabp/bc-official-gui-client';
import { TaskToggle } from "../components/ToggleComponent.js";

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
    const { isPhone } = useResponsiveScreen();
    const navigate = useNavigate();
    const { t: tApp } = useTranslation(appNs);
    const { t } = useTranslation('workflow-header');

    const workflowIdParam: string | undefined = useParams()['workflowId'];
    const [workflowId, setWorkflowId] = useState(workflowIdParam);
    const [options, setOptions] = useState<string[]>([]);
    const [allWorkflows, setAllWorkflows] = useState<string[]>([]);
    const [taskFilter, setTaskFilter] = useState<'all' | 'open' | 'closed'>('all');
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);

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
                        pageNumber: page,
                        sort: 'createdAt',
                        sortAscending: false
                    })
                });

                if (!response.ok) {
                    throw new Error('Failed to fetch workflows');
                }

                const data = await response.json();
                const formatted = data.workflows.map((workflow: Workflow) =>
                    `${workflow.businessId || ''} ${workflow.bpmnProcessId} (${workflow.id})`
                );

                setAllWorkflows(prev => [...prev, ...formatted]);
                setOptions(prev => [...prev, ...formatted]);

                // check if there are more items
                const totalPages = data.page?.totalPages ?? 0;
                if (page + 1 >= totalPages) {
                    setHasMore(false);
                }
            } catch (error) {
                console.error('Error fetching workflows:', error);
                setOptions([]);
                setAllWorkflows([]);
            }
        };

        fetchWorkflows();
    }, [page]);

    const onMore = () => {
        if (hasMore) {
            setPage(prev => prev + 1);
        }
    };

    const loadWorkflow = (selectedWorkflowId?: string) =>
        navigate(`/${tApp('url-workflow')}/${selectedWorkflowId ?? workflowId}`, { replace: true });

    const viewMenuItems = [
        { label: t('view-page'), onClick: () => navigate(`/${tApp('url-workflow')}/${workflowId}`) },
        { label: t('view-list'), onClick: () => navigate(`/${tApp('url-workflow')}/${workflowId}/${tApp('url-list')}`) },
        { label: t('view-icon'), onClick: () => navigate(`/${tApp('url-workflow')}/${workflowId}/${tApp('url-icon')}`) }
    ];

    return (
        <Box
            fill
            direction="row"
            justify="between"
            pad="small">

            <Box direction="row" gap="small" align="center">
                <Box width={isPhone ? '15rem' : '26rem'}>
                    <Select
                        size="medium"
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
                />
            </Box>
        </Box>
    );
};

export { Header };
