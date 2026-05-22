import { Component, OnInit, signal, inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { from } from 'rxjs';
import { concatMap, toArray } from 'rxjs/operators';
import { FormField, FIELD_TYPE_DEFS } from '../form-builder/form-builder';
import { FormService } from '../../../../core/services/form.service';
import { QuestionService, QuestionType } from '../../../../core/services/question.service';

interface Draft {
  meta: { title: string; description: string };
  fields: FormField[];
}

const BACKEND_TYPE_MAP: Record<FormField['type'], QuestionType> = {
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

@Component({
  selector: 'app-form-review',
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './form-review.html',
  styleUrl: './form-review.scss',
})
export class FormReview implements OnInit {
  private router = inject(Router);
  private formService = inject(FormService);
  private questionService = inject(QuestionService);

  draft = signal<Draft | null>(null);
  saving = signal(false);
  saveError = signal<string | null>(null);

  fieldTypes = FIELD_TYPE_DEFS;

  ngOnInit(): void {
    const raw = sessionStorage.getItem('form_draft');
    if (!raw) {
      this.router.navigate(['/dashboard/forms/builder']);
      return;
    }
    this.draft.set(JSON.parse(raw));
  }

  goBack(): void {
    this.router.navigate(['/dashboard/forms/builder']);
  }

  fieldLabel(type: string): string {
    return this.fieldTypes.find(t => t.type === type)?.label ?? type;
  }

  fieldIcon(type: string): string {
    return this.fieldTypes.find(t => t.type === type)?.icon ?? 'help_outline';
  }

  scaleRange(field: FormField): number[] {
    const arr: number[] = [];
    for (let i = field.scaleMin; i <= field.scaleMax; i++) arr.push(i);
    return arr;
  }

  onSaveAndPublish(): void {
    const draft = this.draft();
    if (!draft || this.saving()) return;
    this.saving.set(true);
    this.saveError.set(null);

    this.formService.create({ title: draft.meta.title, description: draft.meta.description ?? '', status: 'ACTIVE' }).subscribe({
      next: (form) => {
        const reqs = draft.fields.map((field, i) => {
          const config: Record<string, string> = {};
          field.options.forEach((opt, idx) => { config[`option_${idx}`] = opt; });
          field.matrixColumns.forEach((col, idx) => { config[`col_${idx}`] = col; });
          if (field.type === 'SCALE') {
            config['min'] = String(field.scaleMin);
            config['max'] = String(field.scaleMax);
            config['minLabel'] = field.scaleMinLabel;
            config['maxLabel'] = field.scaleMaxLabel;
          }
          if (field.type === 'SLIDER') {
            config['min'] = String(field.sliderMin);
            config['max'] = String(field.sliderMax);
            config['step'] = String(field.sliderStep);
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
          sessionStorage.removeItem('form_draft');
          this.saving.set(false);
          this.router.navigate(['/dashboard/counselor-assessments']);
        };

        if (reqs.length > 0) {
          from(reqs).pipe(
            concatMap(req => req),
            toArray(),
          ).subscribe({
            next: finish,
            error: () => {
              this.saveError.set('Some questions failed to save. Please try again.');
              this.saving.set(false);
            },
          });
        } else {
          finish();
        }
      },
      error: () => {
        this.saveError.set('Failed to publish. Please try again.');
        this.saving.set(false);
      },
    });
  }

  onSaveDraft(): void {
    const draft = this.draft();
    if (!draft || this.saving()) return;
    this.saving.set(true);
    this.saveError.set(null);

    this.formService.create({ title: draft.meta.title, description: draft.meta.description ?? '', status: 'CREATED' }).subscribe({
      next: (form) => {
        const reqs = draft.fields.map((field, i) => {
          const config: Record<string, string> = {};
          field.options.forEach((opt, idx) => { config[`option_${idx}`] = opt; });
          field.matrixColumns.forEach((col, idx) => { config[`col_${idx}`] = col; });
          if (field.type === 'SCALE') {
            config['min'] = String(field.scaleMin);
            config['max'] = String(field.scaleMax);
            config['minLabel'] = field.scaleMinLabel;
            config['maxLabel'] = field.scaleMaxLabel;
          }
          if (field.type === 'SLIDER') {
            config['min'] = String(field.sliderMin);
            config['max'] = String(field.sliderMax);
            config['step'] = String(field.sliderStep);
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
          sessionStorage.removeItem('form_draft');
          this.saving.set(false);
          this.router.navigate(['/dashboard/forms/view', form.id]);
        };

        if (reqs.length > 0) {
          from(reqs).pipe(
            concatMap(req => req),
            toArray(),
          ).subscribe({
            next: finish,
            error: () => {
              this.saveError.set('Some questions failed to save. Please try again.');
              this.saving.set(false);
            },
          });
        } else {
          finish();
        }
      },
      error: () => {
        this.saveError.set('Failed to save draft. Please try again.');
        this.saving.set(false);
      },
    });
  }
}
