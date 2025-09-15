(ns cdq.schema.boolean
  (:require [clojure.vis-ui.check-box :as check-box]))

(defn malli-form [[_ & params] _schemas]
  :boolean)

(defn create-value [_ v _db]
  v)

(defn create [_ checked? _ctx]
  (assert (boolean? checked?))
  {:actor/type :actor.type/check-box
   :text ""
   :on-clicked (fn [_])
   :checked? checked?})

(defn value [_ widget _schemas]
  (check-box/checked? widget))
