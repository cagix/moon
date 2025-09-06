(ns cdq.ui.label
  (:require [cdq.ui.actor :as actor]
            [clojure.vis-ui.label :as label]))

(defn create [{:keys [label/text] :as opts}]
  (doto (label/create text)
    (actor/set-opts! opts)))
