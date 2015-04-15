# Running the system in production

This document will tell you how to set up the system in a production
environment. It assumes that you have read the instructions on how to run the
system for development purposes in
[doc/running-development.md](https://github.com/rmoehn/theatralia/blob/master/doc/running-development.md).

## Generating an executable

Run:

  ```
  $ lein uberjar
  ```

This should create an executable JAR file in
`target/theatralia-<version>-standalone.jar`.

## Starting the system


1. Install Datomic on the system where you want to run Theatralia. See
   [doc/running-development.md](https://github.com/rmoehn/theatralia/blob/master/doc/running-development.md#datomic).

1. Start the Datomic transactor with the dev storage protocol as described
    [here](http://docs.datomic.com/getting-started.html#dev-storage).

1. Copy `target/theatralia-<version>-standalone.jar` to the system where you
   want to run Theatralia.

2. Run:

  ```
  $ java -jar target/theatralia-<version>-standalone.jar
  Theatralia started.
  ```

   Theatralia now listens for connections from `localhost` on port 17777. In
   order to make it reachable by the outside world, you probably need something
   to forward requests from port 80 or 443. Nginx would be one possibility and
   [here](https://fitacular.com/blog/clojure/2014/07/14/deploy-clojure-tomcat-nginx/#configure-nginx)
   is a (too) short guide on how to set it up.

## Stopping the system

Send SIGTERM or SIGINT to the Theatralia process. Theatralia will shut down
cleanly. Depending on your setup you might also want to terminate the Datomic
transactor.
