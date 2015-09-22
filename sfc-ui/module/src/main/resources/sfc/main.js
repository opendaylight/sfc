require.config({
  paths : {
    //  'angular-translate-loader-static-files' : '../vendor/angular-translate-loader-static-files/angular-translate-loader-static-files.min',
    //  'd3' : '../vendor/d3/d3.min',
    'angular-sanitize' : 'app/sfc/vendor/angular-sanitize/angular-sanitize',
    'ui-unique'  : 'app/sfc/vendor/angular-ui-utils/modules/unique/unique',
    'select2' :  'app/sfc/vendor/select2/select2',
    'ui-select2' :  'app/sfc/vendor/angular-ui-select2/index',
    //  'ui-bootstrap' : '../vendor/angular-bootstrap/ui-bootstrap-tpls.min',
    'ngTable' : 'app/sfc/vendor/ng-table/ng-table.min',
    'angular-dragdrop': 'app/sfc/vendor/angular-dragdrop/draganddrop',
    'ngStorage': 'app/sfc/vendor/ngstorage/ngStorage.min',
    'xeditable' : 'app/sfc/vendor/angular-xeditable/dist/js/xeditable',
    'ui-sortable' : 'app/sfc/vendor/angular-ui-sortable/sortable',

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
