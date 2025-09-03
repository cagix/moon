(ns cdq.draw.rotated-centered
  (:require [cdq.textures :as textures]
            [cdq.draws-impl :refer [batch-draw!
                                    texture-region-drawing-dimensions]]))

(defn draw!
  [[_ image rotation [x y]]
   {:keys [batch
           textures]
    :as graphics}]
  (let [texture-region (textures/image->texture-region textures image)
        [w h] (texture-region-drawing-dimensions graphics texture-region)]
    (batch-draw! batch
                 texture-region
                 [(- (float x) (/ (float w) 2))
                  (- (float y) (/ (float h) 2))]
                 [w h]
                 rotation)))
