import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, Customer, errorText } from '../../core/api';

@Component({
  selector: 'app-clientes',
  imports: [FormsModule],
  template: `
    <header class="page-head">
      <div>
        <h1>Clientes</h1>
        <p>Só recebe avisos quem autorizou (LGPD). O telefone vira link do WhatsApp automaticamente.</p>
      </div>
    </header>

    @if (error()) {
      <p class="alert" role="alert">{{ error() }}</p>
    }

    <div class="layout">
      <form class="card form" (ngSubmit)="save()" #f="ngForm">
        <h2>{{ editingId() ? 'Editar cliente' : 'Novo cliente' }}</h2>
        <label>Nome <input name="name" [(ngModel)]="name" required maxlength="120" /></label>
        <label>WhatsApp (com DDD) <input name="phone" [(ngModel)]="phone" required inputmode="tel" placeholder="(43) 99999-1234" /></label>
        <label class="check"><input type="checkbox" name="consent" [(ngModel)]="consent" /> Autorizou receber avisos pelo WhatsApp</label>
        <label>Observações <textarea name="notes" [(ngModel)]="notes" rows="2" maxlength="500"></textarea></label>
        <div class="buttons">
          <button class="btn btn-primary" type="submit" [disabled]="f.invalid">Salvar</button>
          @if (editingId()) {
            <button class="btn btn-ghost" type="button" (click)="reset()">Cancelar</button>
          }
        </div>
      </form>

      <section class="card">
        <label class="search"><span class="visually-hidden">Buscar</span>
          <input type="search" placeholder="Buscar por nome ou telefone" [ngModel]="q()" (ngModelChange)="search($event)" />
        </label>
        <ul class="list">
          @for (c of list(); track c.id) {
            <li>
              <span class="avatar" aria-hidden="true">{{ c.name.charAt(0) }}</span>
              <span class="text">
                <strong>{{ c.name }}</strong>
                <span class="hint">{{ c.phonePretty }}</span>
              </span>
              <span class="chip" [class.ok]="c.consent" [class.bad]="!c.consent">{{ c.consent ? 'Autorizou' : 'Sem autorização' }}</span>
              <button type="button" class="btn btn-ghost btn-sm" (click)="edit(c)">Editar</button>
            </li>
          } @empty {
            <li class="empty">Nenhum cliente encontrado.</li>
          }
        </ul>
      </section>
    </div>
  `,
  styles: `
    .layout { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1.5fr); gap: 1.25rem; align-items: start; }
    @media (max-width: 900px) { .layout { grid-template-columns: 1fr; } }
    .form { display: grid; gap: 1rem; }
    .buttons { display: flex; gap: 0.5rem; }
    .search { margin-bottom: 0.5rem; }
    .list { margin: 0; padding: 0; list-style: none; }
    .list li { display: grid; grid-template-columns: 2.4rem minmax(0, 1fr) auto auto; align-items: center; gap: 0.7rem; padding: 0.65rem 0; border-bottom: 1px solid var(--line); }
    .list li:last-child { border-bottom: 0; }
    .avatar { display: grid; place-items: center; width: 2.4rem; height: 2.4rem; border-radius: 50%; background: linear-gradient(135deg, var(--sky-400), var(--blue-600)); color: #fff; font-weight: 700; }
    .text { display: grid; min-width: 0; }
    .text strong { color: var(--blue-950); }
    @media (max-width: 520px) { .list li { grid-template-columns: 2.4rem minmax(0, 1fr) auto; } .list .chip { display: none; } }
  `,
})
export class ClientesPage implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly list = signal<Customer[]>([]);
  protected readonly q = signal('');
  protected readonly error = signal('');
  protected readonly editingId = signal<number | null>(null);
  protected name = '';
  protected phone = '';
  protected consent = true;
  protected notes = '';
  private timer: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    this.load();
  }

  search(q: string): void {
    this.q.set(q);
    if (this.timer) clearTimeout(this.timer);
    this.timer = setTimeout(() => this.load(), 250);
  }

  edit(c: Customer): void {
    this.editingId.set(c.id);
    this.name = c.name;
    this.phone = c.phonePretty;
    this.consent = c.consent;
    this.notes = c.notes ?? '';
  }

  reset(): void {
    this.editingId.set(null);
    this.name = '';
    this.phone = '';
    this.consent = true;
    this.notes = '';
  }

  save(): void {
    this.error.set('');
    this.api
      .saveCustomer({ name: this.name, phone: this.phone, consent: this.consent, notes: this.notes || null }, this.editingId() ?? undefined)
      .subscribe({
        next: () => {
          this.reset();
          this.load();
        },
        error: (e) => this.error.set(errorText(e)),
      });
  }

  private load(): void {
    this.api.customers(this.q()).subscribe({ next: (c) => this.list.set(c), error: (e) => this.error.set(errorText(e)) });
  }
}
