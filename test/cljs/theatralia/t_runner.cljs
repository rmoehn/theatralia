(ns theatralia.t-runner
  (:require [doo.runner :include-macros true]
            theatralia.t-thomsky))

(doo.runner/doo-tests 'theatralia.t-thomsky)
