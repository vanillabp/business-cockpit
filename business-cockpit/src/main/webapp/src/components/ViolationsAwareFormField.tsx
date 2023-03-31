import React from "react";
import { FormField, FormFieldExtendedProps } from "grommet";
import { TFunction } from "i18next";

interface ViolationsAwareFormFieldProps extends Omit<FormFieldExtendedProps, 'name'> {
  violations: any;
  t: TFunction;
  label: string;
  name: string;
}

const ViolationsAwareFormField = ({
  violations,
  name,
  label,
  t,
  children = undefined,
  onChange = undefined,
  ...props
}: ViolationsAwareFormFieldProps) => (<FormField
      name={name}
      onChange={ value => {
          violations[name] = undefined;
          if (onChange) {
            onChange(value);
          }
        }
      }
      label={ t(label) }
      error={ violations[name] !== undefined ? t(`${label}_${violations[name]}`) : undefined }
      {...props}>{children}</FormField>);

export { ViolationsAwareFormField };
