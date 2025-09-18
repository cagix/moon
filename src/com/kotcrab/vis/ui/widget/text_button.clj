(ns com.kotcrab.vis.ui.widget.text-button
  (:require [clojure.scene2d.event :as event]
            [clojure.scene2d.stage :as stage]
            [clojure.scene2d.ui.table :as table]
            [clojure.gdx.scene2d.utils.listener :as listener]
            [com.kotcrab.vis.ui.widget.tooltip :as tooltip])
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
       (tooltip/add! actor tooltip))
     actor)))
