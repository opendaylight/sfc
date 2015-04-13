define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.directive('showValidationError', function ($timeout) {
    return {
      restrict: 'A',
      require: '^form',
      scope: {},  // for watch need isolated scope - specialy for destroying the watch
      link: function (scope, el, attrs, formCtrl) {
        // wait for initial model update & interpolation
        $timeout(function () {
          // find the text box element, which has the 'name' attribute
          var inputEl = el[0].querySelector("[name]");
          // convert the native text box element to an angular element
          var inputNgEl = angular.element(inputEl);
          // get the name on the text box so we know the property to check
          // on the form controller
          var inputName = inputNgEl.attr('name');
          // el.toggleClass('has-error', formCtrl[inputName].$invalid);

          scope.$watch(function () {
            return formCtrl[inputName].$invalid;
          }, function (newVal) {
            el.toggleClass('has-error', newVal);
          });

        }, 500);

        // only apply the has-error class after the user leaves the text box
//        inputNgEl.bind('change', function () {
//          el.toggleClass('has-error', formCtrl[inputName].$invalid);
//        });

      }
    };
  });

  sfc.register.directive('sfcWatchForReinit', function ($log) {
    // adds wotch in scope for 're'-ngInit
    return {
      restrict: 'A',
      link: function (scope, iElement, iAttrs) {
        var count = 1;

        if (iAttrs && iAttrs['sfcWatchForReinit'] && iAttrs['ngInit']) {

          scope.$watch(iAttrs['sfcWatchForReinit'], function (newVal) {
            if (count === 1) {
              count--;
              return; // skip first time - ngInit will do the work
            }

            if (angular.isUndefined(newVal)) {
              return;
            }

            scope.$eval(iAttrs['ngInit']);
          });
        } else {
          $log.warn('sfcWatchForReinit - illegal arguments');
        }
      }
    };
  });

  sfc.register.directive('uiSelect2Label', function ($timeout) {
    return {
      restrict: 'A',
      link: function (scope, iElement, iAttrs) {
        // fix for focus on label click
        $timeout(function () {
            if (iAttrs && iAttrs['uiSelect2Label']) {
              iElement.bind("click", function () {
                $('#s2id_' + iAttrs['uiSelect2Label']).find("input").focus();
              });
            }
          }
        );
      }
    };
  });

  sfc.register.directive('sfcSelect2CreateSearchChoice', function () {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        'inputId': '@inputId',
        'placeHolder': '@placeHolder',
        'bindingProperty': '=',
        'availableOptionsArray': '=?',
        'tmpForSelect2': '=?internalDataModel',
        'ngChangeFunction': '&?'
      },
      template: '<input type="hidden" id="{{inputId}}" class="form-control input-sm" ui-select2="select2Options" ng-model="tmpForSelect2" ng-required="false" data-placeholder="{{placeHolder | translate}}">',
      controller: 'sfcSelect2CreateSearchChoiceCtrl'
    };
  });

  sfc.register.directive('dynamicValidation', function ($parse, SfcValidatorSvc) {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {
        attrs.$observe('dynamicValidation', function (newVal) {
          var validation = $parse(newVal)();

          if (angular.isDefined(validation['type']) && !_.isEmpty(validation['typeRange'])) {
            ctrl.$parsers.unshift(SfcValidatorSvc['numberRange'](ctrl,
              {from: parseInt(validation['typeRange'][0], 10), to: parseInt(validation['typeRange'][1], 10)}));
          }
          else {
            ctrl.$parsers.unshift(SfcValidatorSvc[validation['type']](ctrl));
          }
        });
      }
    };
  });

  sfc.register.directive('dateAndTime', function (SfcValidatorSvc) {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {
        ctrl.$parsers.unshift(SfcValidatorSvc.dateAndTime(ctrl));
      }
    };
  });

  sfc.register.directive('vlanId', function (SfcValidatorSvc) {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {
        ctrl.$parsers.unshift(SfcValidatorSvc.vlanId(ctrl));
      }
    };
  });

  sfc.register.directive('macAddress', function (SfcValidatorSvc) {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {
        ctrl.$parsers.unshift(SfcValidatorSvc.macAddress(ctrl));
      }
    };
  });

  sfc.register.directive('numberRange', function ($parse, SfcValidatorSvc) {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {

        var params = $parse(attrs['numberRange'])();

        ctrl.$parsers.unshift(SfcValidatorSvc.numberRange(ctrl, params));
      }
    };
  });

  sfc.register.directive('uint8', function (SfcValidatorSvc) {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {
        ctrl.$parsers.unshift(SfcValidatorSvc.uint8(ctrl));
      }
    };
  });

  sfc.register.directive('uint16', function (SfcValidatorSvc) {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {
        ctrl.$parsers.unshift(SfcValidatorSvc.uint16(ctrl));
      }
    };
  });

  sfc.register.directive('uint32', function (SfcValidatorSvc) {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {
        ctrl.$parsers.unshift(SfcValidatorSvc.uint32(ctrl));
      }
    };
  });

  sfc.register.directive('uint64', function (SfcValidatorSvc) {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {
        ctrl.$parsers.unshift(SfcValidatorSvc.uint64(ctrl));
      }
    };
  });

  sfc.register.directive('port', function (SfcValidatorSvc) {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {
        ctrl.$parsers.unshift(SfcValidatorSvc.port(ctrl));
      }
    };
  });

  sfc.register.directive('ipAddress', function ($parse, SfcValidatorSvc) {
    return {
      require: 'ngModel',
      link: function (scope, elm, attrs, ctrl) {
        var params = $parse(attrs['ipAddress'])();

        ctrl.$parsers.unshift(SfcValidatorSvc.ipAddress(ctrl, params));
      }
    };
  });

  //textarea from xeditable enhanced with save on blur event
  sfc.register.directive('easyEditableTextarea', ['editableDirectiveFactory',
    function (editableDirectiveFactory) {
      return editableDirectiveFactory({
        directiveName: 'easyEditableTextarea',
        inputTpl: '<textarea></textarea>',
        addListeners: function () {
          var self = this;
          self.parent.addListeners.call(self);
          // submit textarea by ctrl+enter even with buttons
          if (self.single && self.buttons !== 'no') {
            self.autosubmit();
          }
        },
        autosubmit: function () {
          var self = this;
          self.inputEl.bind('keydown', function (e) {
            if ((e.ctrlKey || e.metaKey) && (e.keyCode === 13)) {
              self.scope.$apply(function () {
                self.scope.$form.$submit();
              });
            }
          });
          self.inputEl.bind('blur', function (e) {
            self.scope.$apply(function () {
              self.scope.$form.$submit();
            });
          });
        }
      });
    }]);

  sfc.register.directive('effectMe', function ($timeout, $parse, $rootScope) {
    return {

      link: function (scope, element, attrs) {

        var effectMeGetter = $parse(attrs.effectMe); // will watch this expression
        var effectMeSetter = effectMeGetter.assign;

        scope.$watch(
          function () { //
            return effectMeGetter(scope);
          },
          function (newValue, oldValue) {

            if (newValue === undefined) {
              return;
            }

            effectMeSetter(scope, undefined); // reset watched property

            $timeout(function () {
//              $(element[0]).effect("bounce", { times: 10 }, "slow");
              $(element[0]).effect("highlight", {color: "#aaaaff"}, 600);
            });
          },
          false);
      }
    };
  });

  sfc.register.directive('focusMe', function ($timeout, $parse) {
    return {
      link: function (scope, element, attrs) {
        var model = $parse(attrs.focusMe);

        var setFocus = function () {
          $timeout(function () {
            element[0].focus();
          });
        };

        if (model.assign) {  // binding to assignable expression
          scope.$watch(model, function (value) {
            if (value === true) {
              setFocus();
            }
          });

          element.bind('blur', function () {
            scope.$apply(model.assign(scope, false));
          });
        } else {
          setFocus();
        }
      }
    };
  });
});