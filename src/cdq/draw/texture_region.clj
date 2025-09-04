(ns cdq.draw.texture-region
  (:import (com.badlogic.gdx.graphics.g2d Batch
                                          TextureRegion)))

(defn- draw!* [^Batch batch texture-region x y [w h] rotation]
  (.draw batch
         texture-region
         x
         y
         (/ (float w) 2) ; origin-x
         (/ (float h) 2) ; origin-y
         w
         h
         1 ; scale-x
         1 ; scale-y
         rotation))

(defn draw!
  [[_ ^TextureRegion texture-region [x y] {:keys [center? rotation]}]
   {:keys [g/batch
           g/unit-scale
           g/world-unit-scale]}]
  (let [[w h] (let [dimensions [(.getRegionWidth  texture-region)
                                (.getRegionHeight texture-region)]]
                (if (= @unit-scale 1)
                  dimensions
                  (mapv (comp float (partial * world-unit-scale))
                        dimensions)))]
    (if center?
      (draw!* batch
              texture-region
              (- (float x) (/ (float w) 2))
              (- (float y) (/ (float h) 2))
              [w h]
              (or rotation 0))
      (draw!* batch
              texture-region
              x
              y
              [w h]
              0))))
