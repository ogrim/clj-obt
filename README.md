# clj-obt

The Oslo-Bergen-Tagger is a GPL licensed Norwegian text tagger. This is an interface for accessing it with Clojure.

clj-obt supports Linux only, and you must aquire the tagger from http://tekstlab.uio.no/obt-ny/ or clone it here on GitHub: https://github.com/noklesta/The-Oslo-Bergen-Tagger

Needless to say, as this is only version 0.0.1, please wath your step (hacks).

## Usage

Before tagging any text, you must set the path to the Oslo-Bergen-Tagger either by:

* Calling `set-obt-path!`
* or supply the path when calling `obt-tag`

You only need to set the path once. Currently, only disambiguated bokmål is supported.

## License

Copyright (C) 2011 Aleksander Skjæveland Larsen

Distributed under the Eclipse Public License, the same as Clojure.
