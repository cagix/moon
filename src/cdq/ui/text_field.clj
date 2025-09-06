(ns cdq.ui.text-field
  (:require [cdq.ui :as ui]
            [clojure.vis-ui.text-field :as text-field]))

(defn create [{:keys [text-field/text] :as opts}]
  (-> (text-field/create text)
      (ui/set-opts! opts)))
