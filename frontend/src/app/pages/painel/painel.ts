import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService, Dashboard, QueueItem, Reminder, ReminderStatus, errorText } from '../../core/api';
import { DONE_LABEL, STATUS_LABEL, TYPE_BY_VALUE, fmtDate, fmtTime } from '../../core/labels';

@Component({
  selector: 'app-painel',
  imports: [RouterLink],
  templateUrl: './painel.html',
  styleUrl: './painel.css',
})
export class PainelPage implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly dash = signal<Dashboard | null>(null);
  protected readonly queue = signal<QueueItem[]>([]);
  protected readonly opened = signal<Set<number>>(new Set());
  protected readonly error = signal('');
  protected readonly loading = signal(true);
  protected readonly isDesktop = typeof matchMedia === 'function' && matchMedia('(pointer: fine)').matches;

  protected readonly typeOf = TYPE_BY_VALUE;
  protected readonly statusLabel = STATUS_LABEL;
  protected readonly doneLabel = DONE_LABEL;
  protected readonly fmtTime = fmtTime;
  protected readonly fmtDate = fmtDate;

  constructor() {
    const timer = setInterval(() => this.load(), 60_000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.dashboard().subscribe({
      next: (d) => {
        this.dash.set(d);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(errorText(e));
        this.loading.set(false);
      },
    });
    this.api.queue().subscribe({ next: (q) => this.queue.set(q), error: (e) => this.error.set(errorText(e)) });
  }

  /** O atendente abriu o WhatsApp: destaca o botão "Enviei". */
  open(item: QueueItem): void {
    this.opened.update((s) => new Set(s).add(item.id));
  }

  sent(item: QueueItem): void {
    this.api.markSent(item.id).subscribe({ next: () => this.drop(item), error: (e) => this.error.set(errorText(e)) });
  }

  skip(item: QueueItem): void {
    this.api.skip(item.id).subscribe({ next: () => this.drop(item), error: (e) => this.error.set(errorText(e)) });
  }

  setStatus(r: Reminder, status: ReminderStatus): void {
    this.api.setStatus(r.id, status).subscribe({ next: () => this.load(), error: (e) => this.error.set(errorText(e)) });
  }

  pct(v: number): string {
    return `${Math.round(v * 100)}%`;
  }

  private drop(item: QueueItem): void {
    this.queue.update((q) => q.filter((i) => i.id !== item.id));
    this.dash.update((d) => (d ? { ...d, readyNow: Math.max(0, d.readyNow - 1), sentToday: d.sentToday + 1 } : d));
  }
}
