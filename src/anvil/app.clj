(ns anvil.app
  (:require [anvil.utils :refer [defsystem]]))

(defsystem create)

(defsystem dispose)
(defmethod dispose :default [_])

(defsystem render)
(defmethod render :default [_])

(defsystem resize)
(defmethod resize :default [_ w h])
