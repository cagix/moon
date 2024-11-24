(ns ^:no-doc forge.schema.number
  (:require [forge.schema :as schema]))

(defmethod schema/form :s/number  [_] number?)
(defmethod schema/form :s/nat-int [_] nat-int?)
(defmethod schema/form :s/int     [_] int?)
(defmethod schema/form :s/pos     [_] pos?)
(defmethod schema/form :s/pos-int [_] pos-int?)
