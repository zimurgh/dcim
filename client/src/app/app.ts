import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

type NavItem = {
  label: string;
  path: string;
};

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly navItems: NavItem[] = [
    { label: 'Data Center', path: '/data-center' },
    { label: 'Connectivity', path: '/connectivity' },
    { label: 'Change Spec', path: '/change-spec' },
    { label: 'Firm', path: '/firm' },
    { label: 'Billing', path: '/billing' },
  ];
}
