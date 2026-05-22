import React from 'react';
import { Box, Text } from 'grommet';

type TaskFilter = 'all' | 'open' | 'closed';

type WorkflowFilter = 'all' | 'active' | 'inactive';

interface TaskToggleProps {
    taskValue?: TaskFilter;
    taskOnChange?: (value: TaskFilter) => void;
    workflow: boolean;
}

const taskOptions: { label: string; value: TaskFilter; background?: string }[] = [
    { label: 'All', value: 'all', background: 'light-2' },
    { label: 'Open', value: 'open', background: 'rgba(0, 200, 0, 0.2)' },
    { label: 'Closed', value: 'closed', background: 'rgba(200, 0, 0, 0.2)' },
];

interface WorkflowToggleProps {
    workflowValue?: WorkflowFilter;
    workflowOnChange?: (value: WorkflowFilter) => void;
    workflow: boolean;
}

const workflowOptions: { label: string; value: WorkflowFilter; background?: string }[] = [
    { label: 'All', value: 'all', background: 'light-2' },
    { label: 'Active', value: 'active', background: 'rgba(0, 200, 0, 0.2)' },
    { label: 'Inactive', value: 'inactive', background: 'rgba(200, 0, 0, 0.2)' },
];

const TaskToggle: React.FC<TaskToggleProps & WorkflowToggleProps> = ({ workflow, taskValue, taskOnChange, workflowValue, workflowOnChange }) => {

    return (
        <Box direction="row" gap="xsmall" align="center" margin={{ left: 'small' }}>
            {(workflow ? workflowOptions : taskOptions).map((option) => (
                <Box
                    key={option.value}
                    pad={{ vertical: '3px', horizontal: '10px' }}
                    round="xsmall"
                    background={option.background || 'light-2'}
                    border={{
                        color: (workflow ? workflowValue : taskValue) === option.value ? 'brand' : 'border',
                        size: 'xsmall',
                    }}
                    style={{
                        cursor: 'pointer',
                        fontWeight: (workflow ? workflowValue : taskValue) === option.value ? 'bold' : 'normal',
                    }}
                    onClick={() => workflow
                        ? workflowOnChange!(option.value as WorkflowFilter)
                        : taskOnChange!(option.value as TaskFilter)}
                >
                    <Text size="small">{option.label}</Text>
                </Box>
            ))}
        </Box>
    );
};

export { TaskToggle };
