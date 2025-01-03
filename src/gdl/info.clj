(ns gdl.info
  (:require [gdl.utils :refer [defsystem]]))

(defsystem text)
(defmethod text :default [_ _entity _context])
