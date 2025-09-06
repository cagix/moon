(ns cdq.ui.image-button
  (:require [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [clojure.vis-ui.image-button :as image-button]
            [cdq.ui.utils :as utils]))

(defn create
  [{:keys [drawable/texture-region
           on-clicked
           drawable/scale]}]
  (let [scale (or scale 1)
        [w h] (texture-region/dimensions texture-region)
        drawable (utils/drawable texture-region
                                 :width  (* scale w)
                                 :height (* scale h))
        image-button (image-button/create drawable)]
    (when on-clicked
      (.addListener image-button (utils/change-listener on-clicked)))
    image-button))
