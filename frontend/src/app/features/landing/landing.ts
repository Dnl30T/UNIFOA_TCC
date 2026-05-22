import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-landing',
  imports: [
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatToolbarModule,
    MatCardModule,
    MatDividerModule,
  ],
  templateUrl: './landing.html',
  styleUrl: './landing.scss',
})
export class Landing {
  features = [
    {
      icon: 'monitor_heart',
      title: 'Análise de Risco de Burnout',
      description:
        'Monitoramento contínuo de indicadores psicológicos para avaliar o risco de burnout antes que ele se agrave.',
    },
    {
      icon: 'groups',
      title: 'Gestão de Times',
      description:
        'Tenha uma visão agregada do bem-estar do seu time e identifique membros em risco antecipadamente.',
    },
    {
      icon: 'insights',
      title: 'Insights Pessoais',
      description:
        'Relatórios individuais detalhados com recomendações práticas para manter a saúde mental.',
    },
  ];

  stats = [
    { value: '2.400+', label: 'Funcionários Monitorados' },
    { value: '98%', label: 'Precisão de Detecção' },
    { value: '46.000+', label: 'Avaliações Realizadas' },
    { value: '1.900+', label: 'Times Apoiados' },
  ];
}
