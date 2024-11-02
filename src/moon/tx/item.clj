(ns moon.tx.item
  (:require [moon.component :as component]))

(def ^:private body-props
  {:width 0.75
   :height 0.75
   :z-order :z-order/on-ground})

(defmethods :tx/item
  (component/handle [[_ position item]]
    [[:e/create position body-props {:entity/image (:entity/image item)
                                     :entity/item item
                                     :entity/clickable {:type :clickable/item
                                                        :text (:property/pretty-name item)}}]]))
