(ns cdq.tx.spawn-item
  (:require [cdq.tx.spawn-entity]))

(defn do! [position item]
  (cdq.tx.spawn-entity/do! position
                           {:width 0.75
                            :height 0.75
                            :z-order :z-order/on-ground}
                           {:entity/image (:entity/image item)
                            :entity/item item
                            :entity/clickable {:type :clickable/item
                                               :text (:property/pretty-name item)}}))
