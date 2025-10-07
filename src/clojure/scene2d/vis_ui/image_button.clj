(ns clojure.scene2d.vis-ui.image-button
  (:require [cdq.ui.tooltip :as tooltip]
            [clojure.gdx.scenes.scene2d.event :as event]
            [clojure.gdx.vis-ui.widget.vis-image-button :as vis-image-button]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [cdq.ui.stage :as stage]
            [clojure.scene2d.ui.table :as table])
  (:import (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.scenes.scene2d.utils ChangeListener
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
        image-button (vis-image-button/create drawable)]
    (when on-clicked
      (.addListener image-button (proxy [ChangeListener] []
                                   (changed [event actor]
                                     (on-clicked actor (stage/get-ctx (event/stage event)))))))
    (when-let [tooltip (:tooltip opts)]
      (tooltip/add! image-button tooltip))
    (table/set-opts! image-button opts)))
