import { User as UserDto, Sex, User } from '../client/gui';
import { User as UserMale, UserFemale } from 'grommet-icons';
import { Anchor, Avatar, Box, Text } from 'grommet';
import { BorderType } from 'grommet/utils';
import React, { useRef, useState } from 'react';
import { useOnClickOutside } from '@bc/shared';
import { useAppContext } from '../AppContext';
import { Content } from './MainLayout';

type UserAvatarProps = {
  user: UserDto;
  border?: BorderType;
  size?: 'xsmall' | 'small' | 'medium' | 'large' | 'xlarge' | string;
};

const hashCode = (value: string) => value
    .split('')
    .reduce((s, c) => Math.imul(31, s) + c.charCodeAt(0) | 0, 0);

const UserAvatar = ({
  user,
  border,
  size = 'medium',
}: UserAvatarProps) => {

  const { state } = useAppContext();

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
  
  const [ showDetails, setShowDetails ] = useState<User | undefined>(undefined);
  const ref = useRef(null);
  useOnClickOutside(ref, event => {
      if (!showDetails) return;
      event.preventDefault();
      event.stopPropagation();
      setShowDetails(undefined);
    });
  const loadAndShowDetails = async () => {
    if (state.currentUser!.id === user.id) return;
    // const details = await loginApi.getUserDetails({ memberId: user.memberId! });
    setShowDetails(user);
  };
  
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
                  <Text
                      weight="bold">{ user.id }</Text>
                  <Content>
                    <Text truncate="tip">
                      { showDetails.firstName } { showDetails.lastName }
                    </Text>
                    <Anchor
                        href={ `mailto:${ showDetails.email }` }>
                      { showDetails.email }
                    </Anchor>
                  </Content>
                </Box>
              : undefined
        }
        <Avatar
            style={ { zIndex: (state.currentUser!.id !== user.id) && showDetails ? 20 : undefined } }
            background={ backgroundColor }
            size={ size }
            border={ border }
            src={ user.avatar ? `/api/v1/gui/user/${ user.id }/avatar?ts=${ user.avatar }` : undefined }>
          {
            user.sex === Sex.Female
                ? <UserFemale color='accent-1' size={ symbolSize } />
                : <UserMale color='accent-1' size={ symbolSize } />
          }
        </Avatar>
      </Box>);
    
}

export { UserAvatar }
