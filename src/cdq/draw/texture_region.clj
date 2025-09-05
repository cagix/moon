(ns cdq.draw.texture-region
  (:require [clojure.gdx.graphics.g2d.batch :as batch]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]))

(defn draw!
  [[_ texture-region [x y] {:keys [center? rotation]}]
   {:keys [ctx/batch
           ctx/unit-scale
           ctx/world-unit-scale]}]
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
