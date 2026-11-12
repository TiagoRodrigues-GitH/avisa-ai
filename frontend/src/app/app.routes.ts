import { Routes } from '@angular/router';
import { authGuard } from './core/auth';

export const routes: Routes = [
  { path: 'login', title: 'Entrar · Avisa Aí', loadComponent: () => import('./pages/login/login').then((m) => m.LoginPage) },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '', title: 'Painel · Avisa Aí', loadComponent: () => import('./pages/painel/painel').then((m) => m.PainelPage) },
      { path: 'agenda', title: 'Compromissos · Avisa Aí', loadComponent: () => import('./pages/agenda/agenda').then((m) => m.AgendaPage) },
      { path: 'agenda/novo', title: 'Novo aviso · Avisa Aí', loadComponent: () => import('./pages/agenda/reminder-form').then((m) => m.ReminderFormPage) },
      { path: 'agenda/:id', title: 'Aviso · Avisa Aí', loadComponent: () => import('./pages/agenda/reminder-form').then((m) => m.ReminderFormPage) },
      { path: 'entregas', title: 'Entregas · Avisa Aí', loadComponent: () => import('./pages/entregas/entregas').then((m) => m.EntregasPage) },
      { path: 'clientes', title: 'Clientes · Avisa Aí', loadComponent: () => import('./pages/clientes/clientes').then((m) => m.ClientesPage) },
      { path: 'mensagens', title: 'Mensagens · Avisa Aí', loadComponent: () => import('./pages/mensagens/mensagens').then((m) => m.MensagensPage) },
    ],
  },
  { path: '**', redirectTo: '' },
];
