import { Route, Routes } from "react-router-dom";
import { ListOfTasks } from './ListOfTasks';
import i18n from '../i18n';
import React, { useLayoutEffect } from "react";
import { useAppContext } from "../AppContext";

i18n.addResources('en', 'tasklist', {
      "title.long": 'Tasks',
      "title.short": 'Tasks',
    });
i18n.addResources('de', 'tasklist', {
      "title.long": 'Aufgaben',
      "title.short": 'Aufgaben',
    });

const Main = () => {

  const { setAppHeaderTitle } = useAppContext();
  
  useLayoutEffect(() => {
    setAppHeaderTitle('tasklist', false);
  }, [ setAppHeaderTitle ]);

  return (
    <Routes>
      <Route path='/' element={<ListOfTasks />} />
    </Routes>);
}

export default Main;
