# clj-obt

The Oslo-Bergen-Tagger is a GPL licensed Norwegian text tagger. This is an interface for accessing it with Clojure.

clj-obt supports Linux only, and you must acquire the tagger from http://tekstlab.uio.no/obt-ny/ or clone it here on GitHub: https://github.com/noklesta/The-Oslo-Bergen-Tagger

## Installation

Simply add the library with Leiningen: `[clj-obt "0.3.5"]` and require `clj-obt.core`

## Usage

Before tagging any text, you must set the path to the Oslo-Bergen-Tagger by calling:

    (set-obt-path! "path/to/obt")

Or you can supply it when using the tagger function:

    (obt-tag "tag this" "path/to/obt")

You only need to set the path once per session. Currently, only disambiguated bokmål is supported.

The resulting output from the tagger is parsed to simple maps. There are helper functions in `clj-obt.tools` to manipulate and filter the tagged words.

## Startup time of OBT

There is a startup cost in calling the tagger. If you call `obt-tag` on a number of texts sequentially, there might be a about a seconds worth of overhead for each invocation. This is due to the startup time of OBT. We can get around this by giving `obt-tag` all the texts in a vector like this:

    (obt-tag ["many" "texts" "to" "be" "tagged"])

The tagger function will then concatenate all the texts, tag them in one OBT invocation, split them up again, and return them to you - separately in a vector. Doing this, we save about n-1 of OBT startup time cost.

## License

Copyright (C) 2011-2012 Aleksander Skjæveland Larsen

Distributed under the Eclipse Public License, the same as Clojure.
