import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { CanActivateFn, Router } from '@angular/router';
import { Observable, catchError, map, of, throwError } from 'rxjs';

const SERVER_KEY = 'avisaai:servidor';
const TOKEN_KEY = 'avisaai:acesso';
/** Vazio = mesmo endereço do app (o backend serve o app, ou o proxy do ng serve encaminha /api). */
const DEFAULT_SERVER = '';

function read(storage: () => Storage, key: string): string | null {
  try {
    return storage().getItem(key);
  } catch {
    return null;
  }
}

/**
 * Login com usuário e senha (HTTP Basic). As credenciais ficam só na aba (sessionStorage) e somem ao fechar;
 * o endereço do servidor fica salvo (localStorage) para usar o app no celular apontando para o PC.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  readonly server = signal(read(() => localStorage, SERVER_KEY) ?? DEFAULT_SERVER);
  readonly token = signal(read(() => sessionStorage, TOKEN_KEY));
  readonly user = signal(this.token() ? atob(this.token()!).split(':')[0] : '');

  login(server: string, username: string, password: string): Observable<boolean> {
    const clean = server.trim().replace(/\/+$/, '') || DEFAULT_SERVER;
    const token = btoa(unescape(encodeURIComponent(`${username}:${password}`)));
    return this.http
      .get(`${clean}/api/settings`, { headers: { Authorization: `Basic ${token}` } })
      .pipe(
        map(() => {
          this.server.set(clean);
          this.token.set(token);
          this.user.set(username);
          try {
            localStorage.setItem(SERVER_KEY, clean);
            sessionStorage.setItem(TOKEN_KEY, token);
          } catch {
            /* armazenamento bloqueado: vale só enquanto a página estiver aberta */
          }
          return true;
        }),
        catchError((e: HttpErrorResponse) => (e.status === 401 ? of(false) : throwError(() => e))),
      );
  }

  logout(): void {
    this.token.set(null);
    this.user.set('');
    try {
      sessionStorage.removeItem(TOKEN_KEY);
    } catch {
      /* nada a limpar */
    }
  }
}

/** Acrescenta o cabeçalho de acesso nas chamadas à API e manda para o login quando ele expira. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token();
  const isApi = req.url.startsWith(auth.server());
  const request = token && isApi && !req.headers.has('Authorization')
    ? req.clone({ setHeaders: { Authorization: `Basic ${token}` } })
    : req;
  return next(request).pipe(
    catchError((e: HttpErrorResponse) => {
      if (e.status === 401 && isApi && !req.headers.has('Authorization')) {
        auth.logout();
        router.navigate(['/login']);
      }
      return throwError(() => e);
    }),
  );
};

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.token() ? true : inject(Router).createUrlTree(['/login']);
};
