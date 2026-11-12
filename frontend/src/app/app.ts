import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly nav = [
    { path: '/', label: 'Painel', icon: 'M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z', exact: true },
    { path: '/agenda', label: 'Compromissos', icon: 'M19 4h-1V2h-2v2H8V2H6v2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2zm0 16H5V9h14v11z', exact: false },
    { path: '/entregas', label: 'Entregas', icon: 'M20 8h-3V4H3a2 2 0 0 0-2 2v11h2a3 3 0 0 0 6 0h6a3 3 0 0 0 6 0h2v-5l-3-4zM6 18.5A1.5 1.5 0 1 1 6 15.5a1.5 1.5 0 0 1 0 3zm13.5-9 1.96 2.5H17V9.5h2.5zM18 18.5a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3z', exact: false },
    { path: '/clientes', label: 'Clientes', icon: 'M16 11a4 4 0 1 0-3.999-4A4 4 0 0 0 16 11zm-8 0a3 3 0 1 0-3-3 3 3 0 0 0 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h7v-2.5c0-.85.33-2.34 2.37-3.47A12.4 12.4 0 0 0 8 13zm8 0c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z', exact: false },
    { path: '/mensagens', label: 'Mensagens', icon: 'M20 2H4a2 2 0 0 0-2 2v18l4-4h14a2 2 0 0 0 2-2V4a2 2 0 0 0-2-2zM6 9h12v2H6V9zm8 5H6v-2h8v2zm4-6H6V6h12v2z', exact: false },
  ];

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
