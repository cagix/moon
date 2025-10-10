(ns clojure.scene2d.vis-ui.text-field
  (:require [cdq.ui.tooltip :as tooltip]
            [clojure.scene2d.widget :as widget]
            [clojure.vis-ui.text-field :as text-field]))

(defn create
  [{:keys [text-field/text]
    :as opts}]
  (let [actor (-> (text-field/create text)
                  (widget/set-opts! opts))]
    (when-let [tooltip (:tooltip opts)]
      (tooltip/add! actor tooltip))
    actor))

(def text text-field/text)
