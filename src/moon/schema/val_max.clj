(ns ^:no-doc moon.schema.val-max
  (:require [gdl.schema :as schema]
            [malli.core :as m]
            [moon.val-max :as val-max]))

(defmethod schema/form :s/val-max [_]
  (m/form val-max/schema))

