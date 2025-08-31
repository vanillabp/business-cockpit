import React, { useEffect, useState } from 'react';
import { buildTimestamp, buildVersion } from '../../UserTaskForm';
import { UserTaskForm } from '@vanillabp/bc-shared';

const TestForm2: UserTaskForm = ({ userTask }) => {
  const [ userDetails, setUserDetails ] = useState();
  useEffect(() => {
    if (userDetails !== undefined) {
      return;
    }
    fetch('/wm/TestModule/api/test-user-info')
        .then((response) => response.json())
        .then((data) => {
          console.log(data);
          setUserDetails(data);
        })
        .catch((err) => {
          console.error(err.message);
        });
  }, [ userDetails ]);

  return (<div>
    TestForm2: '{userTask.title.de}' { buildVersion } from { buildTimestamp.toLocaleString() }
    <br />
    User: { userDetails?.email ?? 'unknown' }</div>);
}

export default TestForm2;
