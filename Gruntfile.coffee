'use strict'

module.exports = (grunt) ->


  moment   = require 'moment'
  path     = require 'path'
  packager = require 'electron-packager'

  os = (->
    platform = process.platform
    if /^win/.test(platform)
      "windows"
    else if /^darwin/.test(platform)
      "mac"
    else if /^linux/.test(platform)
      "linux"
    else
      null
  )()

  exe =
    windows:  "electron.exe"
    mac:  "Electron.app/Contents/MacOS/Electron"
    linux:  "electron"

  electron_path    = "electron";
  electron_version = "0.33.1";

  packageJson      = require __dirname + '/package.json'

  #------------------------------------------------------------------------------
  # ShellJS
  #------------------------------------------------------------------------------

  require 'shelljs/global'
  # shelljs/global makes the following imports:
  #   cwd, pwd, ls, find, cp, rm, mv, mkdir, test, cat,
  #   str.to, str.toEnd, sed, grep, which, echo,
  #   pushd, popd, dirs, ln, exit, env, exec, chmod,
  #   tempdir, error

  shellconfig = require('shelljs').config
  shellconfig.silent = false # hide shell cmd output?
  shellconfig.fatal = false  # stop if cmd failed?

  # ------------------------------------------------------------------------------
  #  Grunt Config
  # ------------------------------------------------------------------------------


  grunt.initConfig
    'download-electron':
      version: electron_version
      outputDir: 'electron'
    sass:
      dist:
        options:
          style: 'compressed'
          loadPath: 'src/sass'
        files:
          'app/css/main.css': 'src/sass/main.sass'
      dev:
        options:
          style: 'expanded'
          loadPath: 'src/sass'
        files:
          'app/css/main.css': 'src/sass/main.sass'


  # ------------------------------------------------------------------------------
  #  Third-party tasks
  # ------------------------------------------------------------------------------


  grunt.loadNpmTasks 'grunt-download-electron'
  if os == "mac"
    grunt.loadNpmTasks 'grunt-appdmg'
  grunt.loadNpmTasks 'winresourcer'

  # ------------------------------------------------------------------------------
  #  Setup Tasks
  # ------------------------------------------------------------------------------

  grunt.loadNpmTasks 'grunt-contrib-sass'

  grunt.registerTask 'setup', [
    'download-electron',
    'ensure-config-exists',
    'run-app-bower'
  ]

  grunt.registerTask 'ensure-config-exists', ->
    pushd "app"
    unless test("-f", "config.json")
      grunt.log.writeln "Creating default config.json..."
      cp "example.config.json", "config.json"

    popd()

  grunt.registerTask 'run-app-bower', ->
    exec "bower install"

  grunt.registerTask 'cljsbuild-prod', ->
    grunt.log.writeln "\nCleaning and building ClojureScript production files..."
    exec "lein do clean, with-profile production cljsbuild once"

  grunt.registerTask 'launch', ['sass:dev', 'launch-app' ]

  grunt.registerTask 'launch-app', (async) ->
    IsAsync = (async == "true")
    grunt.log.writeln "\nLaunching development version..."
    local_exe = exe[os]
    grunt.task.run 'sass'
    exec(path.join(electron_path, local_exe) + " app", {async:IsAsync})

  grunt.registerTask 'check-old', ->
    grunt.log.writeln "\nChecking clojure dependencies"
    exec "lein ancient :all", {silent:false}
    grunt.log.writeln "\nChecking npm dependencies"
    exec "npm outdated", {silent:false}
    grunt.log.writeln "\nChecking bower dependencies"
    exec "bower list", {silent:false}

  # ------------------------------------------------------------------------------
  #  Test Tasks
  # ------------------------------------------------------------------------------

  # ------------------------------------------------------------------------------
  #  Release Helper functions
  # ------------------------------------------------------------------------------

  setReleaseConfig = (build, paths) ->
    grunt.log.writeln "\nRemoving config to force default release settings..."
    rm '-f', paths.releaseCfg
    cp paths.prodCfg, paths.releaseCfg

  getBuildMeta = ->
    grunt.log.writeln "Getting project metadata..."
    tokens = cat("project.clj").split(" ")
    build =
      name:    tokens[1]
      version: tokens[2].replace(/"/g, "").trim()
      date:    moment().format("YYYY-MM-DD")

    commit = exec("git rev-list HEAD --count", {silent:true}).output.trim()
    build.commit = if commit == '' then commit else "pre"
    build.releaseName = build.name + "-v" + build.version + "-" + build.commit
    build

  getReleasePaths = (build) ->
    builds = "builds"
    devApp = "app"
    rootPkg = "package.json"
    releaseApp = path.join builds, devApp

    paths =
      builds: builds
      devApp: devApp
      rootPkg: rootPkg
      release: path.join builds, build.releaseName
      devPkg: path.join devApp, "package.json"
      prodCfg: path.join devApp, "prod.config.json"
      releaseApp: releaseApp
      releasePkg: path.join releaseApp, "package.json"
      releaseCfg: path.join releaseApp, "config.json"
      releaseResources: path.join releaseApp, "components"
    paths

  getBasicReleaseInfo = (build, paths) ->
    opts =
      "dir": paths.releaseApp
      "name": packageJson.name
      "version": electron_version
      "asar": true
      "out": paths.release
      "overwrite": true
      "app-bundle-id": "com.example"
      "app-version": build.version
      "version-string":
        "ProductVersion": build.version
        "ProductName": packageJson.name

    opts

  stampRelease = (build, paths) ->
    grunt.log.writeln "\nStamping release with build metadata..."
    pkg = grunt.file.readJSON paths.releasePkg
    pkg.version = build.version
    pkg["build-commit"] = build.commit
    pkg["build-date"] = build.date
    JSON.stringify(pkg, null, "  ").to(paths.releasePkg)

  defineRelease = (done, extra_opts, cb) ->
    callback = cb || (-> {})
    build = getBuildMeta()
    paths = getReleasePaths build
    basic_opts = getBasicReleaseInfo build, paths
    opts = Object.assign basic_opts, extra_opts

    packager opts, (err, appPath) ->
      if err
        grunt.log.writeln "Error: ".red, err
      if appPath
        if Array.isArray appPath
          appPath.forEach (i) ->
            callback i
            grunt.log.writeln "Build: " + i.cyan

        else
          callback appPath
          grunt.log.writeln "Build: " + appPath.cyan
      done err

  deleteExtraResources = (paths) ->
    rm '-rf', path.join(paths.releaseApp, "js", "p", "out")


  # ------------------------------------------------------------------------------
  #  Tasks
  # ------------------------------------------------------------------------------

  grunt.registerTask 'release', [ 'sass',
                                  'cljsbuild-prod',
                                  'prepare-release',
                                  'release-linux',
                                  'release-mac',
                                  'release-win']

  grunt.registerTask 'cljsbuild-prod', ->
    grunt.log.writeln "\nCleaning and building ClojureScript production files..."
    exec "lein do clean, with-profile production cljsbuild once"

  grunt.registerTask 'prepare-release', ->
    build = getBuildMeta()
    paths = getReleasePaths build

    grunt.log.writeln "name:    "+build.name.cyan
    grunt.log.writeln "version: "+build.version.cyan
    grunt.log.writeln "date:    "+build.date.cyan
    grunt.log.writeln "commit:  "+build.commit.cyan
    grunt.log.writeln "release: "+build.releaseName.cyan

    mkdir '-p', paths.builds

    if test "-d", paths.releaseApp
      rm '-r', paths.releaseApp

    if test "-d", paths.release
      rm '-rf', paths.release

    #copy app folder
    cp '-r', paths.devApp, paths.builds

    grunt.log.writeln "\nCopying node dependencies to release..."
    cp '-f', paths.rootPkg, paths.releaseApp
    pushd paths.releaseApp
    exec 'npm install --no-optional --production --silent'
    popd()
    cp '-f', paths.devPkg, paths.releaseApp

    deleteExtraResources paths
    stampRelease build, paths
    setReleaseConfig build, paths

  grunt.registerTask 'release-linux', ->
    done = this.async()
    opts =
      "arch": ["x64"]
      "platform": "linux"
    defineRelease done, opts

  grunt.registerTask 'makensis', ->
    grunt.log.writeln "\nCreating installer..."
    config = grunt.config.get("makensis")

    ret = exec(["makensis",
                "-DPRODUCT_VERSION=" + config.version,
                "-DRELEASE_DIR=" + config.releaseDir,
                "-DOUTFILE=" + config.outFile,
                "scripts/build-windows-exe.nsi"].join(" "))

    if ret.code == 0
      grunt.log.writeln "\nInstaller created. Removing win32 folder:", config.releaseDir.cyan
      rm '-rf', config.releaseDir


  grunt.registerTask 'release-win', ->
    done = this.async()
    build = getBuildMeta()
    cb = (appPath) ->
      if which "makensis"
        dirName = path.join appPath, ".."
        exeName = path.join dirName, path.basename(dirName) + ".exe"
        grunt.config.set "makensis",
          version: build.version
          releaseDir: path.resolve appPath # absolute paths required on linux
          outFile: path.resolve exeName

        grunt.task.run "makensis"
      else
          grunt.log.writeln "\nSkipping windows installer creation:", "makensis not installed or not in path".cyan

    opts =
      "arch": ["x64"],
      "platform": "win32",
      "icon": "app/img/logo.ico"
    defineRelease done, opts, cb

  grunt.registerTask 'release-mac', ->
    done = this.async()
    cb = null
    if os == "mac"
      cb = (f) ->
        dirName = path.join f, ".."
        dmgName = path.join dirName, path.basename(dirName) + ".dmg"
        grunt.config.set "appdmg",
          options:
            "title": "sonority"
            "background": "scripts/dmg/TestBkg.png"
            "icon-size": 80
            "contents": [
              { "x": 448, "y": 344, "type": "link", "path": "/Applications" },
              { "x": 192, "y": 344, "type": "file", "path": path.join(f, packageJson.name + ".app") }
            ]
          target:
            dest: dmgName

        grunt.task.run "appdmg"
    opts =
      arch: "x64"
      platform: "darwin"
      icon: "app/img/logo.icns"
    defineRelease done, opts, cb


  # ------------------------------------------------------------------------------
  #  Default Task
  # ------------------------------------------------------------------------------

  grunt.registerTask 'default', ['setup']

  # end module.exports
