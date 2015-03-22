[![Build Status](https://travis-ci.org/rmoehn/theatralia.svg?branch=master)](https://travis-ci.org/rmoehn/theatralia)

# theatralia

This application is under development and not usable yet. If you're interested
in collaborating, read the documentation (which is becoming quite extensive) and
contact me.

The project uses [Midje](https://github.com/marick/Midje/).

## How to run the tests (introduced by template)

`lein midje` will run all tests.

`lein midje namespace.*` will run only tests beginning with "namespace.".

`lein midje :autotest` will run all the tests indefinitely. It sets up a
watcher on the code files. If they change, only the relevant tests will be
run again.

## Documentation overview

 1. This README. Lots of different things I wrote down when they came to my
    mind. Provides you with some context. Might be cleaned up in the future.
 2. Comments and doc strings in the source code. At the moment rather sparse,
    but get beefed up whenever a region of the code becomes reasonably stable.
 3. Commit log. I am a fan of informative commit messages, so you will see many
    of my thoughts and decisions documented there.
 3. [doc/running.md](https://github.com/rmoehn/theatralia/blob/master/doc/running.md)
    – Shows how to start the system for development.
 4. [doc/system-overview.md](https://github.com/rmoehn/theatralia/blob/master/doc/system-overview.md)
    – To be written.

## Notes from the start

I looked at current ways to set up a Clojure web application. The most
prominent thing applicable to this application (mainly server-side
stuff) appeared [Luminus](http://www.luminusweb.net/). It claims to be
minimal, but when I look at the Leiningen template, it still sets up a
lot of stuff that I'd like to set up step by step. Additionally it talks
about some things I wouldn't use in my project, most prominently Selmer
for templating and non-Datomic databases. It doesn't talk about Datomic
anywhere, which makes me suspicious. However, Luminus looks like a good
orientation for some things, so probably it will will appear in the
[Credits section](#credits).

A second thing you should know is Stuart Sierra's
[component](https://github.com/stuartsierra/component) framework.
Consume these:

 - [Stuart Sierra: Components. Just Enough Structure](http://youtu.be/13cmHf_kt-Q)
 - http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded

## Credits <a name="credits"></a>

The previous section already named some resources I used for my work. Most of
the source files will also contain a Credits section referencing places on the
web (mainly), where I found snippets or hints about how to write a particular
piece of code.

## Database

I'm using [Datomic Pro Starter Edition](http://www.datomic.com/) as a database
system. I wasn't entirely sure whether to use this or Datomic Free, but all the
Datomic pages shouted that I should use DPSE, so I gave in. Victim of marketing?
It does make the whole thing a bit clumsier. Especially typing my PGP password
every time I start a REPL is annoying. However, I didn't buy in to too much,
since all it takes to switch to Datomic Free is changing a Leiningen dependency
and the connection URI. That's also what you could do in order to test locally
if you don't already have Datomic Pro Starter Edition. You should go and get it,
though! Just kidding.

## How to test on a local Tomcat

This doesn't work at the moment, but will be fixed sometime.

 1. Run `lein ring uberwar`.
 2. Copy `target/theatralia.war` to your Tomcat's webapps directory, probably
    `/var/lib/tomcat8/webapps/`.
 3. Restart Tomcat.
 4. Point your browser to http://localhost:8080/theatralia. If Tomcat listens on
    a different port, you have to use that, of course.

Credits:

 - https://fitacular.com/blog/clojure/2014/07/14/deploy-clojure-tomcat-nginx/
 - https://github.com/weavejester/lein-ring

## Tags and versioning

`.travis.yml` specifies that every tagged commit gets deployed to GitHub
Releases (and eventually to the web server). Commits that are not tagged don't
get deployed. I'm too lazy to write a full new version number for every version
I want to deploy for testing, so tags will be `t<number>`.

## User interface design

Currently you can see two approaches to producing the user interface. One is the
server-generated approach of the Welcome page with my own styling. The other is
the client-side approach of the »Try it out« playground page with
[Bootstrap](http://getbootstrap.com/) styling. The final design will probably be
a mix of both. However, for now I'll do some functionality prototyping with
vanilla Bootstrap and leave the Welcome page for reference.

### Fonts

The fonts are not under version control, because I don't yet know whether
they'll make it into the final design. So I don't want to litter the repo with
additions and removals of binaries or whatever fonts are.

The fonts used on the Welcome page are:

 - [Theano Didot](http://www.fontsquirrel.com/fonts/Theano-Didot) by Alexey
   Kryukov as a serif font.
 - [Chivo](http://www.omnibus-type.com/) by Omnibus Type as a sans-serif font.

The Big Theta is set in [GFS
Elpis](http://www.greekfontsociety.gr/pages/en_typefaces20th.html) by Natasha
Raissaki, published by the Greek Font Society.

If fonts are not available as WOFF, I use the [Font
Squirrel](http://www.fontsquirrel.com/) webfont generator for conversion and
packaging.

Note that I'm not a font expert at all, so I welcome all suggestions for
improvement.

### Colours of the Welcome page

Right now I'm using parts of [Solarized](http://ethanschoonover.com/solarized)
as a colour scheme, which is easy on the eyes. The problem is that one has to
get used to it. So it might scare off people. The page's flat look came
naturally with the colour scheme (maybe also inspired by [Light
Table](http://lighttable.com/)) and is slightly unusual, too. These things are
by no means final. I just wanted some basis to work with. (In case you're
wondering about the inconsistency of the spelling of »color« in the source code
and »colour« here: I use American Englisch spelling in source code in order to
prevent bugs, since most programmers are accustomed to American English
spelling. Everything else is not as critical, so I stay British.)

### Om, Kioo, Bootstrap

I'm using Om and I'm using Bootstrap, but I'm not using [Om
Bootstrap](http://om-bootstrap.herokuapp.com/). I found that there is a lot of
stuff in Bootstrap that is not in Om Bootstrap and looking at how to use Om
Bootstrap I also didn't see huge benefits in using it. So contributing the parts
I need is out of question.

Instead, since I like [Enlive](https://github.com/cgrand/enlive), I had a look
at [Kioo](https://github.com/ckirkendall/kioo) and from what I've seen I think
that using the trio of Om, Kioo and Bootstrap can be powerful and reasonably
easy. There's no tutorials or anything on this combination though, so if you're
looking for a topic for your next blog post, this might be something.

## Build system

I cobbled together my `project.clj` and corresponding directory layout following
some of David Nolen's templates:
[om-async-tut](https://github.com/swannodette/om-async-tut),
[mies](https://github.com/swannodette/mies) and
[mies-om](https://github.com/swannodette/mies-om). For more information you
should read [The Essence of ClojureScript
Redux](http://swannodette.github.io/2015/01/02/the-essence-of-clojurescript-redux/)
and [Waitin'](http://swannodette.github.io/2014/12/22/waitin/) (and maybe the Om
tutorials).

## ->LIVE-SPEC

Recently (use `git blame` to see when I wrote this) I listened to the [Cognicast
with Aaron Brooks](http://blog.cognitect.com/cognicast/074) as a guest. He
talked about the concept of live specifications, which I found interesting. He
said that there are actually very few projects that wouldn't benefit from it.
While I won't jump on the idea immediately with this project, I will be making
`->LIVE-SPEC` annotations whenever I write something that would be better off
coming from such a live specification rather than being hard-coded. Of course a
first step could also be putting these things in a configuration file or
something similar.

## License

See [LICENSE.txt](https://github.com/rmoehn/theatralia/blob/master/LICENSE.txt).

### Exceptions

The contents of the following directories are not my work and remain under
their original authorship and license.

 - [resources/public/fonts](https://github.com/rmoehn/theatralia/tree/master/resources/public/fonts)
 - [resources/public/bootstrap](https://github.com/rmoehn/theatralia/tree/master/resources/public/bootstrap)

They usually contain their own copyright and license information and, as of now,
they are not under version control anyway.
