import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { FormService, FormResponseDto } from './form.service';
import { QuestionService, QuestionResponseDto, QuestionRequestDto, QuestionType } from './question.service';
import { FormField, FieldType } from '../../features/dashboard/pages/form-builder/form-builder';

export interface RenderedForm {
  form: FormResponseDto;
  fields: FormField[];
  questionIds: string[];   // original backend IDs in order — needed for update/delete
}

/** Maps backend QuestionType → frontend FieldType */
const TYPE_MAP: Record<string, FieldType> = {
  TEXT: 'TEXT',
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

/** Maps frontend FieldType → backend QuestionType */
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

/** Convert a backend QuestionResponseDto into a frontend FormField */
export function questionToField(q: QuestionResponseDto, index: number): FormField {
  const config = q.config ?? {};
  const type: FieldType = TYPE_MAP[q.type] ?? 'TEXT';
  return {
    id: index + 1,
    type,
    label: q.text,
    required: q.required,
    options: Object.entries(config)
      .filter(([k]) => k.startsWith('option_'))
      .sort(([a], [b]) => Number(a.split('_')[1]) - Number(b.split('_')[1]))
      .map(([, v]) => v as string),
    matrixColumns: Object.entries(config)
      .filter(([k]) => k.startsWith('col_'))
      .sort(([a], [b]) => Number(a.split('_')[1]) - Number(b.split('_')[1]))
      .map(([, v]) => v as string),
    scaleMin: Number(config['min'] ?? 1),
    scaleMax: Number(config['max'] ?? 10),
    scaleMinLabel: (config['minLabel'] as string) ?? 'Very Low',
    scaleMaxLabel: (config['maxLabel'] as string) ?? 'Extreme',
    sliderMin: Number(config['min'] ?? 0),
    sliderMax: Number(config['max'] ?? 100),
    sliderStep: Number(config['step'] ?? 1),
    mapXLeftLabel: (config['xLeftLabel'] as string) ?? 'Left',
    mapXRightLabel: (config['xRightLabel'] as string) ?? 'Right',
    mapYTopLabel: (config['yTopLabel'] as string) ?? 'High',
    mapYBottomLabel: (config['yBottomLabel'] as string) ?? 'Low',
    weight: Number(config['weight'] ?? 0),
    flagStress: String(config['flagStress'] ?? 'false') === 'true',
    flagSleep: String(config['flagSleep'] ?? 'false') === 'true',
    flagOverload: String(config['flagOverload'] ?? 'false') === 'true',
    flagFatigue: String(config['flagFatigue'] ?? 'false') === 'true',
    flagDisengagement: String(config['flagDisengagement'] ?? 'false') === 'true',
    flagIsolation: String(config['flagIsolation'] ?? 'false') === 'true',
  };
}

/** Convert a frontend FormField into a backend QuestionRequestDto config map */
export function fieldToConfig(field: FormField): Record<string, string> {
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
    config['xLeftLabel'] = field.mapXLeftLabel;
    config['xRightLabel'] = field.mapXRightLabel;
    config['yTopLabel'] = field.mapYTopLabel;
    config['yBottomLabel'] = field.mapYBottomLabel;
  }
  config['weight'] = String(Math.max(0, field.weight ?? 0));
  config['flagStress'] = String(!!field.flagStress);
  config['flagSleep'] = String(!!field.flagSleep);
  config['flagOverload'] = String(!!field.flagOverload);
  config['flagFatigue'] = String(!!field.flagFatigue);
  config['flagDisengagement'] = String(!!field.flagDisengagement);
  config['flagIsolation'] = String(!!field.flagIsolation);
  return config;
}

/** Convert a frontend FormField into a QuestionRequestDto ready for POST/PUT */
export function fieldToRequest(field: FormField, order: number): QuestionRequestDto {
  return {
    text: field.label,
    type: BACKEND_TYPE_MAP[field.type],
    required: field.required,
    config: fieldToConfig(field),
    order,
  };
}

@Injectable({ providedIn: 'root' })
export class FormRendererService {
  private formService = inject(FormService);
  private questionService = inject(QuestionService);

  /**
   * Load a form with all its embedded questions in a single request.
   * The backend returns the complete form including all questions in one call —
   * no separate /questions call is required.
   */
  load(formId: string): Observable<RenderedForm> {
    return this.formService.getById(formId).pipe(
      map(form => {
        const questions = form.questions ?? [];
        return {
          form,
          fields: questions.map((q, i) => questionToField(q, i)),
          questionIds: questions.map(q => String(q.id)),
        };
      }),
    );
  }

  /**
   * Save updated fields: update existing questions in place, delete removed ones,
   * create newly added ones. formId is required when creating new questions.
   */
  saveQuestions(
    fields: FormField[],
    originalIds: string[],
    formId: string,
  ): Observable<unknown> {
    const updates = fields
      .filter((_, i) => i < originalIds.length)
      .map((field, i) =>
        this.questionService.update(originalIds[i], fieldToRequest(field, i + 1)),
      );

    const deletes = originalIds
      .slice(fields.length)
      .map(id => this.questionService.delete(id));

    const creates = fields
      .slice(originalIds.length)
      .map((field, i) =>
        this.questionService.create({ ...fieldToRequest(field, originalIds.length + i + 1), formId }),
      );

    const all = [...updates, ...deletes, ...creates];
    return all.length > 0 ? forkJoin(all) : of(null);
  }
}
