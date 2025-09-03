(ns cdq.draw.rotated-centered
  (:require [cdq.graphics :as graphics]
            [cdq.draws-impl :refer [batch-draw!
                                    texture-region-drawing-dimensions]]))

(defn draw!
  [[_ image rotation [x y]]
   {:keys [batch]
    :as graphics}]
  (let [texture-region (graphics/image->texture-region graphics image)
        [w h] (texture-region-drawing-dimensions graphics texture-region)]
    (batch-draw! batch
                 texture-region
                 [(- (float x) (/ (float w) 2))
                  (- (float y) (/ (float h) 2))]
                 [w h]
                 rotation)))
