import { Component, signal, inject } from '@angular/core';
import { RouterLink, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder as NgFormBuilder, FormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSelectModule } from '@angular/material/select';
import { from } from 'rxjs';
import { concatMap, toArray } from 'rxjs/operators';
import { FormService } from '../../../../core/services/form.service';
import { QuestionService, QuestionType } from '../../../../core/services/question.service';
/** Frontend field type — maps 1:1 to backend QuestionType */
export type FieldType =
  | 'TEXT' | 'LONG_TEXT' | 'NUMERIC' | 'BOOLEAN' | 'DATE'
  | 'SCALE' | 'SLIDER' | 'LIKERT'
  | 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'RANKING' | 'MATRIX' | 'MAP';

export interface FormField {
  id: number;
  type: FieldType;
  label: string;
  required: boolean;
  options: string[];            // choices / likert labels / matrix rows
  matrixColumns: string[];      // matrix: column headers
  scaleMin: number;
  scaleMax: number;
  scaleMinLabel: string;
  scaleMaxLabel: string;
  sliderMin: number;
  sliderMax: number;
  sliderStep: number;
  mapXLeftLabel: string;        // map: left axis label
  mapXRightLabel: string;       // map: right axis label
  mapYTopLabel: string;         // map: top axis label
  mapYBottomLabel: string;      // map: bottom axis label
  weight: number;
  flagStress: boolean;
  flagSleep: boolean;
  flagOverload: boolean;
  flagFatigue: boolean;
  flagDisengagement: boolean;
  flagIsolation: boolean;
}

interface FieldTypeDef { type: FieldType; icon: string; label: string }
type RiskFlagKey = 'flagStress' | 'flagSleep' | 'flagOverload' | 'flagFatigue' | 'flagDisengagement' | 'flagIsolation';

interface RiskFlagDef {
  key: RiskFlagKey;
  label: string;
  icon: string;
}

export const FIELD_TYPE_DEFS: FieldTypeDef[] = [
  { type: 'TEXT',            icon: 'short_text',            label: 'Texto Curto' },
  { type: 'LONG_TEXT',       icon: 'notes',                 label: 'Texto Longo' },
  { type: 'NUMERIC',         icon: 'pin',                   label: 'Número' },
  { type: 'BOOLEAN',         icon: 'toggle_on',             label: 'Sim / Não' },
  { type: 'DATE',            icon: 'calendar_today',        label: 'Data' },
  { type: 'SCALE',           icon: 'linear_scale',          label: 'Escala (1–N)' },
  { type: 'SLIDER',          icon: 'tune',                  label: 'Controle Deslizante' },
  { type: 'LIKERT',          icon: 'sentiment_satisfied',   label: 'Escala Likert' },
  { type: 'SINGLE_CHOICE',   icon: 'radio_button_checked',  label: 'Escolha Única' },
  { type: 'MULTIPLE_CHOICE', icon: 'check_box',             label: 'Múltipla Escolha' },
  { type: 'RANKING',         icon: 'format_list_numbered',  label: 'Classificação' },
  { type: 'MATRIX',          icon: 'grid_on',               label: 'Matriz' },
  { type: 'MAP',             icon: 'grid_4x4',              label: 'Mapa 2D' },
];

const RISK_FLAG_DEFS: RiskFlagDef[] = [
  { key: 'flagStress', label: 'Estresse', icon: 'bolt' },
  { key: 'flagSleep', label: 'Sono', icon: 'bedtime' },
  { key: 'flagOverload', label: 'Sobrecarga', icon: 'work_history' },
  { key: 'flagFatigue', label: 'Fadiga', icon: 'battery_alert' },
  { key: 'flagDisengagement', label: 'Desengajamento', icon: 'sentiment_dissatisfied' },
  { key: 'flagIsolation', label: 'Isolamento', icon: 'person_off' },
];

/** Map frontend FieldType → backend QuestionType (identical now) */
const BACKEND_TYPE_MAP: Record<FieldType, QuestionType> = {
  TEXT: 'TEXT',
  LONG_TEXT: 'TEXT',
  NUMERIC: 'NUMERIC',
  BOOLEAN: 'BOOLEAN',
  DATE: 'DATE',
  SCALE: 'SCALE',
  SLIDER: 'SLIDER',
  LIKERT: 'LIKERT',
  SINGLE_CHOICE: 'SINGLE_CHOICE',
  MULTIPLE_CHOICE: 'MULTIPLE_CHOICE',
  RANKING: 'RANKING',
  MATRIX: 'MATRIX',
  MAP: 'MAP',
};

const DEFAULT_LIKERT_LABELS = ['Discordo Totalmente', 'Discordo', 'Neutro', 'Concordo', 'Concordo Totalmente'];

function makeField(id: number, type: FieldType, index: number): FormField {
  const hasOptions = ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'RANKING'].includes(type);
  const isLikert   = type === 'LIKERT';
  const isMatrix   = type === 'MATRIX';
  return {
    id,
    type,
    label: `Question ${index + 1}`,
    required: false,
    options: hasOptions ? ['Option 1', 'Option 2'] : isLikert ? [...DEFAULT_LIKERT_LABELS] : isMatrix ? ['Row 1', 'Row 2'] : [],
    matrixColumns: isMatrix ? ['Column 1', 'Column 2'] : [],
    scaleMin: 1,
    scaleMax: 10,
    scaleMinLabel: 'Very Low',
    scaleMaxLabel: 'Extreme',
    sliderMin: 0,
    sliderMax: 100,
    sliderStep: 1,
    mapXLeftLabel: 'Left',
    mapXRightLabel: 'Right',
    mapYTopLabel: 'High',
    mapYBottomLabel: 'Low',
    weight: 0,
    flagStress: false,
    flagSleep: false,
    flagOverload: false,
    flagFatigue: false,
    flagDisengagement: false,
    flagIsolation: false,
  };
}

@Component({
  selector: 'app-form-builder',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatSlideToggleModule,
    MatTooltipModule,
    MatSelectModule,
  ],
  templateUrl: './form-builder.html',
  styleUrl: './form-builder.scss',
})
export class FormBuilder {
  private fb = inject(NgFormBuilder);
  private router = inject(Router);
  private formService = inject(FormService);
  private questionService = inject(QuestionService);
  private nextId = 1;

  fieldTypes = FIELD_TYPE_DEFS;
  riskFlags = RISK_FLAG_DEFS;
  fields = signal<FormField[]>([]);
  saving = signal(false);
  saveError = signal<string | null>(null);
  // Phase 3: Palette collapse state
  paletteCollapsed = signal(false);
  
  // Phase 3: Track which field config sections are expanded
  expandedSections = signal<Set<string>>(new Set());
  
  // Phase 3: Track unsaved changes
  hasUnsavedChanges = signal(false);

  metaForm = this.fb.group({
    title: ['New Assessment', Validators.required],
    description: [''],
  });

  addField(type: FieldType): void {
    this.fields.update(f => [...f, makeField(this.nextId++, type, f.length)]);
    this.markUnsavedChanges();
  }

  // Phase 3: Toggle palette visibility on mobile
  togglePalette(): void {
    this.paletteCollapsed.update(v => !v);
  }

  // Phase 3: Toggle specific field config section
  toggleSection(fieldId: number, section: string): void {
    const key = `${fieldId}-${section}`;
    this.expandedSections.update(set => {
      const newSet = new Set(set);
      if (newSet.has(key)) {
        newSet.delete(key);
      } else {
        newSet.add(key);
      }
      return newSet;
    });
  }

  // Phase 3: Check if a section is expanded
  isSectionExpanded(fieldId: number, section: string): boolean {
    return this.expandedSections().has(`${fieldId}-${section}`);
  }

  // Phase 3: Mark as unsaved when form changes
  markUnsavedChanges(): void {
    this.hasUnsavedChanges.set(true);
  }

  // Phase 3: Clear unsaved changes when saved
  clearUnsavedChanges(): void {
    this.hasUnsavedChanges.set(false);
  }

  removeField(id: number): void {
    this.fields.update(f => f.filter(x => x.id !== id));
    this.markUnsavedChanges();
  }

  moveUp(index: number): void {
    if (index === 0) return;
    this.fields.update(f => {
      const arr = [...f];
      [arr[index - 1], arr[index]] = [arr[index], arr[index - 1]];
      return arr;
    });
    this.markUnsavedChanges();
  }

  moveDown(index: number): void {
    this.fields.update(f => {
      if (index >= f.length - 1) return f;
      const arr = [...f];
      [arr[index], arr[index + 1]] = [arr[index + 1], arr[index]];
      return arr;
    });
    this.markUnsavedChanges();
  }

  addOption(field: FormField): void {
    field.options = [...field.options, `Option ${field.options.length + 1}`];
    this.fields.update(f => [...f]);
  }

  removeOption(field: FormField, i: number): void {
    field.options = field.options.filter((_, idx) => idx !== i);
    this.fields.update(f => [...f]);
  }

  addMatrixColumn(field: FormField): void {
    field.matrixColumns = [...field.matrixColumns, `Column ${field.matrixColumns.length + 1}`];
    this.fields.update(f => [...f]);
  }

  removeMatrixColumn(field: FormField, i: number): void {
    field.matrixColumns = field.matrixColumns.filter((_, idx) => idx !== i);
    this.fields.update(f => [...f]);
  }

  fieldIcon(type: string): string {
    return FIELD_TYPE_DEFS.find(t => t.type === type)?.icon ?? 'help_outline';
  }

  fieldLabel(type: string): string {
    return FIELD_TYPE_DEFS.find(t => t.type === type)?.label ?? type;
  }

  scaleRange(field: FormField): number[] {
    const arr: number[] = [];
    for (let i = field.scaleMin; i <= field.scaleMax; i++) arr.push(i);
    return arr;
  }

  isFlagEnabled(field: FormField, key: RiskFlagKey): boolean {
    return !!field[key];
  }

  toggleFlag(field: FormField, key: RiskFlagKey): void {
    field[key] = !field[key];
    this.fields.update(f => [...f]);
  }

  onPreview(): void {
    if (this.metaForm.invalid || this.fields().length === 0) return;
    // Persist draft to sessionStorage for the review page
    sessionStorage.setItem('form_draft', JSON.stringify({
      meta: this.metaForm.value,
      fields: this.fields(),
    }));
    this.router.navigate(['/dashboard/forms/review']);
  }

  onSave(): void {
    if (this.metaForm.invalid || this.saving()) return;
    this.saving.set(true);
    this.saveError.set(null);
    const { title, description } = this.metaForm.value;

    this.formService.create({ title: title!, description: description ?? '', status: 'CREATED' }).subscribe({
      next: (form) => {
        const questionRequests = this.fields().map((field, i) => {
          const config: Record<string, string> = {};
          if (field.options.length) {
            field.options.forEach((opt, idx) => { config[`option_${idx}`] = opt; });
          }
          if (field.matrixColumns.length) {
            field.matrixColumns.forEach((col, idx) => { config[`col_${idx}`] = col; });
          }
          if (['SCALE', 'SLIDER'].includes(field.type)) {
            config['min'] = String(field.type === 'SCALE' ? field.scaleMin : field.sliderMin);
            config['max'] = String(field.type === 'SCALE' ? field.scaleMax : field.sliderMax);
            if (field.type === 'SCALE') {
              config['minLabel'] = field.scaleMinLabel;
              config['maxLabel'] = field.scaleMaxLabel;
            }
            if (field.type === 'SLIDER') config['step'] = String(field.sliderStep);
          }
          if (field.type === 'MAP') {
            config['xLeftLabel']   = field.mapXLeftLabel;
            config['xRightLabel']  = field.mapXRightLabel;
            config['yTopLabel']    = field.mapYTopLabel;
            config['yBottomLabel'] = field.mapYBottomLabel;
          }
          config['weight'] = String(Math.max(0, field.weight ?? 0));
          config['flagStress'] = String(!!field.flagStress);
          config['flagSleep'] = String(!!field.flagSleep);
          config['flagOverload'] = String(!!field.flagOverload);
          config['flagFatigue'] = String(!!field.flagFatigue);
          config['flagDisengagement'] = String(!!field.flagDisengagement);
          config['flagIsolation'] = String(!!field.flagIsolation);
          return this.questionService.create({
            formId: form.id,
            text: field.label,
            type: BACKEND_TYPE_MAP[field.type],
            required: field.required,
            config,
            order: i + 1,
          });
        });

        const finish = () => {
          this.saving.set(false);
          this.clearUnsavedChanges();
          this.router.navigate(['/dashboard/forms/view', form.id]);
        };

        if (questionRequests.length > 0) {
          from(questionRequests).pipe(
            concatMap(req => req),
            toArray(),
          ).subscribe({
            next: finish,
            error: () => {
              this.saveError.set('Algumas perguntas falharam ao salvar. Tente novamente.');
              this.saving.set(false);
            },
          });
        } else {
          finish();
        }
      },
      error: () => {
        this.saveError.set('Falha ao salvar formulário. Tente novamente.');
        this.saving.set(false);
      },
    });
  }
}
