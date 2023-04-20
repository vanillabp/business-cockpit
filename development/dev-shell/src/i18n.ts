import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

i18n
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

const t = i18n.t;

export { t };

export default i18n;
