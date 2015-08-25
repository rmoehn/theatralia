(ns theatralia.utils
  "Macros for the Theatralia frontend.")

(defmacro register-handler*
  "Convenience for registering event handlers in re-frame.

  This macro shortens (theatralia.thomsky/register-handler :the-handler
  the-handler) to (theatralia.utils/register-handler* the-handler).

  HANDLER-SYM has to be a symbol denoting the function to be used as handler.
  The dispatch value will be a keyword of the same name. Placing middleware is
  supported.

  Instead of returning register-handler's return value, returns
  :register-handler*-end in order not to obscure warnings about undefined
  handler functions at the REPL.

  See the :voyt.ext/macro-requires in the extension metadata for a list of
  namespaces that have to be required in a namespace where this macro is used."
  {:grenada.cmeta/bars {:voyt.ext/requires ['theatralia.thomsky]}}
  ([handler-sym] `(register-handler* ~handler-sym []))
  ([handler-sym middleware]
   `(do
      (theatralia.thomsky/register-handler
        ~(keyword handler-sym) ~middleware ~handler-sym)
      :register-handler*-end)))

(defmacro register-sub*
  "Convenience for registering subscription handlers in re-frame.

  This macro shortens (re-frame.core/register-sub :the-sub the-sub) to
  (theatralia.utils/register-sub* the-sub).

  HANDLER-SYM has to be a symbol denoting the function to be used as
  subscription handler. The dispatch value will be a keyword of the same name.

  Instead of returning register-sub's return value, returns :register-sub*-end
  in order not to obscure warnings about undefined handler functions at the
  REPL.

  See the :voyt.ext/macro-requires in the extension metadata for a list of
  namespaces that have to be required in a namespace where this macro is used."
  {:grenada.cmeta/bars {:voyt.ext/requires ['re-frame.core]}}
  [handler-sym]
  `(do
     (re-frame.core/register-sub ~(keyword handler-sym)
       ~handler-sym)
     :register-sub*-end))
