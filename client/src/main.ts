import { bootstrapApplication } from '@angular/platform-browser';
import { ModuleRegistry } from 'ag-grid-community';
import { AllEnterpriseModule } from 'ag-grid-enterprise';
import { appConfig } from './app/app.config';
import { App } from './app/app';

ModuleRegistry.registerModules([AllEnterpriseModule]);

bootstrapApplication(App, appConfig).catch((err) => console.error(err));
