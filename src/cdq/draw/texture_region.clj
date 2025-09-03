(ns cdq.draw.texture-region
  (:require [cdq.draws-impl :refer [batch-draw!]])
  (:import (com.badlogic.gdx.graphics.g2d TextureRegion)))

(defn draw!
  [[_ ^TextureRegion texture-region [x y]]
   {:keys [batch]}]
  (batch-draw! batch
               texture-region
               [x y]
               [(.getRegionWidth  texture-region)
                (.getRegionHeight texture-region)]
               0))
