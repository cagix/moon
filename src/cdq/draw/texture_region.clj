(ns cdq.draw.texture-region
  (:require [cdq.draws-impl :refer [batch-draw!
                                    texture-region-drawing-dimensions]]))

(defn draw!
  [[_ texture-region position]
   {:keys [batch]}]
  (batch-draw! batch
               texture-region
               position
               (texture-region-drawing-dimensions graphics texture-region)
               0))
