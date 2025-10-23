(ns cdq.tx.set-item
  (:require [cdq.graphics :as graphics]
            [cdq.ui :as ui]
            [cdq.world.info :as info]))

(defn do!
  [{:keys [ctx/graphics
           ctx/stage]}
   eid cell item]
  (when (:entity/player? @eid)
    (ui/set-item! stage cell
                  {:texture-region (graphics/texture-region graphics (:entity/image item))
                   :tooltip-text (info/text item nil)})
    nil))
