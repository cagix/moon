(ns ^:no-doc anvil.entity.image
  (:require [clojure.component :as component :refer [defcomponent]]
            [gdl.context :as c]))

(defcomponent :entity/image
  (component/render-default [[_ image] entity c]
    (c/draw-rotated-centered c
                             image
                             (or (:rotation-angle entity) 0)
                             (:position entity))))
