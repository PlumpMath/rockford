# rockford

A web application for analysing drug resistance mutations from human pathogens. Fasta parsing and error checking is done via interop with BioJava. At present you need to perform a multiple sequence alignment using separate software (Clustal etc.) and upload the output, but if I can figure out what a usable web interface would look like I'll have BioJava handle the msa too.

I don't really imagine there are a lot of people out there who will find a use for this specific application, but while making it I've had to wrestle with various issues that the documentation doesn't direcly address (How do you deal with enums in Clojure when doing Java interop? What happened to batch processing with prepared statements in clojure.java.jdbc >= 0.6? Was the concept of logging configuration files put on this earth to try us?) so potentially this code will help someone out there.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

## License

Copyright �� 2015-16 Stu West

Distributed under the Eclipse Public License, the same as Clojure.
