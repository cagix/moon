(ns cdq.tx.spawn-item
  (:require [cdq.ctx.world :as w]))

(defn do! [[_ position item]
           {:keys [ctx/world]}]
  (w/spawn-entity! world
                   {:entity/body {:position position
                                  :width 0.75
                                  :height 0.75
                                  :z-order :z-order/on-ground}
                    :entity/image (:entity/image item)
                    :entity/item item
                    :entity/clickable {:type :clickable/item
                                       :text (:property/pretty-name item)}}))
