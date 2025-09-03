(ns cdq.draw.image
  (:require [cdq.textures :as textures]
            [cdq.draws-impl :refer [batch-draw!
                                    texture-region-drawing-dimensions]]))

(defn draw!
  [[_ image position]
   {:keys [batch
           textures]
    :as graphics}]
  (let [texture-region (textures/image->texture-region textures image)]
    (batch-draw! batch
                 texture-region
                 position
                 (texture-region-drawing-dimensions graphics texture-region)
                 0)))
