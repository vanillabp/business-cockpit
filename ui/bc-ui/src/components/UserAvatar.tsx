import { Person } from '@vanillabp/bc-official-gui-client';
import { User as UserMale } from 'grommet-icons';
import { Avatar, Box } from 'grommet';
import { BorderType } from 'grommet/utils';
import React, { useRef, useState } from 'react';
import { TranslationFunction, useOnClickOutside, UserDetailsBox, useResponsiveScreen } from '@vanillabp/bc-shared';

type UserAvatarProps = {
  t: TranslationFunction;
  user: Person;
  isUserLoggedIn?: boolean;
  border?: BorderType;
  size?: 'xsmall' | 'small' | 'medium' | 'large' | 'xlarge' | string;
};

const hashCode = (value: string) => value
    .split('')
    .reduce((s, c) => Math.imul(31, s) + c.charCodeAt(0) | 0, 0);

const UserAvatar = ({
  t,
  user,
  isUserLoggedIn = false,
  border,
  size = 'medium',
}: UserAvatarProps) => {

  const backgroundColor = user.id
      ? `hsl(${ (hashCode(user.id) * 207) % 360 }, 70%, 45%)`
      : '#333333';

  const intSize = parseInt(size);
  let symbolSize: string;
  if (Number.isNaN(intSize)) {
    switch (size) {
      case 'xsmall':
        symbolSize = '10rem';
        break;
      case 'small':
        symbolSize = '16rem';
        break;
      case 'medium':
        symbolSize = '28rem';
        break;
      case 'large':
        symbolSize = '40rem';
        break;
      case 'xlarge':
        symbolSize = '57rem';
        break;
      default:
        symbolSize = '27rem';
    }
  } else {
    symbolSize = `${ intSize * 0.65 }px`;
  }
  
  const [ showDetails, setShowDetails ] = useState<Person | undefined>(undefined);
  const ref = useRef(null);
  useOnClickOutside(ref, event => {
      if (!showDetails) return;
      event.preventDefault();
      event.stopPropagation();
      setShowDetails(undefined);
    });
  const loadAndShowDetails = async () => {
    if (isUserLoggedIn) return;
    // const details = await loginApi.getUserDetails({ memberId: user.memberId! });
    setShowDetails(user);
  };
  
  const { isPhone } = useResponsiveScreen();
  
  return (
      <Box
          style={ { position: 'relative' } }
          onMouseDown={ () => loadAndShowDetails() }>
        {
          showDetails
              ? <Box
                    ref={ ref }
                    background='white'
                    style={ {
                        position: 'absolute',
                        maxWidth: 'unset',
                        margin: '-0.3rem',
                        zIndex: 20,
                        opacity: 2,
                        boxShadow: '5px 5px 10px rgba(0, 0, 0, 0.5)'
                    } }
                    height="xsmall"
                    width="16rem"
                    round="small"
                    border
                    pad={ {
                        left: `calc(${symbolSize} * 0.15)`,
                        top: '0.3rem'
                      } }>
                  <UserDetailsBox user={ user } t={ t } />
                </Box>
              : undefined
        }
        <Avatar
            style={ { zIndex: (!isUserLoggedIn) && showDetails ? 20 : undefined } }
            background={ backgroundColor }
            size={ size }
            border={ border }
            src={ user.avatar ? `/api/v1/gui/user/${ user.id }/avatar?ts=${ user.avatar }` : undefined }>
          <UserMale color='accent-1' size={ symbolSize } />
        </Avatar>
      </Box>);
    
}

export { UserAvatar }
