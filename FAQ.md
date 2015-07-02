## What is the `:grenada.cmeta/extensions` attribute and `:voyt.ext/requires`?

As part of the Google Summer of Code I am developing a metadata build and
distribution system for Clojure. It is called
[Grenada](https://github.com/clj-grenada). The `:grenada.cmeta/extensions`
attribute allows the programmer to attach structured metadata to a definition.
Those data are processed by different extensions plugging in to a Grenada build.

`:voyt.ext/requires` is a hypothetical extension key. You can attach it to a
`defmacro` and as its value list the namespaces that the user of the macro needs
to `require` in the namespace where it is called.
