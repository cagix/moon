(ns cdq.editor.widget.boolean
  (:require [clojure.vis-ui.check-box :as check-box]))

(defn create [_ _attribute checked? _ctx]
  (assert (boolean? checked?))
  {:actor/type :actor.type/check-box
   :text ""
   :on-clicked (fn [_])
   :checked? checked?})

(defn value [_ _attribute widget _schemas]
  (check-box/checked? widget))
