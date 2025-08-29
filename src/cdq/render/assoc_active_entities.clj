(ns cdq.render.assoc-active-entities
  (:require [cdq.world :as world]))

(defn do! [ctx]
  (update ctx :ctx/world world/cache-active-entities))
