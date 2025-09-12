(ns cdq.schema.boolean
  (:require [cdq.schema :as schema]
            [clojure.vis-ui.check-box :as check-box]))

(defmethod schema/malli-form :s/boolean [[_ & params] _schemas]
  :boolean)

(defn create [_ checked? _ctx]
  (assert (boolean? checked?))
  {:actor/type :actor.type/check-box
   :text ""
   :on-clicked (fn [_])
   :checked? checked?})

(defn value [_ widget _schemas]
  (check-box/checked? widget))
