(ns cdq.entity.state
  (:require [cdq.utils :refer [defsystem]]))

(defsystem clicked-skillmenu-skill)
(defmethod clicked-skillmenu-skill :default [_ skill c])
