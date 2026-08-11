import { Routes } from '@angular/router';
import { BillingPage } from './pages/billing/billing';
import { ChangeSpecPage } from './pages/change-spec/change-spec';
import { ChangelogPage } from './pages/change-spec/changelog/changelog';
import { ChangeSpecsListPage } from './pages/change-spec/change-specs-list/change-specs-list';
import { ConnectivityPage } from './pages/connectivity/connectivity';
import { DataCenterPage } from './pages/data-center/data-center';
import { FirmPage } from './pages/firm/firm';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'data-center' },
  { path: 'data-center', component: DataCenterPage },
  { path: 'connectivity', component: ConnectivityPage },
  {
    path: 'change-spec',
    component: ChangeSpecPage,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'specs' },
      { path: 'specs', component: ChangeSpecsListPage },
      { path: 'changelog', component: ChangelogPage },
    ],
  },
  { path: 'firm', component: FirmPage },
  { path: 'billing', component: BillingPage },
  { path: '**', redirectTo: 'data-center' },
];
