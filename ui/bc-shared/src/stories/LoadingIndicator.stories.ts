import type { Meta, StoryObj } from '@storybook/react';

import { LoadingIndicator } from '../components/LoadingIndicator';

// More on how to set up stories at: https://storybook.js.org/docs/react/writing-stories/introduction#default-export
const meta: Meta<typeof LoadingIndicator> = {
  title: 'components/LoadingIndicator',
  component: LoadingIndicator,
  // This component will have an automatically generated Autodocs entry: https://storybook.js.org/docs/react/writing-docs/autodocs
  tags: ['autodocs'],
  // More on argTypes: https://storybook.js.org/docs/react/api/argtypes
  argTypes: {
    backgroundColor: { control: 'color' },
  },
};

export default meta;
type Story = StoryObj<typeof LoadingIndicator>;

export const Primary: Story = {
};
