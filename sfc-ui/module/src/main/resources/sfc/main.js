require.config({
  paths : {
    //  'angular-translate-loader-static-files' : '../vendor/angular-translate-loader-static-files/angular-translate-loader-static-files.min',
    //  'd3' : '../vendor/d3/d3.min',
    'angular-sanitize' : 'app/sfc/node_modules/angular-sanitize/angular-sanitize',
    'ui-unique'  : 'app/sfc/node_modules/angular-ui-utils/modules/unique/unique',
    'select2' :  'app/sfc/node_modules/select2/select2',
    'ui-select2' :  'app/sfc/node_modules/angular-ui-select2/src/select2',
    //  'ui-bootstrap' : '../vendor/angular-bootstrap/ui-bootstrap-tpls.min',
    'ngTable' : 'app/sfc/node_modules/ng-table/dist/ng-table.min',
    'angular-dragdrop': 'app/sfc/node_modules/angular-dragdrop/src/angular-dragdrop',
    'ngStorage': 'app/sfc/node_modules/ngstorage/ngStorage.min',
    'xeditable' : 'app/sfc/node_modules/angular-xeditable/dist/js/xeditable',
    'ui-sortable' : 'app/sfc/node_modules/angular-ui-sortable/src/sortable',

  },

  shim : {
    //  'ui-bootstrap' : ['angular'],
    //  'angular-translate-loader-static-files' : ['angular-translate'],
    'angular-sanitize' : ['angular'],
    'ngTable' : ['angular'],
    'angular-dragdrop' : ['angular'],
    'ui-select2' : ['angular','select2'],
    'select2' : ['angular'],
    'ngStorage' : ['angular'],
    'xeditable' : ['angular'],
    'ngDragDrop' : ['angular'],
    'ui-sortable' : ['angular'],
    'ui-unique' : ['angular'],
  },

});

define(['app/sfc/sfc.module']);
