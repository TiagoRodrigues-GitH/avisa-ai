import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService, Customer, Message, Reminder, ReminderStatus, ReminderType, errorText } from '../../core/api';
import { DONE_LABEL, STATUS_LABEL, TYPES, TYPE_BY_VALUE, fmtDate, fmtTime, toLocalInput } from '../../core/labels';

@Component({
  selector: 'app-reminder-form',
  imports: [FormsModule, RouterLink],
  templateUrl: './reminder-form.html',
  styleUrl: './reminder-form.css',
})
export class ReminderFormPage implements OnInit {
  /** Vem da rota /agenda/:id (vazio em /agenda/novo). */
  readonly id = input<string>();

  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  protected readonly types = TYPES;
  protected readonly typeOf = TYPE_BY_VALUE;
  protected readonly statusLabel = STATUS_LABEL;
  protected readonly doneLabel = DONE_LABEL;
  protected readonly fmtDate = fmtDate;
  protected readonly fmtTime = fmtTime;

  protected readonly customers = signal<Customer[]>([]);
  protected readonly reminder = signal<Reminder | null>(null);
  protected readonly messages = signal<Message[]>([]);
  protected readonly error = signal('');
  protected readonly busy = signal(false);
  protected readonly newCustomer = signal(false);

  protected customerId: number | null = null;
  protected type = signal<ReminderType>('APPOINTMENT');
  protected title = '';
  protected eventAt = toLocalInput(new Date(Date.now() + 86_400_000));
  protected amount: number | null = null;
  protected notes = '';
  protected cName = '';
  protected cPhone = '';
  protected cConsent = true;

  protected readonly selectedType = computed(() => TYPE_BY_VALUE[this.type()]);
  protected readonly editing = computed(() => !!this.id());

  ngOnInit(): void {
    this.api.customers().subscribe({ next: (c) => this.customers.set(c), error: (e) => this.error.set(errorText(e)) });
    if (this.id()) this.loadReminder(Number(this.id()));
  }

  placeholder(): string {
    return {
      APPOINTMENT: 'ex.: consulta com a Dra. Ana',
      PAYMENT: 'ex.: a mensalidade de dezembro',
      PICKUP: 'ex.: o seu pedido #1042 / o resultado do exame',
      DELIVERY: 'ex.: #1042',
      RETURN_VISIT: 'ex.: revisão dos 10.000 km',
      WAITLIST: 'ex.: horário extra',
      QUOTE: 'ex.: conserto do notebook',
      FEEDBACK: 'ex.: atendimento de hoje',
    }[this.type()];
  }

  save(): void {
    this.error.set('');
    if (this.newCustomer()) {
      this.busy.set(true);
      this.api.saveCustomer({ name: this.cName, phone: this.cPhone, consent: this.cConsent, notes: null }).subscribe({
        next: (c) => {
          this.customers.update((list) => [...list, c].sort((a, b) => a.name.localeCompare(b.name)));
          this.customerId = c.id;
          this.newCustomer.set(false);
          this.saveReminder();
        },
        error: (e) => this.fail(e),
      });
    } else {
      this.saveReminder();
    }
  }

  setStatus(status: ReminderStatus): void {
    const r = this.reminder();
    if (!r) return;
    this.api.setStatus(r.id, status).subscribe({ next: () => this.loadReminder(r.id), error: (e) => this.fail(e) });
  }

  sendNow(m: Message): void {
    this.api.sendNow(m.id).subscribe({ next: () => this.router.navigate(['/']), error: (e) => this.fail(e) });
  }

  remove(): void {
    const r = this.reminder();
    if (!r || !confirm('Excluir este compromisso e as mensagens dele?')) return;
    this.api.deleteReminder(r.id).subscribe({ next: () => this.router.navigate(['/agenda']), error: (e) => this.fail(e) });
  }

  msgClass(m: Message): string {
    return { PENDING: '', READY: 'warn', SENT: 'ok', SKIPPED: 'bad', CANCELED: 'bad' }[m.status];
  }

  msgStatus(m: Message): string {
    return { PENDING: 'Agendada', READY: 'Pronta para enviar', SENT: 'Enviada', SKIPPED: 'Não enviada', CANCELED: 'Cancelada' }[m.status];
  }

  private saveReminder(): void {
    if (this.customerId == null) {
      this.error.set('Escolha ou cadastre o cliente.');
      return;
    }
    this.busy.set(true);
    const input = {
      customerId: this.customerId,
      type: this.type(),
      title: this.title,
      eventAt: new Date(this.eventAt).toISOString(),
      amount: this.selectedType().needsAmount ? this.amount : null,
      notes: this.notes || null,
    };
    const call = this.id() ? this.api.updateReminder(Number(this.id()), input) : this.api.createReminder(input);
    call.subscribe({
      next: (r) => {
        this.busy.set(false);
        this.router.navigate(['/agenda', r.id]);
        this.loadReminder(r.id);
      },
      error: (e) => this.fail(e),
    });
  }

  private loadReminder(id: number): void {
    this.api.reminder(id).subscribe({
      next: (r) => {
        this.reminder.set(r);
        this.customerId = r.customer.id;
        this.type.set(r.type);
        this.title = r.title;
        this.eventAt = toLocalInput(new Date(r.eventAt));
        this.amount = r.amount;
        this.notes = r.notes ?? '';
      },
      error: (e) => this.fail(e),
    });
    this.api.messages(id).subscribe({ next: (m) => this.messages.set(m) });
  }

  private fail(e: unknown): void {
    this.busy.set(false);
    this.error.set(errorText(e));
  }
}
