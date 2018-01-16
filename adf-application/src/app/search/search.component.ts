import { Component, OnInit, ViewChild } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { WebscriptComponent } from '@alfresco/adf-content-services';
import { DataTableModule } from "angular2-datatable";

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.css']
})
export class SearchComponent implements OnInit {

  data: any[];
  query: String = null;
  scriptPath: String = null;
  scriptArgs: Object = {};

  constructor() {

  }

  onExecute() {
    this.scriptArgs = { "query": this.query };
  }

  onSearchSuccess(ev) {
    this.data = ev.data;
  }

  ngOnInit() {
    this.scriptPath = "tenantFiles";
    this.query = "@cm\\:name:test";
    this.scriptArgs = { query: "" };
  }


}
