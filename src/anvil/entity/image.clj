(ns ^:no-doc anvil.entity.image
  (:require [anvil.entity :as entity]
            [clojure.utils :refer [defmethods]]
            [gdl.context :as c]))

(defmethods :entity/image
  (entity/render-default [[_ image] entity c]
    (c/draw-rotated-centered c
                             image
                             (or (:rotation-angle entity) 0)
                             (:position entity))))
