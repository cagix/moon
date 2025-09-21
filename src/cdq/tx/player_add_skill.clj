(ns cdq.tx.player-add-skill
  (:require [cdq.ctx :as ctx]
            [cdq.graphics :as graphics]
            [cdq.stage :as stage]))

(defn do!
  [{:keys [ctx/graphics
           ctx/stage]}
   skill]
  (stage/add-skill! stage
                    {:skill-id (:property/id skill)
                     :texture-region (graphics/texture-region graphics (:entity/image skill))
                     :tooltip-text (fn [ctx]
                                     (ctx/info-text ctx skill))})
  nil)

