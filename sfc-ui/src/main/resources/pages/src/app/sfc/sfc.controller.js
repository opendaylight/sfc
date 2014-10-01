define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('rootSfcCtrl', function ($rootScope) {

//    // register watch for debugging - works only in firefox
//    if (angular.isDefined($rootScope.watch)) {
//      $rootScope.watch('sfcs', function(id, oldval, newval) {
//        console.log('change!!!');
//
//        var innerfn = function(a, b, c) {
//          console.log('inner change!!!');
//          return c;
//        };
//
//        newval.watch('0', innerfn);
//        newval.watch('1', innerfn);
//        newval.watch('2', innerfn);
//
//        return newval;
//      });
//    }

    $rootScope['section_logo'] = 'logo_sfc';

    $rootScope.sfcState = {PERSISTED: "persisted", NEW: "new", EDITED: "edited"};
    if (angular.isDefined(Object.freeze)) {
      Object.freeze($rootScope.sfcState);
    }

    $rootScope.sfpState = {PERSISTED: "persisted", NEW: "new", EDITED: "edited"};
    if (angular.isDefined(Object.freeze)) {
      Object.freeze($rootScope.sfpState);
    }

    $rootScope.sfcs = [];
    $rootScope.sfps = [];
    $rootScope.sfpEffectMe = {};
    $rootScope.serviceFunctionConstants =
    {
      type: ["napt44", "dpi", "firewall"],
      failmode: ["open", "close"]
    };
    $rootScope.serviceLocatorConstants =
    {
      transport: ["vxlan-gpe", "gre", "other"],
      type: ["ip", "mac", "lisp"]
    };
    $rootScope.aclConstants =
    {
      "ace-type": ["ip", "eth"],
      "ace-ip": ["IPv4", "IPv6"]
    };
  });
});