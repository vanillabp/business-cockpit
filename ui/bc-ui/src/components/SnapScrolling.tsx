import { Box, BoxExtendedProps, Grid, GridExtendedProps } from "grommet";
import styled, { StyledComponent } from "styled-components";
import * as CSS from 'csstype';
import { FC } from "react";

interface SnapScrollingGridProps extends GridExtendedProps {
  snapDirection: 'horizontal' | 'vertical';
};

const SnapScrollingGrid: StyledComponent<FC<GridExtendedProps>, any, SnapScrollingGridProps, never> = styled(Grid)<SnapScrollingGridProps>`
    overflow-${ props => props.snapDirection === 'horizontal' ? 'x' : 'y' }: auto;
    scroll-snap-type: ${ props => props.snapDirection === 'horizontal' ? 'x' : 'y' } mandatory;
    scroll-behavior: smooth;
  `;

interface SnapAlignBoxProps extends BoxExtendedProps {
  snapAlign: CSS.Property.ScrollSnapAlign;
};

const SnapAlignBox: StyledComponent<FC<BoxExtendedProps>, any, SnapAlignBoxProps, never> = styled(Box)<SnapAlignBoxProps>`
    scroll-snap-align: ${ props => props.snapAlign ? props.snapAlign : 'start' };
  `;  

export { SnapScrollingGrid, SnapAlignBox };
