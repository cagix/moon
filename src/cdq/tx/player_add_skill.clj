(ns cdq.tx.player-add-skill
  (:require [cdq.ctx.graphics :as graphics]
            [cdq.info :as info]
            [cdq.ctx.stage :as stage]))

(defn do!
  [{:keys [ctx/graphics
           ctx/stage]}
   skill]
  (stage/add-skill! stage
                    {:skill-id (:property/id skill)
                     :texture-region (graphics/texture-region graphics (:entity/image skill))
                     :tooltip-text (fn [ctx]
                                     (info/generate (:ctx/info ctx) skill ctx))})
  nil)

