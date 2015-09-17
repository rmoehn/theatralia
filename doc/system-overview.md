# Overview of the system

This document describes the parts of Theatralia on a high level and shows how
they play together. It is intended for people who know how web applications work
in general, but are not familiar with the specific case of Clojure/ClojureScript
web applications. If you're already familiar with Clojure web development, you
probably don't need to read this. If not, you will hopefully get enough of an
understanding to dive into the source code, where the comments and doc strings
will guide you. Note though, that you'll have to learn a lot and consult the
pages I link to in order to understand fully what's going on.

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

## System stories

In the following I will narrate what is going on when you do various things in
the browser. (Note that I'm not completely sure about what some parts are doing,
so if you find something wrong, please tell me. Also I describe everything as if
it happened in real time. For macros this isn't true, of course.)

### Visiting the Theatralia welcome page

The welcome page is a relict from my early experimentations, so it involves a
lot of server-generated things.

 1. You enter `http://<your-theatralia-server>/` in the browser.

 2. The browser sends

    ```
    GET /
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
    GET /main.css
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
     GET /fonts/chivo/chivo-regular-webfont.woff
     ```

     to the server.

 11. In `make-handler` there is no entry for
     `/fonts/chivo/chivo-regular-webfont.woff` explicitly. However, there is the
     entry `(route/resources "/")`, which means: »whenever there is a GET
     request for `x` that hasn't been handled yet, find out whether
     the file `resources/public/x` exists locally and if it does, send it to the
     browser«. `resources/public/fonts/chivo/chivo-regular-webfont.woff` exists
     locally, so the server sends it to the browser. In the following I will
     call files served through this default route *static files*.

 12. The browser receives the font files and re-renders the page with the
     correct font. (Exact behaviour might vary.)

### Visiting the sandbox

The sandbox is generated entirely on the client side and communicates with the
server through XML HTTP requests (XHR). This section describes how it comes to
be displayed in your browser and how the search results appear on it. If you
haven't read the [previous story](#visiting-the-theatralia-welcome-page), do it
before you start with this.

Also, you need to learn about React and re-frame.
[React](https://facebook.github.io/react/index.html) is »a JavaScript library
for building user interfaces«. Described in a short and possibly inaccurate way:
in a traditional JavaScript application you have some data and the user
interface, and some code, which manipulates the data and the user interface
through the DOM. With React, you have a data structure and kind of a
specification how parts of the user interface correspond to this data structure.
You manipulate only the data structure and React takes care of changing the DOM
accordingly and in a way that is efficient to render by the browser. Later I
will describe the exact events for our application in more detail.

Since React is a JavaScript library, we don't use it directly from our
ClojureScript code; we use
[Reagent](https://github.com/reagent-project/reagent). And on top of it, we use
[re-frame](https://github.com/Day8/re-frame). I won't explain much here; you'll
have to read re-frame's README and follow the links in it. Our app is structured
as described [here](https://github.com/Day8/re-frame/wiki/A-Larger-App). There
are two differences to the tutorial: the components are not generated using
Hiccup, but using [Kioo](https://github.com/ckirkendall/kioo); more on this
later. And the `app-db` is not a map, but a
[DataScript](https://github.com/tonsky/datascript) database; see
[theatralia.thomsky](https://github.com/rmoehn/theatralia/blob/src/cljs/theatralia/thomsky.cljs).

On to the story:

 1. You enter `http://<your-theatralia-server>/html/try_it_out.html` in the
    browser.

 2. The browser sends

    ```
    GET /html/try_it_out.html
    ```

    to the server.

 3. There is no entry for this in `make-handler`, so it looks for
    [resources/public/html/try_it_out.html](https://github.com/rmoehn/theatralia/tree/master/resources/public/html/try_it_out.html),
    which exists. The server sends it to the browser.

 4. There are many things referenced in the HTML, so the browser will do many
    more GET requests. There are two groups of things requested. I'll go
    through them one by one.

 5. [Bootstrap](http://getbootstrap.com/) is a framework for styling web
    UIs. The browser requests and the server sends `bootstrap.min.css` and
    `bootstrap.min.js` from the static files. Those are responsible for making
    the page look how it does. The Bootstrap JavaScript code requires jQuery,
    which the browser requests from an external server. You use Boostrap mainly
    through CSS by putting HTML elements in classes like `.col-xs-4`.

 7. `main.js` is generated by the ClojureScript compiler from our ClojureScript
    sources. ClojureScript relies on the [Google Closure
    Tools](https://developers.google.com/closure/) for JavaScript optimization
    and a large libary of common JavaScript tools. `base.js` is one of them and
    necessary to run our application. This completes the loading of stuff by the
    browser.

 8. `goog.require("theatralia.core")` starts our client application, which is
    held together by
    [src/cljs/theatralia/core.cljs](https://github.com/rmoehn/theatralia/blob/master/src/cljs/theatralia/core.cljs).
    Therefore we will continue by looking at what happens when this file is
    loaded.

 9. First, the `ns` form is evaluated. The `:require` causes all the views,
    subscription and event handlers to be loaded. (If you don't know what they
    are, read about [re-frame](https://github.com/Day8/re-frame), as I
    recommended above.) The `:initialize` event causes the database to be set
    up. `reagent/render` mounts the application into the `div` with ID `app` in
    `try_it_out.html`.

 10. The application is constructed as a tree of components (also called views),
     which are defined in the file
     [src/cljs/theatralia/views.cljs](../src/cljs/theatralia/views.cljs). A
     component is a function returning
     [Hiccup](https://github.com/weavejester/hiccup) data that gets rendered in
     the browser. Or a function returning such a function. See
     [here](https://github.com/Day8/re-frame/wiki/Creating-Reagent-Components)
     for more information on the different types of Reagent components.

     We write down Hiccup directly, but leave the job of generating it to Kioo.
     Kioo is similar to Enlive in that you write the code defining the UI in
     HTML and then change the contents and attributes of elements to reflect the
     current state of your application.

     This process starts in `root-view`, the root of the component tree. It
     loads the template
     [resources/templates/sandbox.html](../resources/templates/sandbox.html). It
     finds the elements with IDs `search-field`, `search-results` and
     `add-material-form` and substitutes their contents with the corresponding
     components. But even though, for example, the element with ID
     `search-results` gets substituted with `result-view`, it largely stays the
     same, because `result-view` is based on the `search-results` element. It
     only puts in different contents. These contents are manufactured by
     `result-item`, which constructs one item of the contents by reading an item
     from the template and swapping in the title of a search result.

     > #### Example
     >
     > ```
     > Template:                           With current state:
     >
     > ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓   ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     > ┃                               ┃   ┃                               ┃
     > ┃ Search your materials!        ┃   ┃ Search your materials!        ┃
     > ┃ ┌───────────────────────┐     ┃   ┃ ┌───────────────────────┐     ┃
     > ┃ │                       │ Go! ┃   ┃ │ food                  │ Go! ┃
     > ┃ └───────────────────────┘     ┃   ┃ └───────────────────────┘     ┃
     > ┃                               ┃   ┃                               ┃
     > ┃ - Svend Åge Madsen: Tugt …    ┃   ┃ - Jane Grigson: English …     ┃
     > ┃ - Hans Christian Andersen: …  ┃   ┃ - River Cottage Veg Everyday! ┃
     > ┃                               ┃   ┃ - On Food and Cooking         ┃
     > ┃ Add material                  ┃   ┃                               ┃
     > ┃ …                             ┃   ┃ Add material                  ┃
     > ┃                               ┃   ┃ …                             ┃
     > ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛   ┃                               ┃
     >                                     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     >
     > ```
     >
     > The basic structure stays the same. Elements look the same as in the
     > template, only contents are changed. And, invisibly, event handlers are
     > added.

 11. The rest of the user interface is constructed in a similar way.

### Doing a search

This picks up where the last section ended.

 1. You've loaded the sandbox. You click on the search input field. (It might
    have been active before.) You hit `f`.

 2. The `onChange` listener of the `searchInput` element fires. It calls
    `handle-change` with the event, the key `:text` and the component owning the
    `searchInput` element.

 3. `handle-change` sets the value of the local state of the owning component at
    `:text` to the value of the input element. The local state now is `{:text
    "f"}`.

 4. Since `set-state!` triggers a re-render, `search-view`'s `render-state` is
    called again. We set the `value` attribute of the `searchInput` element to
    `(:text local-state)`, so the `f` you typed appears in the input field.

 5. You continue typing `o`, `o`, `d` and behind the scenes the local state is
    changed to `{:text "food"}` in the same way as described for the first
    letter..

 6. You hit `Enter`. The `onKeyDown` handler of the `searchInput` element fires
    and calls `process-input` with the owning component as an argument.

 7. `process-input` reads the current value of the input field, which is »food«,
    and sends an XML HTTP request of

    ```
    GET /gq/food
    ```

    to the server. »gq« stands for »general query«.

 8. `make-handler` does have a route for `/gq/`. It extracts the text passed
    in the second component of the path and calls `theatralia.routes/search-for`
    with this text and a reference to the database connection as arguments.

 9. `search-for` asks the database to perform a fulltext search on the material
    titles for »food«. We're using Datomic, so the query is formulated as
    a form of [Datalog](http://docs.datomic.com/query.html).

 9. It finds two titles (from
    [resources/database/sample_data.edn](https://github.com/rmoehn/theatralia/blob/master/resources/database/sample_data.edn)),
    represented as a set of maps. It sorts them alphabetically, wraps them in a
    response map and returns them.

 10. The result is sent to the client as an EDN string. EDN is a format often
     used for serializing Clojure data structures and looks just like regular
     Clojure code.

 11. When the client receives the result, the `:on-complete` function defined in
     `process-input` is called. It puts the data into the `:search-result` part
     of the `app-state`.

 12. Since `result-view` observes the `:search-result` part of `app-state`, it
     gets notified by the change. On re-render, it constructs an Om component
     for every item in the `:search-result` part, so that they get displayed to
     you.

### Adding a material

This is similar to the [previous](#doing-a-search) section in that we have the
ClojureScript client actively communicating with the server. However, this time
we're doing a POST request and the server modifies the database.

 1. You type "Jane Grigson: English Food" into the title input field. You type
    "No pictures, but great recipes!" into the comment input field. The local
    state of the `add-material-view` gets updated (see [above](#doing-a-search))
    and now looks like this:

    ```
    {:title "Jane Grigson: English Food"
     :comments "No pictures, but great recipes!"}
    ```

 2. You click the submit button for adding materials. The `onClick` listener
    fires and calls `process-new-material`.

 3. `process-new-material` takes the local state and sends it to the server in a
    POST request to `/materials`.

 4. The server receives the request with the data encoded as an EDN string. It
    extracts them and gives them to `save-material`.

 5. `save-material` calls some functions with the given material to construct a
    data structure that describes the transaction for adding the material. It
    tries to make the database execute this transaction. Depending on the result
    of the transaction, it signals OK or an error back to the client.

 6. If the client receives an error message from the server, it just prints it
    and leaves the text you wrote in the input fields. If the client receives an
    OK, it empties the local state of the `add-material-view`, causing the input
    fields in the user interface to be emptied as well. Now your new material is
    saved in the database and can be requested through the search.

## The components of the server

The server consist of several parts, which I call components, since they
are taken care of by the framework
[Component](https://github.com/stuartsierra/component). See the links in
[README](https://github.com/rmoehn/theatralia#notes-from-the-start) for more
detailed information.

Currently we have four components:

 0. A logging component. It just sets and resets logging configuration:
    [src/clj/theatralia/logging.clj](https://github.com/rmoehn/theatralia/blob/master/src/clj/theatralia/logging.clj)
 1. The Jetty HTTP server running inside the server process. It is defined in
    [src/clj/theatralia/web_server.clj](https://github.com/rmoehn/theatralia/blob/master/src/clj/theatralia/web_server.clj).
 2. The component responsible for the connection to the database. It is defined
    in
    [src/clj/theatralia/database/component.clj](https://github.com/rmoehn/theatralia/blob/master/src/clj/theatralia/database/component.clj)
 3. A static component for routing. See
    [src/clj/theatralia/routes.clj](https://github.com/rmoehn/theatralia/blob/master/src/clj/theatralia/routes.clj).

For starting and stopping the server during development you run `(go)` and
`(stop)` at the REPL. These are defined in
[dev/user.clj](https://github.com/rmoehn/theatralia/blob/master/dev/user.clj).
`(start)` starts the components in the right order (taking care of dependencies
between components). It first sets the logging configuration straight. Then it
turns on the database component, which sets up the connection to our Datomic
transactor and does further setup steps if necessary. It creates the routing
component, providing it with access to the database component. Finally it
switches on the Jetty HTTP server, giving it a reference to the routing
component. `(stop)` stops the components in the reverse order.

When you have changed some code, you use `(reset)` to stop the system, reload
everything that has changed in the proper way and start it again.
