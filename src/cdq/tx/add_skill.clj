(ns cdq.tx.add-skill
  (:require [cdq.graphics :as graphics]
            [cdq.ui :as ui]
            [cdq.world.info :as info]))

(defn do!
  [{:keys [ctx/graphics
           ctx/stage]
    :as ctx}
   eid skill]
  (when (:entity/player? @eid)
    (ui/add-skill! stage
                   {:skill-id (:property/id skill)
                    :texture-region (graphics/texture-region graphics (:entity/image skill))
                    :tooltip-text (fn [{:keys [ctx/world]}]
                                    (info/text skill world))}))
  ctx)
