import {
  Configuration as GuiConfiguration,
  OfficialTasklistApi,
  OfficialWorkflowlistApi
} from '@vanillabp/bc-official-gui-client';

export class officialGuiClientProvider {

  static officialTasklistApi(basePath: string) {
    return {
      provide: OfficialTasklistApi,
      useFactory: () => {
        const config = new GuiConfiguration({
          basePath
        });
        return new OfficialTasklistApi(config);
      }
    }
  }

  static officialWorkflowlistApi(basePath: string) {
    return {
      provide: OfficialWorkflowlistApi,
      useFactory: () => {
        const config = new GuiConfiguration({
          basePath
        });
        return new OfficialWorkflowlistApi(config);
      }
    }
  }

}
