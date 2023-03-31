import React, { PropsWithChildren } from "react";
import { Box, BoxProps, Button, Heading, Layer } from "grommet";
import { TFunction } from "i18next";

interface ModalProperties extends BoxProps {
  show: boolean;
  header?: string | JSX.Element;
  abort?: () => void;
  action?: () => void;
  t: TFunction;
  actionLabel?: string;
  abortLabel?: string;
};

const Modal = ({
    show,
    header,
    abort,
    abortLabel = 'abort',
    action,
    actionLabel,
    t,
    children,
    ...props
  }: PropsWithChildren<ModalProperties>) => {
    
  if (!show) return <></>;
  
  return (
    <Layer
        full
        animation="fadeIn"
        background={ { color: 'dark-1', opacity: true } }
        onEsc={ abort }
        responsive={true}
        modal={true}>
      <Box
          height='100%'
          justify="center"
          direction="column">
        <Box
            justify='center'
            direction='row'
            pad='medium'>
          <Box
              pad='large'
              background='white'
              { ...props }>
            <>
              {
                header === undefined
                  ? undefined
                  : typeof header === 'string' 
                  ? <Heading
                        margin={ { vertical: 'xsmall' } }
                        level='2'>
                      { t(header as string) }
                    </Heading>
                  : header
              }
              {
                children
              }
              {
                action || abort
                  ? <Box
                        direction='row'
                        gap='small'
                        margin={ { bottom: 'medium' } }
                        align='center'
                        justify='around'>
                      {
                        action && actionLabel
                          ? <Button
                                label={ t(actionLabel) }
                                onClick={ action }
                                primary />
                          : undefined
                      }
                      {
                        abort
                          ? <Button
                                label={ t(abortLabel) }
                                onClick={ abort }
                                secondary />
                          : undefined
                      }
                    </Box>
                  : <></>
              }
            </>
          </Box>
        </Box>
      </Box>
    </Layer>);
  
};

export { Modal };
