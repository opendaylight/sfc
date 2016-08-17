var gulp = require('gulp'),
    del = require('del'),
    gutil = require('gulp-util'),
    concat = require('gulp-concat'),
    runSequence = require('run-sequence'),
    install = require("gulp-install");

var config = require( './build.config.js' );

/**
 * Task for cleaning build directory
 */
gulp.task('clean', function() {
    // You can use multiple globbing patterns as you would with `gulp.src`
    return del([config.build_dir]);
});

/**
 * Copy assets
 */
gulp.task('copyAssetsCss', function () {
    return gulp.src(config.assets_files.css)
        .pipe(gulp.dest(config.build_dir + '/asets/css'));
});

gulp.task('copyAssetsLang', function () {
    return gulp.src(config.assets_files.lang)
        .pipe(gulp.dest(config.build_dir  + '/asets/data'));
});

gulp.task('copyAssetsImages', function () {
    return gulp.src(config.assets_files.images)
        .pipe(gulp.dest(config.build_dir  + '/asets/images'));
});

gulp.task('copyAssetsJs', function () {
    return gulp.src(config.assets_files.js)
        .pipe(gulp.dest(config.build_dir  + '/asets/jss'));
});

gulp.task('copyAssetsYang', function () {
    return gulp.src(config.assets_files.yang)
        .pipe(gulp.dest(config.build_dir  + '/asets/yang'));
});

gulp.task('copyAssetsXml', function () {
    return gulp.src(config.assets_files.xml)
        .pipe(gulp.dest(config.build_dir  + '/asets/yang2xml'));
});


/**
 * Copy app files
 */
gulp.task('copyTemplates', function () {
    gutil.log(gutil.colors.cyan('INFO :: copying APP Template files'));
    // Copy html
    return gulp.src(config.app_files.templates)
        .pipe(gulp.dest(config.build_dir));
});

gulp.task('copyAppJs', function () {
    gutil.log(gutil.colors.cyan('INFO :: copying APP Controller JS files'));
    return gulp.src(config.app_files.js)
        .pipe(gulp.dest(config.build_dir));
});

gulp.task('copyRootJs', function () {
    gutil.log(gutil.colors.cyan('INFO :: copying APP Root JS files'));
    return gulp.src(config.app_files.root_js)
        .pipe(gulp.dest(config.build_dir));
});

/**
  * Copy vendor files
 */
gulp.task('copyVendorCss', function () {
    gutil.log(gutil.colors.cyan('INFO :: copying VENDOR css'));
    return gulp.src(config.vendor_files.css, { cwd : 'vendor/**' })
        .pipe(gulp.dest(config.build_dir + '/vendor'))
});

gulp.task('copyVendorJs', function () {
    gutil.log(gutil.colors.cyan('INFO :: copying VENDOR js files'));
    return gulp.src(config.vendor_files.js, { cwd : 'vendor/**' })
        .pipe(gulp.dest(config.build_dir + '/vendor'))
});


/**
 * Copy task aggregated
 */
gulp.task('copy', function() {
    runSequence([
        'copyAssetsCss',
        'copyAssetsLang',
        'copyAssetsImages',
        'copyAssetsJs',
        'copyAssetsYang',
        'copyAssetsXml',
        'copyTemplates',
        'copyAppJs',
        'copyRootJs',
        'copyVendorCss',
    ], 'copyVendorJs');
});

/**
 * Build task
 */
gulp.task('build', function(){
    runSequence('clean', 'copy');
});
