# lein-dpkg

Leiningen dpkg plugin.

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

Build the project via dpkg.

    lein dpkg build

Install the project via dpkg.

    lein dpkg install

Purge the project via dpkg.

    lein dpkg purge

Remove the project via dpkg.

    lein dpkg remove

## License

Copyright © 2012 Roman Scherer

Distributed under the Eclipse Public License, the same as Clojure.
