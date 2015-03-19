require.config({
  baseUrl : 'src',
  paths : {
    'jquery' : '../vendor/jquery/jquery.min',
    'jquery-ui' : '../vendor/jquery-ui/ui/jquery-ui',
    'angular' : '../vendor/angular/angular',
    'angularAMD' : '../vendor/angularAMD/angularAMD',
    'ngload' : '../vendor/angularAMD/ngload',
    'ui-bootstrap' : '../vendor/angular-bootstrap/ui-bootstrap-tpls.min',
    'domReady' : '../vendor/requirejs-domready/domReady',
    'Restangular' : '../vendor/restangular/dist/restangular.min',
    'underscore' : '../vendor/underscore/underscore',
    'underscore-string' : '../vendor/underscore.string/dist/underscore.string.min',
    'angular-ui-router' : '../vendor/angular-ui-router/release/angular-ui-router',
    'angular-css-injector' : '../vendor/angular-css-injector/angular-css-injector',
    'angular-cookies' : '../vendor/angular-cookies/angular-cookies.min',
    'angular-translate' : '../vendor/angular-translate/angular-translate.min',
    'angular-translate-loader-static-files' : '../vendor/angular-translate-loader-static-files/angular-translate-loader-static-files.min',
    'angular-sanitize' : '../vendor/angular-sanitize/angular-sanitize',
    'footable' : '../vendor/footable/dist/footable.min',
    'footable-sort' : '../vendor/footable/dist/footable.sort.min',
    'footable-paginate': '../vendor/footable/dist/footable.paginate.min',
    'd3' : '../vendor/d3/d3.min',
    'vis' : '../vendor/vis/dist/vis.min',
    'select2' :  '../vendor/select2/select2',
    'ui-select2' :  '../vendor/angular-ui-select2/index',
    'ocLazyLoad' : '../vendor/ocLazyLoad/dist/ocLazyLoad',
    'ui-unique'  : '../vendor/angular-ui-utils/modules/unique/unique',
    'ui-sortable' : '../vendor/angular-ui-sortable/sortable',
    'ngDragDrop' : '../vendor/angular-dragdrop/draganddrop',
    'xeditable' : '../vendor/angular-xeditable/dist/js/xeditable',
    'ng-table': '../vendor/ng-table/ng-table',
    'ngStorage': '../vendor/ngStorage/ngStorage.min'
  },

  shim : {
    'angular' : {
      exports: 'angular',
      deps: ['jquery']
    },
    'angularAMD' : ['angular'],
    'ocLazyLoad' : ['angular'],
    'Restangular' : ['angular', 'underscore'],
    'ui-bootstrap' : ['angular'],
    'ui-unique' : ['angular'],
    'ui-sortable' : ['angular'],
    'select2' : ['angular'],
    'ui-select2' : ['select2'],
    'angular-css-injector' : ['angular'],
    'angular-ui-router' : ['angular'],
    'angular-cookies' : ['angular'],
    'angular-translate': ['angular'],
    'angular-translate-loader-static-files' : ['angular-translate'],
    'angular-sanitize' : ['angular'],
    'ngload' : ['angularAMD'],
    'ng-table': ['angular'],
    'ngDragDrop' : ['angular'],
    'ngStorage' : ['angular'],
    'xeditable' : ['angular'],
    'jquery-ui' : ['jquery'],
    'jquery' : {
      exports : '$'
    },
    'footable' : ['jquery'],
    'footable-sort': ['footable'],
    'footable-paginate': ['footable'],
    'underscore' : {
      exports : '_'
    },
    'underscore-string' : {
      exports : '_.str',
      deps : ['underscore']
    }
  },

  deps : ['app/app.module']

});
