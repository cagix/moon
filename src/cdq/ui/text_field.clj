(ns cdq.ui.text-field
  (:require [cdq.ui.widget :as widget]
            [clojure.vis-ui.text-field :as text-field]
            [clojure.vis-ui.tooltip :as tooltip]))

(defn create
  [{:keys [text-field/text]
    :as opts}]
  (let [actor (-> (text-field/create text)
                  (widget/set-opts! opts))]
    (when-let [tooltip (:tooltip opts)]
      (tooltip/add! actor tooltip))
    actor))
