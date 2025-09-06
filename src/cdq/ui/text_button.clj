(ns cdq.ui.text-button
  (:require [cdq.ui.table :as table]
            [cdq.ui.utils :as utils]
            [clojure.vis-ui.text-button :as text-button]))

(defn create
  ([text on-clicked]
   (create {:text text
            :on-clicked on-clicked}))
  ([{:keys [text
            on-clicked]
     :as opts}]
   (doto (text-button/create text)
     (.addListener (utils/change-listener on-clicked))
     (table/set-opts! opts))))
