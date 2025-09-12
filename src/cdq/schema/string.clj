(ns cdq.schema.string
  (:require [cdq.schema :as schema]
            [clojure.vis-ui.text-field :as text-field]))

(defmethod schema/malli-form :s/string [_ _schemas]
  :string)

(defn create [schema  v _ctx]
  {:actor/type :actor.type/text-field
   :text-field/text v
   :tooltip (str schema)})

(defn value [_ widget _schemas]
  (text-field/get-text widget))
