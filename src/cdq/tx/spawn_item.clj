(ns cdq.tx.spawn-item
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/spawn-item [[_ position item] ctx]
  (ctx/spawn-entity! ctx
                     position
                     {:width 0.75
                      :height 0.75
                      :z-order :z-order/on-ground}
                     {:entity/image (:entity/image item)
                      :entity/item item
                      :entity/clickable {:type :clickable/item
                                         :text (:property/pretty-name item)}}))
