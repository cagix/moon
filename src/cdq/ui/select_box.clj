(ns cdq.ui.select-box
  (:require cdq.construct
            [clojure.vis-ui.select-box :as select-box]))

(defmethod cdq.construct/create :actor.type/select-box [options]
  (select-box/create options))
