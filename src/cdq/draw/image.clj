(ns cdq.draw.image
  (:require [cdq.graphics :as graphics]
            [cdq.draws-impl :refer [batch-draw!
                                    texture-region-drawing-dimensions]]))

(defn draw!
  [[_ image position]
   {:keys [batch]
    :as graphics}]
  (let [texture-region (graphics/image->texture-region graphics image)]
    (batch-draw! batch
                 texture-region
                 position
                 (texture-region-drawing-dimensions graphics texture-region)
                 0)))
