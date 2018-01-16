import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DataTableModule } from "angular2-datatable";


// ADF modules
import { AdfModule } from './adf.module';
import { AuthGuardBpm } from '@alfresco/adf-core';
import { AuthGuardEcm } from '@alfresco/adf-core';


// App components
import { AppComponent } from './app.component';
import { HomeComponent } from './home/home.component';
import { LoginComponent } from './login/login.component';
import { SearchComponent } from './search/search.component';

const appRoutes: Routes = [
  {
    path: '',
    component: HomeComponent
  },
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'search',
    component: SearchComponent,
    canActivate: [ AuthGuardEcm ]
  }
];

@NgModule({
  imports: [
    DataTableModule,
    BrowserModule,
    RouterModule.forRoot(
      appRoutes // ,
      // { enableTracing: true } // <-- debugging purposes only
    ),

    // ADF modules
    AdfModule,
  ],
  declarations: [
    AppComponent,
    HomeComponent,
    LoginComponent,
    SearchComponent
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
