# sonority

I recently had to reinstall my operating system and I wanted to import my [bandcamp][] tracks into iTunes, which I couldn't do.

[bandcamp]: http://bandcamp.com

I realized that there's no good excuse why I shouldn't.

There should be a platform independent music player which centers around the idea of community created plugins to extend its capabilities. Hence this project was born.

This is a proof of concept and a test run with the technology stack. Goal is to create an interface for a music player using electron, nodejs, react and clojurescript and then trying to come a up with a module system to allow others to extend the programs capabilities.

## Requirements

- Nodejs, `npm`, `grunt` and `bower`
- Clojure(script)
- Leinigen

## Installation

### Dependencies

Install `nodejs`, `npm` and the Java Runtime Environment with your favorite package manager.

- Install grunt:
  ```
  npm install -g grunt grunt-cli
  ```
- install bower:
  ```
  npm install -g bower
  ```

### Program

- Clone the repository
- Install node dependencies (JavaScript dependencies)
  ```
  npm install
  ```

- Install electron (Base program)
  ```
  grunt
  ```

- Install bower dependencies (Style)
  ```
  bower install
  ```

- Build the code (Builds the ClojureScript code)
  ```
  lein cljsbuild once
  ```
  or
  ```
  lein figwheel
  ```
  for continuous building and automatic code updates in the application.

- Launch the application
  ```
  grunt launch
  ```
