import { Box, MaskedInput } from 'grommet';
import React, { forwardRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import i18n from '../i18n';

i18n.addResources('en', 'km-input', {
      "thousand-separator": ",",
    });
i18n.addResources('de', 'km-input', {
      "thousand-separator": ".",
    });

const formatKm = (km: number | string | undefined, thousandSeparator: string) => {
  
  if (km === undefined) return "";
  const kmStr = km.toString();
  
  return kmStr.length > 3
      ? kmStr.substring(0, kmStr.length - 3) + thousandSeparator + kmStr.substring(kmStr.length - 3)
      : kmStr;

};

interface KmInputProps {
  km: number | undefined;
  setKm: (km: number | undefined) => void;
  onChange?: (km: number | undefined) => void;
};

const KmInput = forwardRef<HTMLInputElement, KmInputProps>(({
    km,
    setKm,
    onChange,
  }: KmInputProps, ref) => {
    
  const { t } = useTranslation('km-input');
  const thousandSeparator = t('thousand-separator');

  const [ focus, setFocus ] = useState(false);
  const [ kmStr, setKmStr ] = useState(formatKm(km, thousandSeparator));

  return (
    <Box
        border={
          focus
              ? {
                  side: 'all',
                  color: 'brand',
                  size: '2px'
                }
              : 'all'
        }
        margin={
          focus
              ? undefined
              : '1px'
        }
        round="xsmall"
        pad={ { vertical: 'xsmall', horizontal: 'small'} }
        gap='xsmall'
        align="center"
        direction="row">
      <MaskedInput
          ref={ ref }
          plain
          focusIndicator={ false }
          onFocus={ () => setFocus(true) }
          onBlur={ () => setFocus(false) }
          style={ { padding: 0 } }
          textAlign='end'
          mask={
               [ {
                  length: [1, 3],
                  regexp: /^[0-9]{1,3}$/,
                  placeholder: '1'
                },
                {
                  fixed: thousandSeparator
                },
                {
                  length: [3, 4],
                  regexp: /^[0-9]{1,4}$/,
                  placeholder: '234'
                } ]
              }
            value={ kmStr }
            onChange={ (event) => {
                if (!Boolean(event.target.value)) {
                  setKmStr('');
                  setKm(undefined);
                  if (onChange) onChange(undefined);
                  return;
                }
                const kmInput = parseInt(event.target.value
                    // @ts-ignore
                    .replaceAll(thousandSeparator, ''));
                if (kmInput > 999999) {
                  if (onChange) onChange(undefined);
                  return;
                }
                setKmStr(formatKm(kmInput, thousandSeparator));
                setKm(kmInput);
                if (onChange) onChange(kmInput);
              } } />
      km
    </Box>);
                                
});

export { KmInput };
