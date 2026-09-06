import { Box, Button, Header, Heading, Image } from "grommet";
import { Login, Menu as MenuIcon } from 'grommet-icons';
import { useRef } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAppContext } from '../../AppContext';
import { useResponsiveScreen } from "@vanillabp/bc-shared";
import { ResponsiveMenu } from "./ResponsiveMenu";

const AppHeader = () => {
  
  const { isPhone } = useResponsiveScreen();
  const { state, showMenu } = useAppContext();
  // @ts-ignore TS2589: Type instantiation is excessively deep and possibly infinite.
  const { t } = useTranslation(state.title);
  const navigate = useNavigate();

  const stateShowMenuRef = useRef(state.showMenu);
  stateShowMenuRef.current = state.showMenu;

  const toggleMenu = () => {
      if (stateShowMenuRef.current) return;
      showMenu(!state.showMenu);
    };
    
  const goToLogin = () => {
      document.getElementById('login')!.scrollIntoView({ behavior: 'smooth' })
    };
  
  return (
    <Header
        tag='header'
        style={ { zIndex: 30 } }
        background={ state.intern ? 'accent-4' : 'white' }
        elevation='medium'
        height='xxsmall'
        pad='xxsmall'>
      <Box
          onClick={() => navigate('/')}
          focusIndicator={false}
          direction='row'
          fill='vertical'
          align='center'>
        <Box
            fill='vertical'
            width={ { max: '3.5rem' } }>
            
          <Image
              src='/assets/logo-192.png'
              fit='contain' />
        </Box>
        {
          isPhone ? (
              <Heading
                  color={ state.intern ? 'light-4' : 'dark-2' }
                  margin={ { horizontal: 'small', vertical: 'none' } }
                  level='2'>{t('title.short')}</Heading>
            ) : (
              <Heading
                  color={ state.intern ? 'light-4' : 'dark-2' }
                  margin={ { horizontal: 'small', vertical: 'none' } }
                  level='3'>{t('title.long')}</Heading>
            )
        }
      </Box>
      <Box>
        {
          state.currentUser || isPhone
              ? <Button
                    plain
                    focusIndicator={ false }
                    margin='small'
                    icon={ state.currentUser
                        ? <MenuIcon
                              color={ state.intern ? 'light-4' : 'dark-2' } />
                        : <Login
                              color={ state.intern ? 'light-4' : 'dark-2' } /> }
                    onMouseDown={ state.currentUser
                        ? toggleMenu
                        : goToLogin }
                    style={ { position: 'relative' } } />
              : undefined
        }
        <ResponsiveMenu />
      </Box>
    </Header>
  );
        
}

export { AppHeader };
