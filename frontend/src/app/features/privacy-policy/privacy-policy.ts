import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-privacy-policy',
  imports: [RouterLink, MatButtonModule, MatIconModule, MatDividerModule],
  templateUrl: './privacy-policy.html',
  styleUrl: './privacy-policy.scss',
})
export class PrivacyPolicy {
  readonly effectiveDate = '21 de maio de 2026';
  readonly lastUpdated = '21 de maio de 2026';
}
