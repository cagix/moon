(ns cdq.ui.text-button
  (:require [cdq.ui.table :as table]
            [clojure.gdx.scenes.scene2d.utils :as utils]
            [clojure.vis-ui.text-button :as text-button]
            [clojure.vis-ui.tooltip :as tooltip]))

(defn create
  ([text on-clicked]
   (create {:text text
            :on-clicked on-clicked}))
  ([{:keys [text
            on-clicked]
     :as opts}]
   (let [actor (doto (text-button/create text)
                 (.addListener (utils/change-listener on-clicked))
                 (table/set-opts! opts))]
     (when-let [tooltip (:tooltip opts)]
       (tooltip/add! actor tooltip))
     actor)))
