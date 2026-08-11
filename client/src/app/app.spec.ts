import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter(routes), provideHttpClient()],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the product name and primary nav', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('DCIM');
    expect(text).toContain('Data Center');
    expect(text).toContain('Connectivity');
    expect(text).toContain('Change Spec');
    expect(text).toContain('Firm');
    expect(text).toContain('Billing');
  });

  it('should show Change Spec subnav on the change-spec route', async () => {
    const fixture = TestBed.createComponent(App);
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/change-spec');
    await fixture.whenStable();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Change Specs');
    expect(text).toContain('Changelog');
  });
});
