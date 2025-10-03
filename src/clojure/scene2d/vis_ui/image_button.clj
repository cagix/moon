(ns clojure.scene2d.vis-ui.image-button
  (:require [com.kotcrab.vis.ui.widget.vis-image-button :as vis-image-button]
            [com.badlogic.gdx.graphics.g2d.texture-region :as texture-region]
            [gdl.scene2d.actor :as actor]
            [com.badlogic.gdx.scenes.scene2d.event :as event]
            [gdl.scene2d.stage :as stage]
            [clojure.scene2d.vis-ui.table :as table]
            [com.badlogic.gdx.scenes.scene2d.utils.drawable :as drawable]
            [com.badlogic.gdx.scenes.scene2d.utils.listener :as listener]))

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
      (.addListener image-button (listener/change
                                  (fn [event actor]
                                    (on-clicked actor (stage/get-ctx (event/stage event)))))))
    (when-let [tooltip (:tooltip opts)]
      (actor/add-tooltip! image-button tooltip))
    (table/set-opts! image-button opts)))
