import React from 'react';
import { Box, Text, Button} from 'grommet';

type TaskFilter = 'all' | 'open' | 'closed';

interface TaskToggleProps {
    value: TaskFilter;
    onChange: (value: TaskFilter) => void;
}

const TaskToggle: React.FC<TaskToggleProps> = ({ value, onChange }) => {
    const options: { label: string; value: TaskFilter; background?: string }[] = [
        { label: 'All Tasks', value: 'all', background: 'light-2' },
        { label: 'Open Tasks', value: 'open', background: 'rgba(0, 200, 0, 0.2)' },
        { label: 'Closed Tasks', value: 'closed', background: 'rgba(200, 0, 0, 0.2)' },
    ];

    return (
        <Box direction="row" gap="xsmall" align="center" margin={{ left: 'small' }}>
            {options.map((option) => (
                <Box
                    key={option.value}
                    pad={{ vertical: '3px', horizontal: '10px' }}
                    round="xsmall"
                    background={option.background || 'light-2'}
                    border={{
                        color: value === option.value ? 'brand' : 'border',
                        size: 'xsmall',
                    }}
                    style={{
                        cursor: 'pointer',
                        fontWeight: value === option.value ? 'bold' : 'normal',
                    }}
                    onClick={() => onChange(option.value)}
                >
                    <Text size="small">{option.label}</Text>
                </Box>
            ))}
        </Box>
    );
};

export { TaskToggle };
