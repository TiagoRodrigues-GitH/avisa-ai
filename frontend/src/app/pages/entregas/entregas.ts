import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, DeliveryStage, QueueItem, Reminder, errorText } from '../../core/api';
import { STAGES, fmtMoney, fmtTime } from '../../core/labels';

@Component({
  selector: 'app-entregas',
  imports: [FormsModule, RouterLink],
  template: `
    <header class="page-head">
      <div>
        <h1>Entregas</h1>
        <p>Avance a etapa e o aviso fica pronto: “saiu para entrega”, “a caminho” e “entregue”.</p>
      </div>
      <a class="btn btn-primary" routerLink="/agenda/novo">+ Nova entrega</a>
    </header>

    @if (error()) {
      <p class="alert" role="alert">{{ error() }}</p>
    }

    <div class="board">
      @for (col of columns(); track col.stage.value) {
        <section class="col" [attr.aria-label]="col.stage.label">
          <h2><span aria-hidden="true">{{ col.stage.icon }}</span> {{ col.stage.label }} <span class="chip">{{ col.items.length }}</span></h2>
          @for (r of col.items; track r.id) {
            <article class="card ticket">
              <header>
                <strong>Pedido {{ r.title }}</strong>
                <span class="hint">{{ fmtTime(r.eventAt) }}{{ r.amount != null ? ' · ' + fmtMoney(r.amount) : '' }}</span>
              </header>
              <span>{{ r.customer.name }}</span>
              <span class="hint">{{ r.customer.phonePretty }}</span>

              @for (q of pending(r.id); track q.id) {
                <div class="ready">
                  <span class="hint">Aviso pronto: {{ q.kindLabel }}</span>
                  <div class="row">
                    <a class="btn btn-wa btn-sm" [href]="q.chatLink" target="_blank" rel="noopener">Abrir no WhatsApp</a>
                    <button type="button" class="btn btn-outline btn-sm" (click)="sent(q)">✓ Enviei</button>
                  </div>
                </div>
              }

              @if (next(r); as n) {
                @if (n.value === 'NEARBY') {
                  <label class="eta">Chega em (min)
                    <input type="number" min="1" max="600" [(ngModel)]="eta[r.id]" [ngModelOptions]="{ standalone: true }" />
                  </label>
                }
                <button type="button" class="btn btn-primary btn-sm" (click)="advance(r, n.value)">{{ n.icon }} {{ n.label }}</button>
              }
            </article>
          } @empty {
            <p class="empty small">Nenhuma</p>
          }
        </section>
      }
    </div>
  `,
  styles: `
    .board { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 1rem; align-items: start; }
    @media (max-width: 900px) { .board { grid-template-columns: 1fr; } }
    .col { display: grid; gap: 0.75rem; padding: 0.75rem; border-radius: var(--radius); background: var(--blue-100); }
    .col h2 { display: flex; align-items: center; gap: 0.4rem; font-size: 1rem; color: var(--blue-900); }
    .ticket { display: grid; gap: 0.35rem; padding: 0.9rem; }
    .ticket header { display: flex; justify-content: space-between; gap: 0.5rem; color: var(--blue-950); }
    .ready { display: grid; gap: 0.35rem; margin: 0.35rem 0; padding: 0.6rem; border-radius: 0.7rem; background: var(--blue-50); }
    .row { display: flex; flex-wrap: wrap; gap: 0.35rem; }
    .eta { max-width: 9rem; }
    .eta input { min-height: 2.2rem; }
    .small { padding: 1rem; background: rgb(255 255 255 / 55%); }
    .ticket > .btn { justify-self: start; margin-top: 0.35rem; }
  `,
})
export class EntregasPage implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly fmtTime = fmtTime;
  protected readonly fmtMoney = fmtMoney;
  protected readonly deliveries = signal<Reminder[]>([]);
  protected readonly queue = signal<QueueItem[]>([]);
  protected readonly error = signal('');
  protected eta: Record<number, number> = {};

  /** Três colunas visíveis: preparando, saiu, a caminho ("entregue" sai do quadro). */
  protected readonly columns = computed(() =>
    STAGES.filter((s) => s.value !== 'DELIVERED').map((stage) => ({
      stage,
      items: this.deliveries().filter((d) => (d.deliveryStage ?? 'PREPARING') === stage.value),
    })),
  );

  ngOnInit(): void {
    this.load();
  }

  next(r: Reminder): (typeof STAGES)[number] | undefined {
    const i = STAGES.findIndex((s) => s.value === (r.deliveryStage ?? 'PREPARING'));
    return STAGES[i + 1];
  }

  pending(reminderId: number): QueueItem[] {
    return this.queue().filter((q) => q.reminder.id === reminderId);
  }

  advance(r: Reminder, stage: DeliveryStage): void {
    this.api.advanceDelivery(r.id, stage, stage === 'NEARBY' ? this.eta[r.id] ?? 15 : null).subscribe({
      next: () => this.load(),
      error: (e) => this.error.set(errorText(e)),
    });
  }

  sent(q: QueueItem): void {
    this.api.markSent(q.id).subscribe({
      next: () => this.queue.update((list) => list.filter((i) => i.id !== q.id)),
      error: (e) => this.error.set(errorText(e)),
    });
  }

  private load(): void {
    this.api.deliveries().subscribe({ next: (d) => this.deliveries.set(d), error: (e) => this.error.set(errorText(e)) });
    this.api.queue().subscribe({ next: (q) => this.queue.set(q.filter((i) => i.reminder.type === 'DELIVERY')) });
  }
}
