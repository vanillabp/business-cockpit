import { Box, Drop } from 'grommet';
import React, { PropsWithChildren, useState, useRef } from 'react';
import { Copy } from 'grommet-icons';
import { BackgroundType, EdgeSizeType } from 'grommet/utils';

interface CopyClipbardProps {
  content?: string;
  size?: EdgeSizeType,
  color?: string;
  background?: BackgroundType;
};

// based on https://github.com/grommet/grommet/blob/master/src/js/components/Tip/Tip.js
const CopyClipboard = ({
  content,
  size = 'medium',
  background = { color: 'rgba(0, 0, 0, 0.5)' },
  color,
  children,
}: PropsWithChildren<CopyClipbardProps>) => {
  const targetRef = useRef<HTMLSpanElement>(null);
  const [over, setOver] = useState(false);
  if (!Boolean(content)) {
    return <>{ children }</>;
  }
  const copyContent = async (data: string) => {
      try {
        await navigator.clipboard.writeText(data);
        setOver(false);
      } catch (err) {
        console.error('Failed to copy: ', err);
      }
    };
  return (
      <span
          ref={ targetRef }
          style={ { display: 'inline-block' } }
          onMouseLeave={ () => setOver(false) }
          onMouseEnter={ () => setOver(true) }>
        { children }
        { over && targetRef.current &&
          <Drop
              plain
              target={ targetRef.current }
              stretch={ false }
              align={ { left: 'right' } }>
            <Box
                flex={ false }
                round="xsmall"
                background={ background }
                pad="xsmall">
              <Copy
                  color={ color }
                  size={
                    (
                      targetRef.current.clientHeight *
                          (size === 'xxsmall'
                              ? 0.3
                              : size === 'xsmall'
                              ? 0.4
                              : size === 'small'
                              ? 0.5
                              : size === 'medium'
                              ? 0.6
                              : size === 'large'
                              ? 0.8
                              : size === 'xlarge'
                              ? 1
                              : 1.2)
                    ).toString() + 'px' }
                  onClick={ () => copyContent(content!) } />
             </Box>
          </Drop>
        }
      </span>);
};

export { CopyClipboard };
