(ns anvil.item-on-cursor
  (:require [anvil.graphics :as g]
            [anvil.stage :refer [mouse-on-actor?]]
            [anvil.math.vector :as v]))

(defn world-item? []
  (not (mouse-on-actor?)))

; It is possible to put items out of sight, losing them.
; Because line of sight checks center of entity only, not corners
; this is okay, you have thrown the item over a hill, thats possible.

(defn- placement-point [player target maxrange]
  (v/add player
         (v/scale (v/direction player target)
                  (min maxrange
                       (v/distance player target)))))

(defn item-place-position [entity]
  (placement-point (:position entity)
                   (g/world-mouse-position)
                   ; so you cannot put it out of your own reach
                   (- (:entity/click-distance-tiles entity) 0.1)))
