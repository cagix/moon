(ns ^:no-doc moon.schema.number
  (:require [moon.schema :as schema]))

(defmethod schema/form :s/number  [_] number?)
(defmethod schema/form :s/nat-int [_] nat-int?)
(defmethod schema/form :s/int     [_] int?)
(defmethod schema/form :s/pos     [_] pos?)
(defmethod schema/form :s/pos-int [_] pos-int?)
