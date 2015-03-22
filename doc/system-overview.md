# Overview of the system

This document describes the parts of Theatralia on a high level and shows how
they play together. It is intended for people who know how web applications work
in general, but are not familiar with the specific case of Clojure/ClojureScript
web applications. If you're already familiar with Clojure web development, you
probably don't need to read this. If not, you will hopefully get enough of an
understanding to dive into the source code, where the comments and doc strings
will guide you.

At the moment the system consists of three main parts: the server, the database
and the client running in the browser.

```
+----------+
| Browser: |
| Client   |
+----------+
     ↓↑
+----------+   +----------+
|  Server  | ⇄ | Database |
+----------+   +----------+
```

See the [bottom](#the-components-of-the-server) of the file for a description of
how the server is again divided into multiple parts.

In the following I will narrate what is going on when you do various things in
the browser. (Note that I'm not completely sure about what some parts are doing,
so if you find something wrong, please tell me. Also I describe everything as if
it happened in real time. For macros this isn't true, of course.)

## Visiting the Theatralia welcome page

The welcome page is a relict from my early experimentations, so it involves a
lot of server-generated things.

 1. You enter `http://<your-theatralia-server>/` in the browser.
 2. The browser sends

    ```
    GET "/"
    ```

    to the server.
 3. On the server, an instance of the Jetty HTTP server is running. It
    integrates with the library [Ring](https://github.com/ring-clojure/ring).
    The HTTP server receives the request and Ring wraps it in a format suitable
    for Clojure.
 4. Ring calls a function to handle the request. This function was created by
    `theatralia.routes.make-handler` in
    [src/clj/theatralia/routes.clj](https://github.com/rmoehn/theatralia/blob/master/src/clj/theatralia/routes.clj).
    `make-handler` doesn't create this function directly, but uses `routes` from
    the library [Compojure](https://github.com/weavejester/compojure) for this
    purpose. If you look into `routes.clj`, you will see that `make-handler`
    contains something like a mapping from URLs the client can send requests to
    to the functions that will handle these requests.
 5. We made a GET request to `/`, so `theatralia.welcome-page.index`, defined in
    [src/clj/theatralia/welcome_page.clj](https://github.com/rmoehn/theatralia/blob/master/src/clj/theatralia/welcome_page.clj)
    will be invoked.
 6. `theatralia.welcome-page.index` is a function defined by the macro
    `deftemplate` from the templating library
    [Enlive](https://github.com/cgrand/enlive). It reads the HTML files
    [resources/templates/around.html](https://github.com/rmoehn/theatralia/blob/master/resources/templates/around.html)
    and
    [resource/templates/welcome.html](https://github.com/rmoehn/theatralia/blob/master/resource/templates/welcome.html),
    does some substitutions, puts them together and returns them as HTML.
 7. The server takes the returned HTML and sends it to the browser.
 8. The browser notices that the HTML references a file with the path
    `/main.css`. It sends

    ```
    GET "/main.css"
    ```

    to the server.
 9. In `make-handler` we've also defined that `theatralia.welcome-page/main-css`
    should be called to handle this request. `main-css` uses
    [Garden](https://github.com/noprompt/garden) to assemble a CSS string from
    the data in `welcome_page.clj`. Again, the server sends it to the browser.
 10. The browser renders the HTML and CSS and displays it to you. But now it
     notices that the CSS references some font files with paths like
     `/fonts/chivo/chivo-regular-webfont.woff`. Suppose for now that it's only
     this file. (The same happens for the other font files.) The browser sends

     ```
     GET "/fonts/chivo/chivo-regular-webfont.woff"
     ```

     to the server.
 11. In `make-handler` there is no entry for
     `/fonts/chivo/chivo-regular-webfont.woff` explicitly. However, there is the
     entry `(route/resources "/")`, which means: »whenever there is a GET
     request for `x` that hasn't been handled yet, find out whether
     the file `resources/public/x` exists locally and if it does, send it to the
     browser«. `resources/public/fonts/chivo/chivo-regular-webfont.woff` exists
     locally, so the server sends it to the browser.
 12. The browser receives the font files and re-renders the page with the
     correct font. (Exact behaviour might vary.)

## The components of the server

The server consist of several parts, which I call components, since they
are taken care of by the framework
[Component](https://github.com/stuartsierra/component). See the links in
[README](https://github.com/rmoehn/theatralia#notes-from-the-start) for more
detailed information.

Currently we have three components. One is the Jetty HTTP server running inside
the server process. It is defined in
[src/clj/theatralia/web_server.clj](https://github.com/rmoehn/theatralia/blob/master/src/clj/theatralia/web_server.clj).
One takes care of the connection to the database. It is defined in
[src/clj/theatralia/database.clj](https://github.com/rmoehn/theatralia/blob/master/src/clj/thea)
The third is a static component for routing. See
[src/clj/theatralia/routes.clj](https://github.com/rmoehn/theatralia/blob/master/src/clj/theatralia/routes.clj).

For starting and stopping the server during development you run `(go)` and
`(stop)` at the REPL. These are defined in
[dev/user.clj](https://github.com/rmoehn/theatralia/blob/master/dev/user.clj).
`(start)` starts the components in the right order (taking care of dependencies
between components). It first turns on the database component, which sets up the
connection to our Datomic transactor and does further setup steps if necessary.
It creates the routing componentm, providing it with access to the database
component. Finally it switches on the Jetty HTTP server, giving it a reference
to the routing component. `(stop)` stops the components in the reverse order.

If you have changed some code, you use `(reset)` to stop the system, reload
everything that has changed in a proper way and start it again.
