(ns cdq.graphics.draw.texture-region
  (:require [clojure.gdx.graphics.g2d.batch :as batch]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]))

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
                   (/ (float w) 2) ; origin-x
                   (/ (float h) 2) ; origin-y
                   w
                   h
                   1 ; scale-x
                   1 ; scale-y
                   (or rotation 0))
      (batch/draw! batch texture-region x y w h))))
