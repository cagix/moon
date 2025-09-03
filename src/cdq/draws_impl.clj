(ns cdq.draws-impl
  (:import (com.badlogic.gdx.graphics.g2d Batch
                                          TextureRegion)))

(defn texture-region-drawing-dimensions
  [{:keys [unit-scale
           world-unit-scale]}
   ^TextureRegion texture-region]
  (let [dimensions [(.getRegionWidth  texture-region)
                    (.getRegionHeight texture-region)]]
    (if (= @unit-scale 1)
      dimensions
      (mapv (comp float (partial * world-unit-scale))
            dimensions))))

(defn batch-draw! [^Batch batch texture-region [x y] [w h] rotation]
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
