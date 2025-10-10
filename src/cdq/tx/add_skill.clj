(ns cdq.tx.add-skill
  (:require [cdq.graphics.textures :as textures]
            [cdq.ui :as ui]
            [cdq.world.info :as info]))

(defn do!
  [{:keys [ctx/graphics
           ctx/stage]}
   eid skill]
  (when (:entity/player? @eid)
    (ui/add-skill! stage
                   {:skill-id (:property/id skill)
                    :texture-region (textures/texture-region graphics (:entity/image skill))
                    :tooltip-text (fn [{:keys [ctx/world]}]
                                    (info/text skill world))})
    nil))
