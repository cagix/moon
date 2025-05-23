(ns cdq.render.remove-destroyed-entities
  (:require [cdq.g :as g]))

(defn do! [ctx]
  (g/remove-destroyed-entities! ctx))
