(ns cdq.create.graphics.draw.texture-region
  (:require [gdl.graphics.texture-region :as texture-region]
            [gdl.graphics.g2d.batch :as batch]))

(defn do!
  [{:keys [graphics/batch
           graphics/unit-scale
           graphics/world-unit-scale]}
   texture-region
   [x y]
   & {:keys [center? rotation]}]
  (let [[w h] (let [dimensions (texture-region/dimensions texture-region)]
                (if (= @unit-scale 1)
                  dimensions
                  (mapv (comp float (partial * world-unit-scale))
                        dimensions)))]
    (if center?
      (batch/draw! batch
                   texture-region
                   (- (float x) (/ (float w) 2))
                   (- (float y) (/ (float h) 2))
                   [w h]
                   (or rotation 0))
      (batch/draw! batch
                   texture-region
                   x
                   y
                   [w h]
                   0))))
