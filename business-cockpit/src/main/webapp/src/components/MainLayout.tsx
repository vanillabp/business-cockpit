import { Box, BoxExtendedProps, BoxTypes, Heading as GrommetHeading, HeadingProps, Page, PageContent, Paragraph } from "grommet";
import React, { PropsWithChildren } from "react";
import useResponsiveScreen from '../utils/responsiveUtils';

const MainLayout = ({ children, ...props }: PropsWithChildren<BoxExtendedProps>) => {

  return (
      <Page
          kind='wide'>
        <PageContent
            responsive
            margin={ {
                horizontal: 'large',
                vertical: 'medium'
              } }
            { ...props }>
          {
            children
          }
        </PageContent>
      </Page>);
      
}

const SubHeading = ({ icon, children, ...props }: PropsWithChildren<IconHeadingProps>) => {
  
  return icon === undefined
      ? <GrommetHeading
            level='4'
            color='accent-3'
            margin={ {
                top: 'xsmall',
                bottom: 'small',
              } }
            {...props}>
          {
            children
          }
        </GrommetHeading>
      : <Box
            margin={ {
                top: 'xsmall',
                bottom: 'small',
              } }
            direction="row"
            align="center"
            gap="small">
          { icon }
          <GrommetHeading
              level='4'
              color='accent-3'
              margin="0"
              {...props}>
            {
              children
            }
          </GrommetHeading>
        </Box>;

}

const TextHeading = ({ icon, children, ...props }: PropsWithChildren<IconHeadingProps>) => {
  
  return icon === undefined
      ? <Paragraph
            style={ { fontStyle: 'italic' } }
            margin={ {
                top: 'xsmall',
                bottom: 'small',
              } }
            {...props}>
          {
            children
          }
        </Paragraph>
      : <Box
            margin={ {
                top: 'xsmall',
                bottom: 'small',
              } }
            style={ { fontStyle: 'italic' } }
            direction="row"
            align="center"
            gap="small">
          { icon }
          <Paragraph
              margin="0"
              {...props}>
            {
              children
            }
          </Paragraph>
        </Box>;

}

interface IconHeadingProps extends HeadingProps {
  icon?: JSX.Element;
};

const Heading = ({ icon, margin, children, ...props }: PropsWithChildren<IconHeadingProps>) => {
  
  return icon === undefined
      ? <GrommetHeading
            level='2'
            {...props}
            margin={
              margin
                  ? margin
                  : {
                      top: 'small',
                      bottom: 'medium',
                    }
            }>
          {
            children
          }
        </GrommetHeading>
      : <Box
            direction="row"
            align="center"
            gap="small"
            margin={
              margin
                  ? margin
                  : {
                      top: 'small',
                      bottom: 'medium',
                    }
            }>
          { icon }
          <GrommetHeading
              level="2"
              {...props}
              margin="0">
            {
              children
            }
          </GrommetHeading>
        </Box>;
   
}

const Content = ({ children, ...props }: PropsWithChildren<BoxTypes>) => {

  const { isPhone } = useResponsiveScreen();

  return (
      <Box
          { ...props }
          pad={ { bottom: isPhone ? 'medium' : 'small' } }>{
            children
          }</Box>
    );
  
}

export { MainLayout, Heading, SubHeading, Content, TextHeading };
