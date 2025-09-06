(ns cdq.ui.image-button
  (:require [cdq.ui.table :as table]
            [cdq.ui.utils :as utils]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [clojure.vis-ui.image-button :as image-button]))

(defn create
  [{:keys [drawable/texture-region
           on-clicked
           drawable/scale]
    :as opts}]
  (let [scale (or scale 1)
        [w h] (texture-region/dimensions texture-region)
        drawable (utils/drawable texture-region
                                 :width  (* scale w)
                                 :height (* scale h))
        image-button (image-button/create drawable)]
    (when on-clicked
      (.addListener image-button (utils/change-listener on-clicked)))
    (table/set-opts! image-button opts)))
