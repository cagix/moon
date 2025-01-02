(ns gdl.context.ui
  (:require [gdl.ui :as ui]))

(defn create [[_ config] _c]
  (ui/setup config))

(defn dispose [_]
  (ui/cleanup))
