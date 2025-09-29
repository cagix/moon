(ns gdl.scene2d.vis-ui.text-button
  (:require [gdl.scene2d.actor :as actor]
            [gdl.scene2d.event :as event]
            [gdl.scene2d.stage :as stage]
            [gdl.scene2d.ui.table :as table]
            [gdl.scene2d.utils.listener :as listener])
  (:import (com.kotcrab.vis.ui.widget VisTextButton)))

(defn create
  ([text on-clicked]
   (create {:text text
            :on-clicked on-clicked}))
  ([{:keys [text
            on-clicked]
     :as opts}]
   (let [actor (doto (VisTextButton. (str text))
                 (.addListener (listener/change
                                (fn [event actor]
                                  (on-clicked actor (stage/get-ctx (event/stage event))))))
                 (table/set-opts! opts))]
     (when-let [tooltip (:tooltip opts)]
       (actor/add-tooltip! actor tooltip))
     actor)))
