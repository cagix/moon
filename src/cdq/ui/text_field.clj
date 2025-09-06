(ns cdq.ui.text-field
  (:require [cdq.ui :as ui]
            cdq.construct
            [clojure.vis-ui.text-field :as text-field]))

(defmethod cdq.construct/create :actor.type/text-field [{:keys [text-field/text] :as opts}]
  (-> (text-field/create)
      (ui/set-opts! opts)))
