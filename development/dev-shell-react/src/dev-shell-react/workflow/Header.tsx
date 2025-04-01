import { Box, Menu, Select, Text} from 'grommet';
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
    const [workflows, setWorkflows] = useState<Workflow[]>([]);
    const [searchText, setSearchText] = useState('');
    const [taskFilter, setTaskFilter] = useState<'all' | 'open' | 'closed'>('all');
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);

    const selectOptions = workflows.map(workflow => ({
        label: `${workflow.businessId || ''} | ${workflow.bpmnProcessId} (${workflow.id})`,
        value: workflow.id,
    }));

    const filteredOptions = selectOptions.filter(option => {
        const escapedText = searchText.replace(/[-\\^$*+?.()|[\]{}]/g, '\\$&');
        const regex = new RegExp(escapedText, 'i');
        return regex.test(option.label);
    });

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

            setWorkflows(prev => [...prev, ...data.workflows]);

            const totalPages = data.page?.totalPages ?? 0;
            if (page + 1 >= totalPages) {
                setHasMore(false);
            }
        } catch (error) {
            console.error('Error fetching workflows:', error);
            setWorkflows([]);
        }
    };

    useEffect(() => {
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
                        placeholder="Select workflow"
                        labelKey="label"
                        valueKey={{ key: 'value', reduce: true }}
                        value={workflowId}
                        valueLabel={
                            (() => {
                                const selected = selectOptions.find(o => o.value === workflowId);
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
                            setWorkflowId(value);
                            loadWorkflow(value);
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
                    disabled={!Boolean(workflowId)}
                    label={t('views-label')}
                    items={viewMenuItems}
                />
            </Box>
        </Box>
    );
};


export { Header };
