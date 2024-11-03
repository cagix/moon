(ns ^:no-doc moon.schema.val-max
  (:require [malli.core :as m]
            [moon.schema :as schema]
            [moon.val-max :as val-max]))

(defmethod schema/form :s/val-max [_]
  (m/form val-max/schema))

