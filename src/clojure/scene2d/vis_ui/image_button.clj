(ns clojure.scene2d.vis-ui.image-button
  (:require [com.badlogic.gdx.graphics.g2d.texture-region :as texture-region]
            [com.badlogic.gdx.scenes.scene2d.event :as event]
            [clojure.gdx.scenes.scene2d.utils.drawable :as drawable]
            [clojure.gdx.scenes.scene2d.utils.change-listener :as change-listener]
            [com.kotcrab.vis-ui.widget.vis-image-button :as vis-image-button]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.stage :as stage]
            [clojure.scene2d.ui.table :as table]))

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
        image-button (vis-image-button/create drawable)]
    (when on-clicked
      (.addListener image-button (change-listener/create
                                  (fn [event actor]
                                    (on-clicked actor (stage/get-ctx (event/stage event)))))))
    (when-let [tooltip (:tooltip opts)]
      (actor/add-tooltip! image-button tooltip))
    (table/set-opts! image-button opts)))
