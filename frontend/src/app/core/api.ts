import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth';

export type ReminderType =
  | 'APPOINTMENT' | 'PAYMENT' | 'PICKUP' | 'DELIVERY' | 'RETURN_VISIT' | 'WAITLIST' | 'QUOTE' | 'FEEDBACK';
export type ReminderStatus = 'SCHEDULED' | 'CONFIRMED' | 'RESCHEDULE_REQUESTED' | 'CANCELED' | 'DONE' | 'NO_SHOW';
export type DeliveryStage = 'PREPARING' | 'OUT_FOR_DELIVERY' | 'NEARBY' | 'DELIVERED';
export type NotificationStatus = 'PENDING' | 'READY' | 'SENT' | 'SKIPPED' | 'CANCELED';

export interface Customer {
  id: number;
  name: string;
  phone: string;
  phonePretty: string;
  consent: boolean;
  notes: string | null;
}

export interface Reminder {
  id: number;
  customer: Customer;
  type: ReminderType;
  typeLabel: string;
  title: string;
  eventAt: string;
  amount: number | null;
  status: ReminderStatus;
  deliveryStage: DeliveryStage | null;
  etaMinutes: number | null;
  notes: string | null;
}

export interface ReminderInput {
  customerId: number;
  type: ReminderType;
  title: string;
  eventAt: string;
  amount: number | null;
  notes: string | null;
}

export interface QueueItem {
  id: number;
  kind: string;
  kindLabel: string;
  dueAt: string;
  text: string;
  chatLink: string;
  desktopLink: string;
  reminder: Reminder;
}

export interface Message {
  id: number;
  reminderId: number;
  kind: string;
  kindLabel: string;
  dueAt: string;
  status: NotificationStatus;
  text: string | null;
  reason: string | null;
  sentAt: string | null;
}

export interface Dashboard {
  readyNow: number;
  sentToday: number;
  scheduledToday: number;
  confirmedNext7Days: number;
  awaitingConfirmation: number;
  overduePayments: number;
  openDeliveries: number;
  noShows30Days: number;
  confirmationRate30Days: number;
  today: Reminder[];
}

export interface Template {
  kind: string;
  label: string;
  type: ReminderType;
  typeLabel: string;
  text: string;
  custom: boolean;
  preview: string;
}

export interface Settings {
  businessName: string;
  address: string;
  businessPhone: string | null;
}

/** Cliente HTTP da API do Avisa Aí (o endereço do servidor é configurável na tela de login). */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  private url(path: string): string {
    return `${this.auth.server()}${path}`;
  }

  dashboard(): Observable<Dashboard> {
    return this.http.get<Dashboard>(this.url('/api/dashboard'));
  }

  queue(): Observable<QueueItem[]> {
    return this.http.get<QueueItem[]>(this.url('/api/queue'));
  }

  markSent(id: number): Observable<Message> {
    return this.http.post<Message>(this.url(`/api/queue/${id}/sent`), {});
  }

  skip(id: number, reason?: string): Observable<Message> {
    return this.http.post<Message>(this.url(`/api/queue/${id}/skip`), { reason: reason ?? null });
  }

  sendNow(id: number): Observable<Message> {
    return this.http.post<Message>(this.url(`/api/messages/${id}/send-now`), {});
  }

  reminders(from: Date, to: Date, type?: ReminderType | ''): Observable<Reminder[]> {
    const params: Record<string, string> = { from: from.toISOString(), to: to.toISOString() };
    if (type) params['type'] = type;
    return this.http.get<Reminder[]>(this.url('/api/reminders'), { params });
  }

  deliveries(): Observable<Reminder[]> {
    return this.http.get<Reminder[]>(this.url('/api/reminders/deliveries'));
  }

  reminder(id: number): Observable<Reminder> {
    return this.http.get<Reminder>(this.url(`/api/reminders/${id}`));
  }

  messages(id: number): Observable<Message[]> {
    return this.http.get<Message[]>(this.url(`/api/reminders/${id}/messages`));
  }

  createReminder(input: ReminderInput): Observable<Reminder> {
    return this.http.post<Reminder>(this.url('/api/reminders'), input);
  }

  updateReminder(id: number, input: ReminderInput): Observable<Reminder> {
    return this.http.put<Reminder>(this.url(`/api/reminders/${id}`), input);
  }

  setStatus(id: number, status: ReminderStatus): Observable<Reminder> {
    return this.http.patch<Reminder>(this.url(`/api/reminders/${id}/status`), { status });
  }

  advanceDelivery(id: number, stage: DeliveryStage, etaMinutes?: number | null): Observable<Reminder> {
    return this.http.patch<Reminder>(this.url(`/api/reminders/${id}/delivery`), { stage, etaMinutes: etaMinutes ?? null });
  }

  deleteReminder(id: number): Observable<void> {
    return this.http.delete<void>(this.url(`/api/reminders/${id}`));
  }

  customers(q = ''): Observable<Customer[]> {
    return this.http.get<Customer[]>(this.url('/api/customers'), { params: q ? { q } : {} });
  }

  saveCustomer(input: Omit<Customer, 'id' | 'phonePretty'>, id?: number): Observable<Customer> {
    return id
      ? this.http.put<Customer>(this.url(`/api/customers/${id}`), input)
      : this.http.post<Customer>(this.url('/api/customers'), input);
  }

  deleteCustomer(id: number): Observable<void> {
    return this.http.delete<void>(this.url(`/api/customers/${id}`));
  }

  templates(): Observable<Template[]> {
    return this.http.get<Template[]>(this.url('/api/templates'));
  }

  saveTemplate(kind: string, text: string): Observable<Template> {
    return this.http.put<Template>(this.url(`/api/templates/${kind}`), { text });
  }

  resetTemplate(kind: string): Observable<void> {
    return this.http.delete<void>(this.url(`/api/templates/${kind}`));
  }

  settings(): Observable<Settings> {
    return this.http.get<Settings>(this.url('/api/settings'));
  }

  saveSettings(s: Settings): Observable<Settings> {
    return this.http.put<Settings>(this.url('/api/settings'), s);
  }
}

/** Mensagem de erro legível a partir da resposta da API (Problem Details). */
export function errorText(err: unknown): string {
  const e = err as { status?: number; error?: { detail?: string } };
  if (e?.status === 0) return 'Servidor fora do ar ou endereço incorreto.';
  return e?.error?.detail ?? 'Não foi possível concluir. Tente de novo.';
}
