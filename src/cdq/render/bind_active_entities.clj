(ns cdq.render.bind-active-entities
  (:require [cdq.g :as g]))

(defn do! [ctx]
  (assoc ctx :ctx/active-entities (g/get-active-entities ctx)))
