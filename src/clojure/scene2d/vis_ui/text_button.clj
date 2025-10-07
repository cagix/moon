(ns clojure.scene2d.vis-ui.text-button
  (:require [cdq.ui.tooltip :as tooltip]
            [clojure.gdx.scenes.scene2d.event :as event]
            [cdq.ui.stage :as stage]
            [clojure.scene2d.ui.table :as table])
  (:import (com.badlogic.gdx.scenes.scene2d.utils ChangeListener)
           (com.kotcrab.vis.ui.widget VisTextButton)))

(defn create
  ([text on-clicked]
   (create {:text text
            :on-clicked on-clicked}))
  ([{:keys [text
            on-clicked]
     :as opts}]
   (let [actor (doto (VisTextButton. (str text))
                 (.addListener (proxy [ChangeListener] []
                                 (changed [event actor]
                                   (on-clicked actor (stage/get-ctx (event/stage event))))))
                 (table/set-opts! opts))]
     (when-let [tooltip (:tooltip opts)]
       (tooltip/add! actor tooltip))
     actor)))
