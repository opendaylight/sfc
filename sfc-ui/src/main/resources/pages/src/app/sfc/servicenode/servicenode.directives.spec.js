define(['app/sfc/sfc.test.module.loader'], function (sfc) {

  ddescribe('SFC app', function () {
    var rootScope, scope, state, stateParams, compile, provide;
    var exampleGraph = {
      name: "sn1",
      children: [
        {
          name: "sff1",
          children: [
            {
              name: "sf1",
              text: {x: -50, y: 20, align: "top"},
              image : {url: "assets/images/Device_switch_3062_unknown_64.png", wh: "40", xy: "-20"}
            },
            {name: "sf2"}
          ]
        }
      ]
    };

    beforeEach(angular.mock.module('ui.router'));
    beforeEach(angular.mock.module('pascalprecht.translate'));
    beforeEach(angular.mock.module('app.common.layout'));
    beforeEach(angular.mock.module('app.sfc'));

    beforeEach(angular.mock.module(function ($provide){
      provide = $provide;
    }));

    beforeEach(angular.mock.inject(function ($controller, $q, $state, $stateParams, $rootScope, $compile) {
      rootScope = $rootScope;
      scope = $rootScope.$new();
      state = $state;
      stateParams = $stateParams;
      compile = $compile;
    }));

    //special event for testing d3 nodes topology visualization
    jQuery.fn.d3Click = function () {
      this.each(function (i, e) {
        var evt = document.createEvent("MouseEvents");
        evt.initMouseEvent("click", true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null);
        e.dispatchEvent(evt);
      });
    };

    //special event for testing d3 nodes topology visualization
    jQuery.fn.d3MouseOver = function () {
      this.each(function (i, e) {
        var evt = document.createEvent("MouseEvents");
        evt.initMouseEvent("mouseover", true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null);
        e.dispatchEvent(evt);
      });
    };

    //special event for testing d3 nodes topology visualization
    jQuery.fn.d3MouseOut = function () {
      this.each(function (i, e) {
        var evt = document.createEvent("MouseEvents");
        evt.initMouseEvent("mouseout", true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null);
        e.dispatchEvent(evt);
      });
    };

    describe('servicenode.directives', function () {
      var compileDirective, element;
      var serviceNodeTopologyBackend;

      beforeEach(function() {
        provide.decorator('ServiceNodeTopologyBackend', function($delegate){
          serviceNodeTopologyBackend = $delegate;
          spyOn(serviceNodeTopologyBackend, '_removeIds').andCallThrough();
          spyOn(serviceNodeTopologyBackend, '_collapse').andCallThrough();
          spyOn(serviceNodeTopologyBackend, '_click').andCallThrough();
          spyOn(serviceNodeTopologyBackend, 'msieversion').andCallThrough();
          return $delegate;
        });
      });

      beforeEach(function(){
        compileDirective = function(){
          scope.nodeGraph = exampleGraph;
          var div = compile('<div style="width: 400px"><service-nodes-topology tree-view-data="nodeGraph"></service-nodes-topology></div>')(scope);
          element = div.find('service-nodes-topology');
          element.isolateScope().$apply();
        };
      });

      it("should replace directive element with service node visualization (svg)", function () {
        compileDirective();
        var svg = element[0].children[0];
        expect(svg.nodeName).toBe("svg");
        var g = svg.children[0];
        expect(g.nodeName).toBe("g");
        expect(g.childElementCount).toBe(7);
      });

      it("should remove IDs from node elements", function () {
        compileDirective();
        expect(serviceNodeTopologyBackend._removeIds).toHaveBeenCalled();
      });

      it("g nodes should have foreignObject label (if not IE)", function() {
        compileDirective();
        var svg = element[0].children[0];
        var g = svg.children[0];
        var sn = g.children[3];
        var foreignObject = sn.lastChild;
        expect(serviceNodeTopologyBackend.msieversion).toHaveBeenCalled();
        expect(foreignObject.nodeName).toBe("foreignObject");
        expect(foreignObject.innerHTML).toBe('<div class="nodeLabelOutContainer"><p class="nodeLabelInContainer" style="vertical-align: bottom;">' +
          '<p class="nodeLabel">sn1</p></p></div>');
      });

      it("g nodes should have text label (IE only)", function() {
        provide.decorator('ServiceNodeTopologyBackend', function($delegate){
          $delegate.msieversion = function (){return true;};
          serviceNodeTopologyBackend = $delegate;
          spyOn(serviceNodeTopologyBackend, 'msieversion').andCallThrough();
          return $delegate;
        });

        compileDirective();
        var svg = element[0].children[0];
        var g = svg.children[0];
        var sn = g.children[3];
        var text = sn.lastChild;
        expect(serviceNodeTopologyBackend.msieversion).toHaveBeenCalled();
        expect(text.nodeName).toBe("text");
        expect(text.innerHTML).toBe("sn1");
      });

      //TODO: add check if the node was collasped
      it("should collapse node (call ._click function)", function () {
        compileDirective();
        var svg = element[0].children[0];
        var g = svg.children[0];
        var sn = angular.element(g.children[3]);
        sn.d3Click();
        expect(serviceNodeTopologyBackend._click).toHaveBeenCalled();
      });

      //TODO: mouseover / mouseout not working
      it("should display tooltip div (on mouseover)", function () {
        compileDirective();
        var svg = element[0].children[0];
        var g = svg.children[0];
        var sn = angular.element(g.children[3]);
        sn.d3MouseOut();
      });

    });
  });
});