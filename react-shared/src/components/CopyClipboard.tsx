import { Box, Drop } from 'grommet';
import React, { PropsWithChildren, useRef, useState } from 'react';
import { Copy } from 'grommet-icons';
import { BackgroundType, EdgeSizeType } from 'grommet/utils';

type ButtonSizeType = 'xxsmall' | 'xsmall' | 'small' | 'medium' | 'large' | 'xlarge' | 'xxlarge' | string;

interface CopyClipbardButtonProps {
  content?: string;
  color?: string;
  background?: BackgroundType;
  onCopy?: () => any;
  size?: ButtonSizeType;
};

const CopyClipboardButton = ({
    content,
    color = 'white',
    background = { color: 'rgba(0, 0, 0, 0.45)' },
    onCopy,
    size = 'medium',
}: CopyClipbardButtonProps) => {
  const copyContent = async (data: string) => {
    try {
      await navigator.clipboard.writeText(data);
    } catch (err) {
      console.error('Failed to copy: ', err);
    }
    if (onCopy) {
      onCopy();
    }
  };

  return (
      <Box
          flex={ false }
          round="xsmall"
          background={ background }
          margin={ { left: 'xsmall' } }
          pad="xsmall">
        <Copy
            color={ color }
            size={ size }
            onClick={ () => copyContent(content!) } />
      </Box>);
};

interface CopyClipbardProps {
  content?: string;
  size?: EdgeSizeType | string,
  color?: string;
  background?: BackgroundType;
};

// based on https://github.com/grommet/grommet/blob/master/src/js/components/Tip/Tip.js
const CopyClipboard = ({
  content,
  size = 'medium',
  background,
  color = 'white',
  children,
}: PropsWithChildren<CopyClipbardProps>) => {
  const targetRef = useRef<HTMLSpanElement>(null);
  const [ over, _setOver ] = useState(false);
  const hideTimer = React.useRef(0);
  if (!Boolean(content)) {
    return <>{children}</>;
  }
  const setOver = (status: boolean) => {
    if (Boolean(hideTimer.current)) {
      window.clearTimeout(hideTimer.current);
      hideTimer.current = 0;
    }
    if (status) {
      _setOver(status);
      return;
    }
    hideTimer.current = window.setTimeout(() => {
        _setOver(false);
        hideTimer.current = 0;
      }, 200);
  };
  const sizeFactor =
    size === "xxsmall"
      ? 0.3
      : size === "xsmall"
      ? 0.4
      : size === "small"
      ? 0.5
      : size === "medium"
      ? 0.6
      : size === "large"
      ? 0.8
      : size === "xlarge"
      ? 1
      : size === "xxlarge"
      ? 1.2
      : 0;
  const iconSize =
    sizeFactor === 0 || !targetRef.current
      ? size
      : `${targetRef.current.clientHeight * sizeFactor}px`;
  return (
      <span
          ref={ targetRef }
          style={ { display: 'inline-block' } }
          onMouseLeave={ event => setOver(false) }
          onMouseEnter={ event => setOver(true) }>
        { children }
        { over && targetRef.current &&
          <Drop
              plain
              target={ targetRef.current }
              stretch={ false }
              align={ { left: 'right' } }>
            <CopyClipboardButton
                content={ content }
                color={ color }
                onCopy={ () => setOver(false) }
                size={ iconSize }
                background={ background } />
          </Drop>
        }
      </span>);
};

export { CopyClipboard, CopyClipboardButton };
