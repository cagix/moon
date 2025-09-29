(ns clojure.scene2d.vis-ui.image-button
  (:require [com.kotcrab.vis.ui.widget.vis-image-button :as vis-image-button]
            [clojure.graphics.texture-region :as texture-region]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.event :as event]
            [clojure.scene2d.stage :as stage]
            [clojure.scene2d.ui.table :as table]
            [clojure.scene2d.utils.drawable :as drawable]
            [clojure.scene2d.utils.listener :as listener]))

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
