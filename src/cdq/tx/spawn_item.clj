(ns cdq.tx.spawn-item
  (:require [cdq.ctx :as ctx]
            [cdq.world :as world]))

(defn do! [position item]
  (world/spawn-entity! ctx/world
                       position
                       {:width 0.75
                        :height 0.75
                        :z-order :z-order/on-ground}
                       {:entity/image (:entity/image item)
                        :entity/item item
                        :entity/clickable {:type :clickable/item
                                           :text (:property/pretty-name item)}}))
