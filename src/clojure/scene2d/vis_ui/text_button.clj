(ns clojure.scene2d.vis-ui.text-button
  (:require [cdq.ui.tooltip :as tooltip]
            [com.badlogic.gdx.scenes.scene2d.event :as event]
            [com.badlogic.gdx.scenes.scene2d.utils.change-listener :as change-listener]
            [com.kotcrab.vis-ui.widget.vis-text-button :as vis-text-button]
            [com.badlogic.gdx.scenes.scene2d.actor :as actor]
            [cdq.ui.stage :as stage]
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
       (tooltip/add! actor tooltip))
     actor)))
