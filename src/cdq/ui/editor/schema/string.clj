(ns cdq.ui.editor.schema.string
  (:require [clojure.scene2d.vis-ui.text-field :as text-field]))

(defn create [schema v _ctx]
  (text-field/create
   {:text-field/text v
    :tooltip (str schema)}))

(defn value [_ widget _schemas]
  (text-field/text widget))
