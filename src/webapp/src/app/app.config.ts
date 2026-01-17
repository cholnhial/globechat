import { ApplicationConfig, provideBrowserGlobalErrorListeners, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  LucideAngularModule,
  Globe,
  Plus,
  X,
  MapPin,
  Home,
  MessageCircle,
  Music,
  Folder,
  Upload,
  Search,
  LogOut,
  Users,
  Target,
  FileText,
  Clock,
  ChevronDown,
  ArrowRight,
  Plug,
  UserX,
  Square,
  Send
} from 'lucide-angular';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([authInterceptor, errorInterceptor])
    ),
    importProvidersFrom(
      LucideAngularModule.pick({
        Globe,
        Plus,
        X,
        MapPin,
        Home,
        MessageCircle,
        Music,
        Folder,
        Upload,
        Search,
        LogOut,
        Users,
        Target,
        FileText,
        Clock,
        ChevronDown,
        ArrowRight,
        Plug,
        UserX,
        Square,
        Send
      })
    )
  ]
};
