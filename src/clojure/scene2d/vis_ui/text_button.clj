(ns clojure.scene2d.vis-ui.text-button
  (:require [cdq.ui.tooltip :as tooltip]
            [cdq.ui.table :as table]
            [cdq.ui.stage :as stage]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.event :as event]
            [clojure.gdx.scene2d.utils.change-listener :as change-listener]
            [clojure.vis-ui.text-button :as text-button])
  (:import (com.badlogic.gdx.scenes.scene2d.utils ChangeListener)))

(defn create
  ([text on-clicked]
   (create {:text text
            :on-clicked on-clicked}))
  ([{:keys [text
            on-clicked]
     :as opts}]
   (let [actor (doto (text-button/create text)
                 (actor/add-listener!
                  (change-listener/create
                    (fn [event actor]
                      (on-clicked actor (stage/ctx (event/stage event))))))
                 (table/set-opts! opts))]
     (when-let [tooltip (:tooltip opts)]
       (tooltip/add! actor tooltip))
     actor)))
