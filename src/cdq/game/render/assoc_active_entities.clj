(ns cdq.game.render.assoc-active-entities
  (:require [cdq.world :as world]))

(defn step
  [{:keys [ctx/world]
    :as ctx}]
  (update ctx :ctx/world world/cache-active-entities))
