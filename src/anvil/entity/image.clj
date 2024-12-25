(ns ^:no-doc anvil.entity.image
  (:require [anvil.component :as component]
            [gdl.context :as c]))

(defmethods :entity/image
  (component/render-default [[_ image] entity c]
    (c/draw-rotated-centered c
                             image
                             (or (:rotation-angle entity) 0)
                             (:position entity))))
