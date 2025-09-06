(ns cdq.ui.text-field
  (:require [cdq.ui.widget :as widget]
            [clojure.vis-ui.text-field :as text-field]))

(defn create [{:keys [text-field/text] :as opts}]
  (-> (text-field/create text)
      (widget/set-opts! opts)))
