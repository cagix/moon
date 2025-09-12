(ns cdq.schema.string
  (:require [clojure.vis-ui.text-field :as text-field]))

(defn malli-form [_ _schemas]
  :string)

(defn create [schema  v _ctx]
  {:actor/type :actor.type/text-field
   :text-field/text v
   :tooltip (str schema)})

(defn value [_ widget _schemas]
  (text-field/get-text widget))
