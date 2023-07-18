import { Route, Routes } from "react-router-dom";
import { ListOfWorkflows } from './ListOfWorkflows';
import i18n from '../../i18n';
import React, { useLayoutEffect } from "react";
import { useAppContext } from "../../AppContext";

i18n.addResources('en', 'tasklist', {
      "title.long": 'Workflows',
      "title.short": 'Workflows',
    });
i18n.addResources('de', 'tasklist', {
      "title.long": 'Vorgänge',
      "title.short": 'Vorgänge',
    });

const Main = () => {

  const { setAppHeaderTitle } = useAppContext();
  
  useLayoutEffect(() => {
    setAppHeaderTitle('tasklist', false);
  }, [ setAppHeaderTitle ]);

  return (
    <Routes>
      <Route index element={<ListOfWorkflows />} />
      <Route path="/:workflowId" element={<Main />} />
    </Routes>);
}

export default Main;