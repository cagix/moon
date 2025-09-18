(ns com.kotcrab.vis.ui.widget.image-button
  (:require [clojure.scene2d.event :as event]
            [clojure.scene2d.stage :as stage]
            [clojure.scene2d.ui.table :as table]
            [com.badlogic.gdx.graphics.g2d.texture-region :as texture-region]
            [clojure.gdx.scene2d.utils.drawable :as drawable]
            [clojure.gdx.scene2d.utils.listener :as listener]
            [com.kotcrab.vis.ui.widget.tooltip :as tooltip])
  (:import (com.badlogic.gdx.scenes.scene2d.utils Drawable)
           (com.kotcrab.vis.ui.widget VisImageButton)))

(defn create
  [{:keys [drawable/texture-region
           on-clicked
           drawable/scale]
    :as opts}]
  (let [scale (or scale 1)
        [w h] (texture-region/dimensions texture-region)
        drawable (drawable/create texture-region
                                  :width  (* scale w)
                                  :height (* scale h))
        image-button (VisImageButton. ^Drawable drawable)]
    (when on-clicked
      (.addListener image-button (listener/change
                                  (fn [event actor]
                                    (on-clicked actor (stage/get-ctx (event/stage event)))))))
    (when-let [tooltip (:tooltip opts)]
      (tooltip/add! image-button tooltip))
    (table/set-opts! image-button opts)))
