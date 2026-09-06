import "react-i18next";

// see https://www.i18next.com/overview/typescript

declare module 'react-i18next' {
  interface CustomTypeOptions {
    allowObjectInHTMLChildren: true,
  };
};
