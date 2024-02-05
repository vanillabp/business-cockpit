import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

const i18nInstance = i18n.createInstance();

i18nInstance
  .use(initReactI18next)
  .init({
    lng: 'de',
    fallbackLng: 'en',
    react: {
      useSuspense: false
    },
    interpolation: {
      escapeValue: false // react already safes from xss
    }
  });

// @ts-ignore
window.i18n = i18nInstance;

const t = i18n.t;

export { t };
