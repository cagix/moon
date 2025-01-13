(ns clojure.effect
  (:require [clojure.utils :refer [defsystem]]))

(defsystem applicable?)

(defsystem handle)

(defsystem useful?)
(defmethod useful? :default [_ _effect-ctx context] true)
