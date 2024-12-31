(ns gdl.effect.component
  (:require [clojure.utils :refer [defsystem]]))

(defsystem applicable?)

(defsystem handle)

(defsystem useful?)
(defmethod useful? :default [_ _effect-ctx context] true)

(defsystem render-effect)
(defmethod render-effect :default [_ _effect-ctx context])
