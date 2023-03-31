import { ResponsiveContext } from "grommet";
import { useContext } from "react";

type Screen = 'phone' | 'tablet' | 'computer';

type CurrentScreen = {
  isPhone: boolean;
  isNotPhone: boolean;
  isTablet: boolean;
  isNotTablet: boolean;
  isComputer: boolean;
  isNotComputer: boolean;
  currentScreen: Screen;
}
const useResponsiveScreen = (): CurrentScreen => {

  const size = useContext(ResponsiveContext);
  
  return {
      isPhone: size === 'small',
      isNotPhone: size !== 'small',
      isTablet: size === 'medium',
      isNotTablet: size !== 'medium',
      isComputer: size === 'large',
      isNotComputer: size !== 'large',
      currentScreen
          : size === 'small'
          ? 'phone'
          : size === 'medium'
          ? 'tablet'
          : 'computer'
    };
  
};

export default useResponsiveScreen;
