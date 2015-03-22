# Running the system for development

[doc/system-overview.md](https://github.com/rmoehn/theatralia/blob/master/doc/system-overview.md)
shows you which parts the system has and how they interact. This document is
about how to start all these parts when you want to work on Theatralia.

## Preparing dependencies

There are some dependencies which don't come with the repository and won't be
downloaded automatically by Leiningen. You have to add them manually before you
start. If you get stuck with any of the steps, just contact me.

### Bootstrap

I will probably switch to using Bootstrap sources sometime and then I'll have to
add it as a submodule. Maybe I'll do that soon. I don't know. For now you'll
have to download it manually.

```
$ cd resources/public
$ wget https://github.com/twbs/bootstrap/releases/download/v3.3.4/bootstrap-3.3.4-dist.zip
$ unzip bootstrap-3.3.4-dist.zip
$ mv bootstrap-3.3.4-dist bootstrap
```

### Fonts

For my old design of the main page I had chosen some non-standard fonts. See the
[README](https://github.com/rmoehn/theatralia#fonts). You'll have to download
them, have the Font Squirrel web font generator generate WOFFs from them and
then unpack them into `resources/public/fonts/chivo` and
`resources/public/fonts/theano-didot` respectively.

### Datomic

This is the hardest part. I'm using Datomic Pro Starter Edition, which is free
(as in beer), but brings with it some enterprisy nuisances. For example, you
might have to set up
[PGP](https://help.riseup.net/en/security/message-security/openpgp/best-practices).
Anyway, you'll have to get Datomic Pro Starter Edition. The [Datomic
website](http://www.datomic.com/get-datomic.html) will guide you through the
process. Especially the [Leiningen
setup](http://docs.datomic.com/getting-started.html#lein-setup) step is
important.

## Starting the system

 1. Start the Datomic transactor with the dev storage protocol as described
    [here](http://docs.datomic.com/getting-started.html#dev-storage).
 2. Start automatic ClojureScript compilation (see
    [below](#automatic-clojurescript-compilation)).

    ```
    $ lein cljsbuild auto dev
    Compiling ClojureScript.
    ...
    ```

 3. Launch the REPL and do the `(go)` (see the [system
    overview](https://github.com/rmoehn/theatralia/blob/master/doc/system_overview.clj#the-components-of-the-server)).
    You might have to type in your PGP password first.

    ```
    $ lein repl
    user=> (go)
    #<SystemMap>
    user=>
    ```

    That's it. The system is running.
 4. Go to http://localhost:17777/ and you'll see the welcome page.

## Stopping the system

Simply killing all the processes probably wouldn't do any harm, but this is the
clean way:

 1. At the REPL:

    ```
    user=> (stop)
    #<SystemMap>
    user=> (quit)
    Bye for now!
    $
    ```

 2. Kill or `Strg-C` the automatic ClojureScript compilation and the Datomic
    transactor.

## Automatic ClojureScript compilation

The ClojureScript compiler has a mode where it watches your ClojureScript source
files and automatically compiles them to JavaScript whenever you save a change.
There are some notes in the
[README](https://github.com/rmoehn/theatralia#build-system) on how I set this
up. You might also want to read the ClojureScript [Quick Start
Guide](https://github.com/clojure/clojurescript/wiki/Quick-Start).
