import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, Reminder, ReminderType, errorText } from '../../core/api';
import { STATUS_LABEL, TYPES, TYPE_BY_VALUE, fmtDate, fmtMoney, fmtTime } from '../../core/labels';

type Range = 'past' | 'today' | 'week' | 'month';

@Component({
  selector: 'app-agenda',
  imports: [RouterLink, FormsModule],
  template: `
    <header class="page-head">
      <div>
        <h1>Compromissos</h1>
        <p>Tudo o que gera aviso: consultas, vencimentos, retiradas, entregas, retornos e mais.</p>
      </div>
      <a class="btn btn-primary" routerLink="/agenda/novo">+ Novo aviso</a>
    </header>

    <div class="filters card">
      <div class="seg" role="group" aria-label="Período">
        @for (r of ranges; track r.value) {
          <button type="button" class="btn btn-sm" [class.btn-primary]="range() === r.value" [class.btn-ghost]="range() !== r.value"
                  [attr.aria-pressed]="range() === r.value" (click)="setRange(r.value)">{{ r.label }}</button>
        }
      </div>
      <label class="type-filter">
        <span class="visually-hidden">Tipo</span>
        <select [ngModel]="type()" (ngModelChange)="setType($event)">
          <option value="">Todos os tipos</option>
          @for (t of types; track t.value) {
            <option [value]="t.value">{{ t.icon }} {{ t.label }}</option>
          }
        </select>
      </label>
    </div>

    @if (error()) {
      <p class="alert" role="alert">{{ error() }}</p>
    }

    @for (day of days(); track day.label) {
      <section class="day">
        <h2>{{ day.label }}</h2>
        <div class="card list">
          @for (r of day.items; track r.id) {
            <a class="item" [routerLink]="['/agenda', r.id]">
              <time>{{ fmtTime(r.eventAt) }}</time>
              <span class="icon" aria-hidden="true">{{ typeOf[r.type].icon }}</span>
              <span class="text">
                <strong>{{ r.title }}</strong>
                <span class="hint">{{ r.customer.name }} · {{ r.typeLabel }}{{ r.amount != null ? ' · ' + fmtMoney(r.amount) : '' }}</span>
              </span>
              <span class="chip" [class.ok]="r.status === 'CONFIRMED' || r.status === 'DONE'" [class.warn]="r.status === 'RESCHEDULE_REQUESTED'" [class.bad]="r.status === 'NO_SHOW' || r.status === 'CANCELED'">
                {{ statusLabel[r.status] }}
              </span>
            </a>
          }
        </div>
      </section>
    } @empty {
      <p class="empty">{{ loading() ? 'Carregando…' : 'Nenhum compromisso neste período.' }}</p>
    }
  `,
  styles: `
    .filters { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 0.75rem; padding: 0.75rem; margin-bottom: 1.25rem; }
    .seg { display: flex; flex-wrap: wrap; gap: 0.3rem; }
    .type-filter { min-width: 14rem; }
    .day + .day { margin-top: 1.25rem; }
    .day h2 { margin-bottom: 0.5rem; font-size: 0.95rem; color: var(--blue-800); text-transform: capitalize; }
    .list { padding: 0.25rem 0.75rem; }
    .item {
      display: grid; grid-template-columns: 3.3rem 2.2rem minmax(0, 1fr) auto; align-items: center; gap: 0.6rem;
      padding: 0.75rem 0.25rem; border-bottom: 1px solid var(--line); color: inherit; text-decoration: none;
    }
    .item:last-child { border-bottom: 0; }
    .item:hover { background: var(--blue-50); }
    time { font-weight: 700; color: var(--blue-700); font-variant-numeric: tabular-nums; }
    .icon { font-size: 1.2rem; text-align: center; }
    .text { display: grid; min-width: 0; }
    .text strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--blue-950); }
    @media (max-width: 560px) { .item { grid-template-columns: 3rem minmax(0, 1fr) auto; } .icon { display: none; } }
  `,
})
export class AgendaPage implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly types = TYPES;
  protected readonly typeOf = TYPE_BY_VALUE;
  protected readonly statusLabel = STATUS_LABEL;
  protected readonly fmtTime = fmtTime;
  protected readonly fmtMoney = fmtMoney;
  protected readonly ranges: { value: Range; label: string }[] = [
    { value: 'past', label: 'Últimos 30 dias' },
    { value: 'today', label: 'Hoje' },
    { value: 'week', label: '7 dias' },
    { value: 'month', label: '30 dias' },
  ];
  protected readonly range = signal<Range>('week');
  protected readonly type = signal<ReminderType | ''>('');
  protected readonly items = signal<Reminder[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal('');

  protected readonly days = computed(() => {
    const groups = new Map<string, Reminder[]>();
    const list = this.range() === 'past' ? [...this.items()].reverse() : this.items();
    for (const r of list) {
      const key = fmtDate(r.eventAt);
      groups.set(key, [...(groups.get(key) ?? []), r]);
    }
    return [...groups.entries()].map(([label, items]) => ({ label, items }));
  });

  ngOnInit(): void {
    this.load();
  }

  setRange(r: Range): void {
    this.range.set(r);
    this.load();
  }

  setType(t: ReminderType | ''): void {
    this.type.set(t);
    this.load();
  }

  private load(): void {
    const start = new Date();
    start.setHours(0, 0, 0, 0);
    const day = 86_400_000;
    const [from, to] = {
      past: [new Date(start.getTime() - 30 * day), new Date()],
      today: [start, new Date(start.getTime() + day)],
      week: [start, new Date(start.getTime() + 7 * day)],
      month: [start, new Date(start.getTime() + 30 * day)],
    }[this.range()];
    this.loading.set(true);
    this.api.reminders(from, to, this.type()).subscribe({
      next: (list) => {
        this.items.set(list);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(errorText(e));
        this.loading.set(false);
      },
    });
  }
}
