(ns cdq.render.update-ui
  (:require [cdq.g :as g]))

(defn do! [ctx]
  (g/update-stage! ctx))
