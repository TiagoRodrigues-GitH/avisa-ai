import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, Settings, Template, errorText } from '../../core/api';
import { TYPE_BY_VALUE } from '../../core/labels';

@Component({
  selector: 'app-mensagens',
  imports: [FormsModule],
  template: `
    <header class="page-head">
      <div>
        <h1>Mensagens</h1>
        <p>Personalize os textos. Variáveis entre chaves são trocadas pelos dados de cada cliente.</p>
      </div>
    </header>

    @if (error()) {
      <p class="alert" role="alert">{{ error() }}</p>
    }
    @if (saved()) {
      <p class="ok-msg" role="status">{{ saved() }}</p>
    }

    <section class="card company">
      <h2>Sua empresa</h2>
      <div class="three">
        <label>Nome que aparece nas mensagens <input [(ngModel)]="settings().businessName" maxlength="80" /></label>
        <label>Endereço <input [(ngModel)]="settings().address" maxlength="200" /></label>
        <label>Telefone (opcional) <input [(ngModel)]="settings().businessPhone" inputmode="tel" /></label>
      </div>
      <button type="button" class="btn btn-primary" (click)="saveSettings()">Salvar dados da empresa</button>
    </section>

    <p class="vars">
      @for (v of vars; track v) {
        <code>{{ '{' + v + '}' }}</code>
      }
    </p>

    @for (group of groups(); track group.type) {
      <section class="group">
        <h2><span aria-hidden="true">{{ typeOf[group.type].icon }}</span> {{ group.label }}</h2>
        <div class="cards">
          @for (t of group.items; track t.kind) {
            <article class="card tpl">
              <header>
                <strong>{{ t.label }}</strong>
                @if (t.custom) {
                  <span class="chip">Personalizada</span>
                }
              </header>
              <label><span class="visually-hidden">Texto de {{ t.label }}</span>
                <textarea [(ngModel)]="drafts[t.kind]" rows="4" maxlength="1000"></textarea>
              </label>
              <span class="hint">Exemplo:</span>
              <p class="bubble">{{ t.preview }}</p>
              <div class="row">
                <button type="button" class="btn btn-outline btn-sm" [disabled]="drafts[t.kind] === t.text" (click)="save(t)">Salvar</button>
                @if (t.custom) {
                  <button type="button" class="btn btn-ghost btn-sm" (click)="reset(t)">Voltar ao padrão</button>
                }
              </div>
            </article>
          }
        </div>
      </section>
    }
  `,
  styles: `
    .company { display: grid; gap: 1rem; margin-bottom: 1.25rem; }
    .company .btn { justify-self: start; }
    .three { display: grid; grid-template-columns: repeat(auto-fit, minmax(14rem, 1fr)); gap: 1rem; }
    .vars { display: flex; flex-wrap: wrap; gap: 0.4rem; margin: 0 0 1rem; }
    code { padding: 0.15rem 0.5rem; border-radius: 0.4rem; background: var(--blue-100); color: var(--blue-900); font-size: 0.85rem; }
    .group + .group { margin-top: 1.5rem; }
    .group h2 { margin-bottom: 0.6rem; font-size: 1.05rem; color: var(--blue-900); }
    .cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(20rem, 1fr)); gap: 1rem; }
    .tpl { display: grid; gap: 0.5rem; }
    .tpl header { display: flex; justify-content: space-between; gap: 0.5rem; color: var(--blue-950); }
    .bubble { margin: 0; padding: 0.6rem 0.8rem; border-radius: 0.2rem 0.8rem 0.8rem 0.8rem; background: var(--blue-50); white-space: pre-wrap; font-size: 0.9rem; }
    .row { display: flex; gap: 0.4rem; }
    .ok-msg { padding: 0.7rem 1rem; border-left: 4px solid var(--ok); border-radius: 0.7rem; background: var(--ok-bg); color: var(--ok); }
  `,
})
export class MensagensPage implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly typeOf = TYPE_BY_VALUE;
  protected readonly vars = ['nome', 'empresa', 'titulo', 'data', 'hora', 'valor', 'endereco', 'minutos'];
  protected readonly templates = signal<Template[]>([]);
  protected readonly error = signal('');
  protected readonly saved = signal('');
  protected drafts: Record<string, string> = {};
  protected readonly settings = signal<Settings>({ businessName: '', address: '', businessPhone: null });

  protected readonly groups = computed(() => {
    const map = new Map<string, { type: Template['type']; label: string; items: Template[] }>();
    for (const t of this.templates()) {
      const g = map.get(t.type) ?? { type: t.type, label: t.typeLabel, items: [] };
      g.items.push(t);
      map.set(t.type, g);
    }
    return [...map.values()];
  });

  ngOnInit(): void {
    this.load();
    this.api.settings().subscribe({
      next: (s) => {
        this.settings.set({ ...s });
      },
      error: (e) => this.error.set(errorText(e)),
    });
  }

  save(t: Template): void {
    this.api.saveTemplate(t.kind, this.drafts[t.kind]).subscribe({ next: () => this.done('Texto salvo.'), error: (e) => this.error.set(errorText(e)) });
  }

  reset(t: Template): void {
    this.api.resetTemplate(t.kind).subscribe({ next: () => this.done('Texto padrão restaurado.'), error: (e) => this.error.set(errorText(e)) });
  }

  saveSettings(): void {
    this.api.saveSettings(this.settings()).subscribe({ next: () => this.done('Dados da empresa salvos.'), error: (e) => this.error.set(errorText(e)) });
  }

  private done(msg: string): void {
    this.error.set('');
    this.saved.set(msg);
    this.load();
  }

  private load(): void {
    this.api.templates().subscribe({
      next: (list) => {
        this.drafts = Object.fromEntries(list.map((t) => [t.kind, t.text]));
        this.templates.set(list);
      },
      error: (e) => this.error.set(errorText(e)),
    });
  }
}
