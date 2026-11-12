import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth';
import { errorText } from '../../core/api';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <div class="login-bg">
      <form class="login card" (ngSubmit)="submit()" #f="ngForm">
        <div class="logo" aria-hidden="true">
          <svg viewBox="0 0 24 24"><path d="M12 22a2.5 2.5 0 0 0 2.45-2h-4.9A2.5 2.5 0 0 0 12 22zm7-6V11a7 7 0 0 0-5-6.71V3.5a2 2 0 0 0-4 0v.79A7 7 0 0 0 5 11v5l-2 2v1h18v-1l-2-2z"/></svg>
        </div>
        <h1>Avisa <span>Aí</span></h1>
        <p class="tag">Lembretes pelo WhatsApp que reduzem faltas e atrasos — sem custo por mensagem.</p>

        <label>Usuário
          <input name="username" [(ngModel)]="username" required autocomplete="username" />
        </label>
        <label>Senha
          <input name="password" type="password" [(ngModel)]="password" required autocomplete="current-password" />
        </label>
        <details>
          <summary>Servidor</summary>
          <label>Endereço da API
            <input name="server" [(ngModel)]="server" inputmode="url" placeholder="(este mesmo endereço)" />
            <span class="hint">Deixe em branco quando o app é aberto pelo próprio servidor (ex.: http://192.168.0.10:8080 no celular).</span>
          </label>
        </details>
        @if (error()) {
          <p class="alert" role="alert">{{ error() }}</p>
        }
        <button class="btn btn-primary" type="submit" [disabled]="busy() || f.invalid">
          {{ busy() ? 'Entrando…' : 'Entrar' }}
        </button>
      </form>
    </div>
  `,
  styles: `
    .login-bg {
      display: grid;
      place-items: center;
      min-height: 100dvh;
      padding: 1rem;
      background:
        radial-gradient(circle at 15% 15%, rgb(56 189 248 / 35%), transparent 40%),
        radial-gradient(circle at 85% 90%, rgb(37 99 235 / 45%), transparent 45%),
        linear-gradient(160deg, var(--blue-950), var(--blue-800));
    }
    .login { display: grid; gap: 1rem; width: min(25rem, 100%); padding: 2rem; }
    .logo {
      display: grid; place-items: center; width: 3.2rem; height: 3.2rem; border-radius: 1rem;
      background: linear-gradient(135deg, var(--sky-400), var(--blue-600));
      box-shadow: 0 8px 22px rgb(37 99 235 / 40%);
    }
    .logo svg { width: 1.7rem; fill: #fff; }
    h1 { font-size: 2rem; color: var(--blue-950); }
    h1 span { color: var(--blue-600); }
    .tag { margin: -0.5rem 0 0.25rem; color: var(--muted); }
    details summary { cursor: pointer; color: var(--blue-700); font-weight: 600; margin-bottom: 0.5rem; }
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected username = '';
  protected password = '';
  protected server = this.auth.server();
  protected readonly busy = signal(false);
  protected readonly error = signal('');

  submit(): void {
    this.busy.set(true);
    this.error.set('');
    this.auth.login(this.server, this.username.trim(), this.password).subscribe({
      next: (ok) => {
        this.busy.set(false);
        if (ok) this.router.navigate(['/']);
        else this.error.set('Usuário ou senha incorretos.');
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorText(e));
      },
    });
  }
}
