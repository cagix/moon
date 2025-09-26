(ns cdq.db.schema.string)

(defn malli-form [_ _schemas]
  :string)

(defn create-value [_ v _db]
  v)

(defn create [schema  v _ctx]
  {:actor/type :actor.type/text-field
   :text-field/text v
   :tooltip (str schema)})

(defn value [_ widget _schemas]
  (:text-field/text widget))
