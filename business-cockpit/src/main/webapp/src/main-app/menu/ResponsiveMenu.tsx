import React, { useRef } from 'react';
import { Layer, Collapsible, Box, Button, Keyboard } from 'grommet';
import { Menu } from './Menu';
import { FormClose } from 'grommet-icons';
import { useAppContext } from '../../AppContext';
import useResponsiveScreen from '../../utils/responsiveUtils';
import useOnClickOutside from '../../utils/clickOutside';

const ResponsiveMenu = () => {
  
  const { state, showMenu } = useAppContext();
  const { isPhone } = useResponsiveScreen();
  
  const hideMenu = () => showMenu(false);
  
  const stateShowMenuRef = useRef(state.showMenu);
  stateShowMenuRef.current = state.showMenu;
  
  const ref = useRef(null);
  useOnClickOutside(ref, event => {
      if (!stateShowMenuRef.current) return;
      event.preventDefault();
      event.stopPropagation();
      hideMenu();
    });

  return isPhone && state.showMenu
      ? <Layer
            onEsc={ hideMenu }
            responsive={ true }
            modal={ true }>
          <Box
              background='light-2'
              tag='header'
              justify='end'
              align='center'
              direction='row'
              pad='small'>
            <Button
                plain
                focusIndicator={ false }
                icon={ <FormClose /> }
                onClick={ hideMenu }
            />
          </Box>
          <Box
              fill
              pad="small"
              background='light-2'>
            <Menu />
          </Box>
        </Layer>
      : <Keyboard
            onEsc={ (event) => {
                  event.stopPropagation();
                  hideMenu();
                } }
            target='document'>
          <Box
              style={ { position: 'absolute', top: 0, right: '0', zIndex: 20 } }>
            <Box
                height="xxsmall"></Box>
            <Collapsible
                direction="vertical"
                open={ state.showMenu }>
              <Box
                  ref={ ref }
                  width='medium'
                  background='light-2'
                  elevation='small'>
                <Menu />
              </Box>
            </Collapsible>
          </Box>
        </Keyboard>;
                
}

export { ResponsiveMenu };
