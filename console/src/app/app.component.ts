import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'CloudScale';
  isDarkMode = signal(true);

  toggleTheme() {
    this.isDarkMode.update(dark => !dark);
    document.documentElement.setAttribute('data-theme', this.isDarkMode() ? 'dark' : 'light');
  }
}
