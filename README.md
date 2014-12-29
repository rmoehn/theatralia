[![Build Status](https://travis-ci.org/rmoehn/theatralia.svg?branch=master)](https://travis-ci.org/rmoehn/theatralia)

# theatralia

The project uses [Midje](https://github.com/marick/Midje/).

## How to run the tests

`lein midje` will run all tests.

`lein midje namespace.*` will run only tests beginning with "namespace.".

`lein midje :autotest` will run all the tests indefinitely. It sets up a
watcher on the code files. If they change, only the relevant tests will be
run again.

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

## How to test on a local Tomcat

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

## Fonts

The fonts are not yet under version control, because I'm not sure what fonts to
use and whether to use custom fonts at all. So I don't want to litter the repo
with additions and removals of binaries or whatever fonts are. At the moment I'm
using [Linux Libertine and Biolinum](http://www.linuxlibertine.org/), but those
look a bit horrible in the browser. If for some reason you want to play around
with Theatralia, just download the WOFF fonts from the website and extract them
into `resource/public/fonts`.

## Colours

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

## License

The MIT License (MIT)

Copyright (c) 2014 Richard Möhn

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

### Exception

The fonts lying in `resources/public/fonts` are not my work and remain under
their original authorship and license. See the contents of the directory for
copyright and licensing information. As of now, they are not under version
control anyway.
