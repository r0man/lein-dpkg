# lein-dpkg [![Build Status](https://travis-ci.org/r0man/lein-dpkg.png)](https://travis-ci.org/r0man/lein-dpkg)

Leiningen plugin for the Debian package management system.

## Installation

Via Clojars: https://clojars.org/lein-dpkg

## Usage

Add the Debian control file debian/DEBIAN/control

    Package: {{name}}
    Version: {{version}}
    Section: java
    Priority: optional
    Maintainer: maintainer maintainer@example.com
    Architecture: all
    Depends: {{depends}}
    Description: {{description}}

Add the template for the changes file debian/DEBIAN/template.changes


    Format: 1.8
    Date: {{date}}
    Source: {{name}} 
    Binary: {{name}}
    Architecture: all
    Version: {{version}}
    Distribution: demo
    Maintainer: Lein <test@leindpkg.com>
    Description: whatever description
    Changes:
     * new version system
    Files:
       {{md5}} {{size}} main important {{deb-file}}

Build the Debian package.

    lein dpkg build

Install the Debian package.

    lein dpkg install

Purge the Debian package.

    lein dpkg purge

Remove the Debian package.

    lein dpkg remove

## License

Copyright © 2012 Roman Scherer

Distributed under the Eclipse Public License, the same as Clojure.
