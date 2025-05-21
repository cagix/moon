(ns cdq.create.ui
  (:require [gdl.ui :as ui]))

(defn do! [{:keys [ctx/config]}]
  (ui/load! (:ui config)))
