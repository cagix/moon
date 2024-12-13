(ns anvil.entity.image
  (:require [anvil.component :as component]
            [gdl.graphics :as g]
            [gdl.utils :refer [defmethods]]))

(defmethods :entity/image
  (component/render-default [[_ image] entity]
    (g/draw-rotated-centered image
                             (or (:rotation-angle entity) 0)
                             (:position entity))))
