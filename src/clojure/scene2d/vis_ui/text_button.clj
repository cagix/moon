(ns clojure.scene2d.vis-ui.text-button
  (:require [gdl.scene2d.actor :as actor]
            [com.badlogic.gdx.scenes.scene2d.event :as event]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]
            [clojure.scene2d.vis-ui.table :as table]
            [com.badlogic.gdx.scenes.scene2d.utils.listener :as listener]
            [com.kotcrab.vis.ui.widget.vis-text-button :as vis-text-button]))

(defn create
  ([text on-clicked]
   (create {:text text
            :on-clicked on-clicked}))
  ([{:keys [text
            on-clicked]
     :as opts}]
   (let [actor (doto (vis-text-button/create text)
                 (.addListener (listener/change
                                (fn [event actor]
                                  (on-clicked actor (stage/get-ctx (event/stage event))))))
                 (table/set-opts! opts))]
     (when-let [tooltip (:tooltip opts)]
       (actor/add-tooltip! actor tooltip))
     actor)))
