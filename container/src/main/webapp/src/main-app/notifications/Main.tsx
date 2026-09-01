import { useEffect, useLayoutEffect, useState } from 'react';
import { Box, Button, CheckBox, Heading, Table, TableBody, TableCell, TableHeader, TableRow, Text, TextInput, Tip } from 'grommet';
import { i18n, useResponsiveScreen } from '@vanillabp/bc-shared';
import { useTranslation } from 'react-i18next';
import { NotificationConfiguration, NotificationMedium, NotificationWorkflow, RecipientMediumConfiguration } from '@vanillabp/bc-official-gui-client';
import { useAppContext } from '../../AppContext';
import { useNotificationConfigApi } from '../../utils/apis';
import {
  emptyConfiguration,
  globalMediumEnabled,
  globalNoneChecked,
  setGlobalMedium,
  setGlobalNone,
  setWorkflowMedium,
  setWorkflowNone,
  translate,
  workflowKey,
  workflowMediumEnabled,
  workflowNoneChecked,
} from './notificationConfig';

i18n.addResources('en', 'notifications', {
  "title.short": "Notifications",
  "title.long": "Notifications",
  "intro": "Choose which task changes you want to be notified about.",
  "global": "All tasks",
  "per-workflow": "Exceptions for tasks of certain workflows",
  "workflow": "Workflow",
  "none": "None",
  "all-via": "All via {{medium}}",
  "recipient": "Delivery",
  "save": "Save",
  "saved": "Notification settings saved.",
  "no-media": "The technical setup of notifications is not yet complete. No notification medium has been configured. Please contact the application administrator.",
});
i18n.addResources('de', 'notifications', {
  "title.short": "Benachrichtigungen",
  "title.long": "Benachrichtigungen",
  "intro": "Lege fest, über welche Änderungen an Aufgaben du benachrichtigt werden möchtest.",
  "global": "Alle Aufgaben",
  "per-workflow": "Ausnahmen für Aufgaben aus bestimmten Vorgängen",
  "workflow": "Vorgang",
  "none": "Keine",
  "all-via": "Alle via {{medium}}",
  "recipient": "Zustellung",
  "save": "Speichern",
  "saved": "Benachrichtigungs-Einstellungen gespeichert.",
  "no-media": "Die technische Einrichtung der Benachrichtigungen ist noch nicht abgeschlossen. Es wurde kein Benachrichtigungsmedium konfiguriert. Bitte wenden Sie sich an den Anwendungsadministrator.",
});

const Notifications = () => {

  const { setAppHeaderTitle, toast } = useAppContext();
  const { t } = useTranslation('notifications');
  const { isPhone } = useResponsiveScreen();
  const currentLanguage = i18n.language;
  const api = useNotificationConfigApi();

  const [media, setMedia] = useState<Array<NotificationMedium>>();
  const [config, setConfig] = useState<NotificationConfiguration>();
  const [workflows, setWorkflows] = useState<Array<NotificationWorkflow>>();
  const [recipient, setRecipient] = useState<Array<RecipientMediumConfiguration>>();
  const [recipientValues, setRecipientValues] = useState<{ [medium: string]: { [type: string]: string } }>({});

  useLayoutEffect(() => {
    setAppHeaderTitle('notifications', false);
  }, [ setAppHeaderTitle ]);

  useEffect(() => {
    const load = async () => {
      const [ loadedMedia, loadedConfig, loadedWorkflows, loadedRecipient ] = await Promise.all([
        api.getNotificationMedia(),
        api.getNotificationConfiguration(),
        api.getNotificationWorkflows(),
        api.getRecipientConfiguration(),
      ]);
      setMedia(loadedMedia);
      setConfig(loadedConfig ?? emptyConfiguration());
      setWorkflows(loadedWorkflows);
      setRecipient(loadedRecipient);
      const values: { [medium: string]: { [type: string]: string } } = {};
      loadedRecipient.forEach(mediumConfig => {
        values[mediumConfig.medium] = {};
        (mediumConfig.values ?? []).forEach(value => {
          values[mediumConfig.medium][value.type] = value.value ?? '';
        });
      });
      setRecipientValues(values);
    };
    load();
  }, [ api ]);

  if (!media || !config || !workflows || !recipient) {
    return <Box pad="medium"><Text>...</Text></Box>;
  }

  const mediaTypes = media.map(medium => medium.type);
  const mediumName = (type: string) => translate(media.find(m => m.type === type)?.name, currentLanguage) || type;

  if (mediaTypes.length === 0) {
    return (
      <Box pad="medium" gap="medium">
        <Heading level="3" margin="none">{ t('title.long') }</Heading>
        <Text>{ t('no-media') }</Text>
      </Box>);
  }

  const save = async () => {
    await api.saveNotificationConfiguration({ notificationConfiguration: config });
    await Promise.all(recipient.map(mediumConfig => api.saveRecipientConfiguration({
      mediumType: mediumConfig.medium,
      requestBody: recipientValues[mediumConfig.medium] ?? {},
    })));
    toast({ title: t('title.short'), message: t('saved') });
  };

  return (
    <Box pad="medium" gap="medium" overflow={ { vertical: 'auto' } }>
      
      <Text>{ t('intro') }</Text>      <Box gap="small">
        <Heading level="4" margin={ { vertical: 'small' } }>{ t('global') }</Heading>
        <Box direction={ isPhone ? 'column' : 'row' } gap={ isPhone ? 'xsmall' : 'medium' } wrap>
          <CheckBox
              label={ t('none') }
              checked={ globalNoneChecked(config, mediaTypes) }
              onChange={ event => { if (event.target.checked) { setConfig(prev => setGlobalNone(prev!)); } } } />
          {
            mediaTypes.map(type => (
              <CheckBox
                  key={ `global-${type}` }
                  label={ t('all-via', { medium: mediumName(type) }) }
                  checked={ globalMediumEnabled(config, type) }
                  onChange={ event => setConfig(prev => setGlobalMedium(prev!, type, event.target.checked)) } />
            ))
          }
        </Box>
      </Box>      {
        workflows.length === 0 ? undefined :
        <Box gap="small">
          <Heading level="4" margin={ { vertical: 'small' } }>{ t('per-workflow') }</Heading>
          {
            isPhone ?
            <Box gap="small">
              {
                workflows.map(workflow => {
                  const key = workflowKey(workflow);
                  return (
                    <Box key={ key } gap="xsmall" pad={ { vertical: 'xsmall' } } border={ { side: 'bottom' } }>
                      <Text weight="bold">{ translate(workflow.workflowTitle, currentLanguage) || key }</Text>
                      <CheckBox
                          label={ t('none') }
                          checked={ workflowNoneChecked(config, key, mediaTypes) }
                          onChange={ event => { if (event.target.checked) { setConfig(prev => setWorkflowNone(prev!, key)); } } } />
                      { mediaTypes.map(type => (
                        <CheckBox
                            key={ `${key}-${type}` }
                            label={ t('all-via', { medium: mediumName(type) }) }
                            checked={ workflowMediumEnabled(config, key, type) }
                            onChange={ event => setConfig(prev => setWorkflowMedium(prev!, key, type, event.target.checked, mediaTypes)) } />
                      )) }
                    </Box>);
                })
              }
            </Box>
            :
          <Table>
            <TableHeader>
              <TableRow>
                <TableCell scope="col" border="bottom">{ t('workflow') }</TableCell>
                <TableCell scope="col" border="bottom">{ t('none') }</TableCell>
                { mediaTypes.map(type => (
                  <TableCell key={ `head-${type}` } scope="col" border="bottom">{ t('all-via', { medium: mediumName(type) }) }</TableCell>
                )) }
              </TableRow>
            </TableHeader>
            <TableBody>
              {
                workflows.map(workflow => {
                  const key = workflowKey(workflow);
                  return (
                    <TableRow key={ key }>
                      <TableCell scope="row"><Text>{ translate(workflow.workflowTitle, currentLanguage) || key }</Text></TableCell>
                      <TableCell>
                        <CheckBox
                            checked={ workflowNoneChecked(config, key, mediaTypes) }
                            onChange={ event => { if (event.target.checked) { setConfig(prev => setWorkflowNone(prev!, key)); } } } />
                      </TableCell>
                      { mediaTypes.map(type => (
                        <TableCell key={ `${key}-${type}` }>
                          <CheckBox
                              checked={ workflowMediumEnabled(config, key, type) }
                              onChange={ event => setConfig(prev => setWorkflowMedium(prev!, key, type, event.target.checked, mediaTypes)) } />
                        </TableCell>
                      )) }
                    </TableRow>);
                })
              }
            </TableBody>
          </Table>
          }
        </Box>
      }      <Box gap="small">
        <Heading level="4" margin={ { vertical: 'small' } }>{ t('recipient') }</Heading>
        {
          recipient.map(mediumConfig => (
            <Box key={ `recipient-${mediumConfig.medium}` } gap="xsmall">
              <Text weight="bold">{ translate(mediumConfig.name, currentLanguage) || mediumConfig.medium }</Text>
              {
                (mediumConfig.values ?? []).map(value => (
                  <Box key={ `${mediumConfig.medium}-${value.type}` } direction="row" gap="small" align="center">
                    <Tip content={ translate(value.description, currentLanguage) }>
                      <Text>{ translate(value.title, currentLanguage) || value.type }</Text>
                    </Tip>
                    <TextInput
                        value={ recipientValues[mediumConfig.medium]?.[value.type] ?? '' }
                        onChange={ event => {
                          const newValue = event.target.value;
                          setRecipientValues(prev => ({
                            ...prev,
                            [mediumConfig.medium]: { ...prev[mediumConfig.medium], [value.type]: newValue },
                          }));
                        } } />
                  </Box>
                ))
              }
            </Box>
          ))
        }
      </Box>

      <Box direction="row">
        <Button primary label={ t('save') } onClick={ save } />
      </Box>

    </Box>);

};

export default Notifications;
