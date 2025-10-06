(ns clojure.scene2d.vis-ui.text-button
  (:require [com.badlogic.gdx.scenes.scene2d.event :as event]
            [clojure.gdx.scenes.scene2d.utils.change-listener :as change-listener]
            [com.kotcrab.vis-ui.widget.vis-text-button :as vis-text-button]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.stage :as stage]
            [clojure.scene2d.ui.table :as table]))

(defn create
  ([text on-clicked]
   (create {:text text
            :on-clicked on-clicked}))
  ([{:keys [text
            on-clicked]
     :as opts}]
   (let [actor (doto (vis-text-button/create text)
                 (.addListener (change-listener/create
                                (fn [event actor]
                                  (on-clicked actor (stage/get-ctx (event/stage event))))))
                 (table/set-opts! opts))]
     (when-let [tooltip (:tooltip opts)]
       (actor/add-tooltip! actor tooltip))
     actor)))
