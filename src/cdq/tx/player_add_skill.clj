(ns cdq.tx.player-add-skill
  (:require [cdq.gdx.graphics :as graphics]
            [cdq.info :as info]
            [cdq.stage :as stage]))

(defn do!
  [{:keys [ctx/stage]
    :as ctx}
   skill]
  (stage/add-skill! stage
                    {:skill-id (:property/id skill)
                     :texture-region (graphics/texture-region ctx (:entity/image skill))
                     :tooltip-text (fn [ctx]
                                     (info/generate (:ctx/info ctx) skill ctx))})
  nil)

