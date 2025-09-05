(ns cdq.ui.image-button
  (:require [cdq.ui.utils :as utils])
  (:import (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.scenes.scene2d.utils Drawable)
           (com.kotcrab.vis.ui.widget VisImageButton)))

(defn create [{:keys [^TextureRegion texture-region on-clicked scale]}]
  (let [scale (or scale 1)
        [w h] [(.getRegionWidth  texture-region)
               (.getRegionHeight texture-region)]
        drawable (utils/drawable texture-region
                                 :width  (* scale w)
                                 :height (* scale h))
        image-button (VisImageButton. ^Drawable drawable)]
    (when on-clicked
      (.addListener image-button (utils/change-listener on-clicked)))
    image-button))
