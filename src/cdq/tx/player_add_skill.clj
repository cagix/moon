(ns cdq.tx.player-add-skill
  (:require [cdq.image :as image]
            [cdq.info :as info]
            [cdq.stage :as stage]))

(defn do!
  [{:keys [ctx/textures
           ctx/stage]}
   skill]
  (stage/add-skill! stage
                    {:skill-id (:property/id skill)
                     :texture-region (image/texture-region (:entity/image skill) textures)
                     :tooltip-text (fn [ctx]
                                     (info/generate (:ctx/info ctx) skill ctx))})
  nil)

