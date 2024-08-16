import { ThemeType } from "grommet";
import { css } from "styled-components";

export const theme: ThemeType = {
  global: {
    colors: {
      brand: '#e2e2e2',
      'accent-1': '#ffe699',
      'accent-2': '#bf9000',
      'accent-3': '#663300',
      'accent-4': '#333333',
      'placeholder': '#bbbbbb',
      'light-5': '#c7c7c7',
      'light-6': '#b4b4b4',
      'link': '#663300',
      'list-new': 'rgba(102, 51, 0, 0.1)',
      'list-text-new': 'dark-1',
      'list-updated': 'rgba(255, 230, 153, 0.15)',
      'list-text-updated': 'dark-1',
      'list-ended': 'rgba(242, 242, 242, 0.25)',
      'list-text-ended': 'dark-4',
      'list-removed_from_list': 'rgba(242, 242, 242, 0.25)',
      'list-text-removed_from_list': 'dark-1',
      'list-refresh': '#ffe699',
    },
    font: {
      family: 'Roboto',
      size: '18px',
      height: '20px',
    },
    focus: {
      border:  {
        color: '#e0a244',
      },
      outline: {
        color: '#e0a244',
      }
    },
    drop: {
      zIndex: 99,
    },
  },
  table: {
    header: {
      border: undefined,
    },
    body: {
      border: undefined,
      extend: css`
        overflow: visible;
      `
    },
  },
  heading: {
    color: '#444444',
    extend: css`
      margin-top: 0;
    `
  },
  formField: {
    label: {
      requiredIndicator: true,
    }
  },
  textArea: {
    extend: css`
      font-weight: normal;
      ::placeholder {
        font-weight: normal;
        color: ${props => props.theme.global.colors.placeholder};
      }
    `,
  },
  maskedInput: {
    extend: css`
      ::placeholder {
        font-weight: normal;
        color: ${props => props.theme.global.colors.placeholder};
      }
    `,
  },
  textInput: {
    extend: css`
      ::placeholder {
        font-weight: normal;
        color: ${props => props.theme.global.colors.placeholder};
      }
    `,
    placeholder: {
      extend: css`
          font-weight: normal;
          color: ${props => props.theme.global.colors.placeholder};
        `
    }
  },
  button: {
    default: {
      background: '#ffffff',
      border: { color: 'accent-1', width: '3px' },
      color: 'accent-3'
    },
    primary: {
      background: 'accent-1',
      border: { color: 'accent-2', width: '3px' },
      color: 'accent-3'
    },
    secondary: {
      background: 'accent-2',
      border: { color: 'accent-3', width: '3px' },
      color: 'accent-1'
    },
    hover: {
      default: {
        background: 'accent-1',
        color: 'accent-3'
      },
      primary: {
        background: 'accent-2',
        color: 'accent-1'
      },
      secondary: {
        background: 'accent-3',
        color: 'accent-1'
      }
    },
    disabled: {
      opacity: 1,
      color: 'dark-4',
      background: 'light-2',
      border: { color: 'light-4' }
    }
  },
  accordion: {
    heading: {
      margin: 'small'
    },
    icons: {
      color: 'accent-3'
    }
  },
  dataTable: {
    pinned: {
      header: {
        background: {
          color: 'accent-2',
          opacity: 'strong'
        },
        extend: css`
          z-index: 19;
        `
      },
    }
  },
  page: {
    wide: {
      width: {
        min: 'small',
        max: 'xlarge'
      }
    }
  },
  paragraph: {
    extend: css`
      margin-top: 0;
    `
  },
  checkBox: {
    size: '20px',
    color: 'black',
  },
  tip: {
      content: {
          background: "white"
      },
  },
  layer: {
    zIndex: '100',
  },
};
