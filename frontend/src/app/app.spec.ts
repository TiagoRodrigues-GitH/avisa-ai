import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { App } from './app';
import { TYPES, fmtMoney, toLocalInput } from './core/labels';

describe('App', () => {
  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient()],
    }).compileComponents();
  });

  it('shows only the page (no navigation) before login', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.sidebar')).toBeNull();
  });

  it('shows the blue navigation after login', async () => {
    sessionStorage.setItem('avisaai:acesso', btoa('admin:x'));
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const links = [...fixture.nativeElement.querySelectorAll('.nav-link')].map((a: Element) => a.textContent?.trim());
    expect(links).toEqual(['Painel', 'Compromissos', 'Entregas', 'Clientes', 'Mensagens']);
  });
});

describe('labels', () => {
  it('covers every reminder type with an icon and a hint', () => {
    expect(TYPES.length).toBe(8);
    for (const t of TYPES) {
      expect(t.icon.length).toBeGreaterThan(0);
      expect(t.hint.length).toBeGreaterThan(10);
    }
  });

  it('formats money and datetime-local values', () => {
    expect(fmtMoney(150).replace(/\s/g, ' ')).toBe('R$ 150,00');
    expect(fmtMoney(null)).toBe('');
    expect(toLocalInput(new Date(2026, 10, 20, 9, 5))).toBe('2026-11-20T09:05');
  });
});
