(ns cdq.db.schema.boolean
  (:require [clojure.scene2d.vis-ui.check-box :as check-box]))

(defn create-value [_ v _db]
  v)

(defn create [_ checked? _ctx]
  (assert (boolean? checked?))
  (check-box/create
   :text ""
   :on-clicked (fn [_])
   :checked? checked?))

(defn value [_ widget _schemas]
  (check-box/checked? widget))
