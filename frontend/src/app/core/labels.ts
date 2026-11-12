import { DeliveryStage, ReminderStatus, ReminderType } from './api';

/** Tipos de aviso, na ordem em que aparecem nos formulários, com ícone e dica de uso. */
export const TYPES: { value: ReminderType; label: string; icon: string; hint: string; needsAmount?: boolean }[] = [
  { value: 'APPOINTMENT', label: 'Consulta / horário', icon: '🗓️', hint: 'Pede confirmação 24 h antes e lembra 2 h antes.' },
  { value: 'PAYMENT', label: 'Pagamento', icon: '💳', hint: 'Avisa 3 dias antes, no dia e no dia seguinte ao vencimento.', needsAmount: true },
  { value: 'PICKUP', label: 'Retirada', icon: '📦', hint: 'Avisa que está pronto e lembra de novo em 2 dias.' },
  { value: 'DELIVERY', label: 'Entrega', icon: '🛵', hint: 'Avisa quando sai, quando está a caminho e quando foi entregue.' },
  { value: 'RETURN_VISIT', label: 'Retorno / revisão', icon: '🔁', hint: 'Lembra 7 dias antes de vencer o retorno ou a revisão.' },
  { value: 'WAITLIST', label: 'Vaga liberada', icon: '✨', hint: 'Oferece na hora um horário que abriu.' },
  { value: 'QUOTE', label: 'Orçamento', icon: '🧾', hint: 'Envia o orçamento e acompanha em 3 dias.', needsAmount: true },
  { value: 'FEEDBACK', label: 'Pesquisa de satisfação', icon: '⭐', hint: 'Pergunta a nota de 0 a 10, 2 h depois do atendimento.' },
];

export const TYPE_BY_VALUE = Object.fromEntries(TYPES.map((t) => [t.value, t])) as Record<ReminderType, (typeof TYPES)[number]>;

export const STATUS_LABEL: Record<ReminderStatus, string> = {
  SCHEDULED: 'Agendado',
  CONFIRMED: 'Confirmado',
  RESCHEDULE_REQUESTED: 'Remarcar',
  CANCELED: 'Cancelado',
  DONE: 'Concluído',
  NO_SHOW: 'Faltou',
};

/** Texto do botão "concluir" conforme o tipo. */
export const DONE_LABEL: Record<ReminderType, string> = {
  APPOINTMENT: 'Compareceu',
  PAYMENT: 'Pago',
  PICKUP: 'Retirado',
  DELIVERY: 'Entregue',
  RETURN_VISIT: 'Agendou',
  WAITLIST: 'Aceitou',
  QUOTE: 'Aprovado',
  FEEDBACK: 'Respondeu',
};

export const STAGES: { value: DeliveryStage; label: string; icon: string }[] = [
  { value: 'PREPARING', label: 'Preparando', icon: '🧑‍🍳' },
  { value: 'OUT_FOR_DELIVERY', label: 'Saiu para entrega', icon: '🛵' },
  { value: 'NEARBY', label: 'A caminho / chegando', icon: '📍' },
  { value: 'DELIVERED', label: 'Entregue', icon: '✅' },
];

const DATE = new Intl.DateTimeFormat('pt-BR', { weekday: 'short', day: '2-digit', month: '2-digit' });
const TIME = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' });
const MONEY = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export const fmtDate = (iso: string) => DATE.format(new Date(iso));
export const fmtTime = (iso: string) => TIME.format(new Date(iso));
export const fmtMoney = (v: number | null) => (v == null ? '' : MONEY.format(v));

/** Valor para <input type="datetime-local"> a partir de uma data. */
export function toLocalInput(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
