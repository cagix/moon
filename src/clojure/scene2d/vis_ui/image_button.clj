(ns clojure.scene2d.vis-ui.image-button
  (:require [cdq.ui.tooltip :as tooltip]
            [cdq.ui.table :as table]
            [clojure.vis-ui.image-button :as image-button])
  (:import (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.scenes.scene2d.utils ChangeListener
                                                  Drawable
                                                  TextureRegionDrawable)))

(defn create
  [{:keys [^TextureRegion drawable/texture-region
           on-clicked
           drawable/scale]
    :as opts}]
  (let [scale (or scale 1)
        [w h] [(.getRegionWidth  texture-region)
               (.getRegionHeight texture-region)]
        drawable (doto (TextureRegionDrawable. texture-region)
                   (.setMinSize (float (* scale w)) (float (* scale h))))
        image-button (image-button/create drawable)]
    (when on-clicked
      (.addListener image-button (proxy [ChangeListener] []
                                   (changed [event actor]
                                     (on-clicked actor (.ctx (.getStage event)))))))
    (when-let [tooltip (:tooltip opts)]
      (tooltip/add! image-button tooltip))
    (table/set-opts! image-button opts)))
