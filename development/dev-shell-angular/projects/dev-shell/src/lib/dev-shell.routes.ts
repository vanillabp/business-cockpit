import { Routes } from '@angular/router';
import { UserTaskComponent } from "./user-task/user-task.component";
import { MainComponent } from "./main/main.component";
import { userTaskResolver } from './user-task.resolver';
import { Type } from "@angular/core";
import { BcUserTask, BcWorkflow } from "@vanillabp/bc-shared";
import { UserTaskWrapperComponent } from "./user-task/user-task-wrapper.component";
import { WorkflowPageComponent } from "./workflow-page/workflow-page.component";
import { workflowResolver } from "./workflow.resolver";
import { WorkflowPageWrapperComponent } from "./workflow-page/workflow-page-wrapper.component";

type RouteConfigFunction = (userTaskForm: Type<{ userTask?: BcUserTask }>, workFlowPage: Type<{ workflow?: BcWorkflow }>) => Routes;

export const routes: RouteConfigFunction = (userTaskForm, workFlowPage) => {
  return [
    {
      path: "",
      component: MainComponent,
    },
    {path: "task", component: UserTaskComponent},
    {
      path: "task/:userTaskId",
      component: UserTaskComponent,
      children: [
        {
          path: "",
          resolve: { userTask: userTaskResolver },
          component: UserTaskWrapperComponent,
          children: [
            {
              path: "",
              component: userTaskForm
            }
          ]
        }
      ]
    },
    {path: "workflow", component: WorkflowPageComponent},
    {
      path: "workflow/:workflowId",
      component: WorkflowPageComponent,
      children: [
        {
          path: "",
          resolve: { workflow: workflowResolver },
          component: WorkflowPageWrapperComponent,
          children: [
            {
              path: "",
              component: workFlowPage
            }
          ]
        }
      ]
    },
  ] as Routes;
}
