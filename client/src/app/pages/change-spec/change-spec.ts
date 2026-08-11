import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

type SubNavItem = {
  label: string;
  path: string;
};

@Component({
  selector: 'app-change-spec-page',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './change-spec.html',
})
export class ChangeSpecPage {
  protected readonly subNavItems: SubNavItem[] = [
    { label: 'Change Specs', path: 'specs' },
    { label: 'Changelog', path: 'changelog' },
  ];
}
