import {Routes} from '@angular/router';
import {ShellAppComponent} from "./shell-app/shell-app.component";
import {MainComponent} from "./main/main.component";
import {Type} from "@angular/core";
import { userTaskResolver } from './user-task.resolver';
import { workflowResolver } from './workflow.resolver';

type RouteConfigFunction = (userTaskForm: Type<{ userProps: string }>, workFlowPage: Type<{ workflowProps: string }>) => Routes;

export const routes: RouteConfigFunction = (userTaskForm, workFlowPage) => {
  return [
    {
      path: "",
      component: MainComponent,
    },
    {path: "task", component: ShellAppComponent},
    {path: "workflow", component: ShellAppComponent},
    {
      path: "task/:userTaskId",
      component: ShellAppComponent,
      children: [
        {
          path: "",
          resolve: { userTask: userTaskResolver },
          component: userTaskForm,
        }
      ]
    },
    {
      path: "workflow/:workflowId",
      component: ShellAppComponent,
      children: [
        {
          path: "",
          resolve: { workflow: workflowResolver },
          component: workFlowPage,
        }
      ]
    }
  ] as Routes;
}
