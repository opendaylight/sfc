var controllers = [
  'app/sfc/sfc.controller',
  'app/sfc/servicenode/servicenode.controller',
  'app/sfc/serviceforwarder/serviceforwarder.controller',
  'app/sfc/servicefunction/servicefunction.controller',
  'app/sfc/servicechain/servicechain.controller',
  'app/sfc/servicepath/servicepath.controller',
  'app/sfc/config/config.controller',
  'app/sfc/utils/modal.controller',
  'app/sfc/acl/acl.controller',
  'app/sfc/metadata/metadata.controller',
  'app/sfc/servicelocator/servicelocator.controller'];
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
  'app/sfc/servicepath/servicepath.services',
  'app/sfc/servicelocator/servicelocator.services',
  'app/sfc/acl/acl.services'];
var directives = [
  'app/sfc/sfc.directives',
  'app/sfc/servicenode/servicenode.directives',
  'app/sfc/config/config.directives',
  'app/sfc/servicelocator/servicelocator.directives',
  'app/sfc/acl/acl.directives',
  'app/sfc/metadata/metadata.directives'
];

define(['app/sfc/sfc.module'].concat(services).concat(directives).concat(controllers), function (sfc) {

  sfc.isKarmaTest = true;  // not used

  sfc.customJasmineMatchers = {

    toContainProperty: function (expected) {

      // this will be jasmine object

      if (expected === undefined || expected === null) {
        throw new Error('expected should not be empty');
      }

      return this.env.contains_(Object.keys(this.actual), expected);
    }
  };

  // all scripts loaded before bootstrap

  return sfc;
});