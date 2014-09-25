var controllers = [
  'app/sfc/sfc.controller',
  'app/sfc/servicenode/servicenode.controller',
  'app/sfc/serviceforwarder/serviceforwarder.controller',
  'app/sfc/servicefunction/servicefunction.controller',
  'app/sfc/servicechain/servicechain.controller',
  'app/sfc/servicepath/servicepath.controller',
  'app/sfc/config/config.controller',
  'app/sfc/utils/modal.controller'];
var services = [
  'app/core/core.services',
  'app/sfc/sfc.services',
  'app/sfc/utils/modal.services',
  'app/sfc/servicechain/servicechain.services',
  'app/sfc/servicenode/servicenode.services',
  'app/sfc/config/config.services',
  'app/sfc/config/schemas.services',
  'app/sfc/serviceforwarder/serviceforwarder.services',
  'app/sfc/servicefunction/servicefunction.services',
  'app/sfc/servicepath/servicepath.services'];
var directives = [
  'app/sfc/sfc.directives',
  'app/sfc/servicenode/servicenode.directives',
  'app/sfc/config/config.directives'
];

define(['app/sfc/sfc.module'].concat(services).concat(directives).concat(controllers), function (sfc) {

  sfc.isKarmaTest = true;  // not used

  // all scripts loaded before bootstrap

});