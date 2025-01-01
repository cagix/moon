(ns ^:no-doc anvil.entity.image
  (:require [anvil.entity :as entity]
            [clojure.component :refer [defcomponent]]
            [gdl.context :as c]))

(defcomponent :entity/image
  (entity/render-default [[_ image] entity c]
    (c/draw-rotated-centered c
                             image
                             (or (:rotation-angle entity) 0)
                             (:position entity))))
