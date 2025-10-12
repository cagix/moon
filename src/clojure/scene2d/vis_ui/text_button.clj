(ns clojure.scene2d.vis-ui.text-button
  (:require [cdq.ui.tooltip :as tooltip]
            [cdq.ui.table :as table]
            [clojure.vis-ui.text-button :as text-button] )
  (:import (com.badlogic.gdx.scenes.scene2d.utils ChangeListener)))

(defn create
  ([text on-clicked]
   (create {:text text
            :on-clicked on-clicked}))
  ([{:keys [text
            on-clicked]
     :as opts}]
   (let [actor (doto (text-button/create text)
                 (.addListener (proxy [ChangeListener] []
                                 (changed [event actor]
                                   (on-clicked actor (.ctx (.getStage event))))))
                 (table/set-opts! opts))]
     (when-let [tooltip (:tooltip opts)]
       (tooltip/add! actor tooltip))
     actor)))
