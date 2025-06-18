(ns cdq.game-state.create-world
  (:require [cdq.world :as world]))

(defn do!
  [{:keys [ctx/config]
    :as ctx}]
  (let [level (let [[f params] (:config/starting-world config)]
                (f ctx params))]
    (assoc ctx :ctx/world (world/create (:cdq.ctx.game/world config)
                                        level))))
