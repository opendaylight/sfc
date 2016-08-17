/**
 * This file/module contains all configuration for the build process.
 */
module.exports = {
    build_dir: 'build',
    app_dir: 'app',

    app_files: {
        js: [
            '*/**/*.js',

            '!node_modules/**/*.*',
            '!vendor/**/*.*',
            '!assets/**/*.*'
        ],
        root_js: [
            'main.js',
            'setAssetsDirInSrc.js',
            'sfc.controller.js',
            'sfc.directives.js',
            'sfc.module.js',
            'sfc.services.js',
            'sfc.test.module.loader.js'
        ],

        templates: ['*/**/*.tpl.html'],
    },

    assets_files: {
        css: ['assets/css/*.css'],
        lang: ['assets/data/*.json'],
        images: ['assets/images/*.*'],
        js: ['assets/js/**/*.js'],
        yang: ['assets/yang/*.yang'],
        xml: ['assets/yang2xml/*.xml']
    },

    vendor_files: {
        js: [
            'angular-sanitize/angular-sanitize.js',
            'angular-ui-utils/modules/unique/unique.js',
            'select2/select2.js',
            'angular-ui-select2/index.js',
            'ng-table/ng-table.min.js',
            'angular-dragdrop/draganddrop.js',
            'ngstorage/ngStorage.min.js',
            'angular-xeditable/dist/js/xeditable.js',
            'angular-ui-sortable/sortable.js'
        ],
        css: [
            'select2/select2.css',
            'select2-bootstrap-css/select2-bootstrap.css'

        ],
        fonts: []
    }

};
