(ns cdq.application.create.ui-viewport
  (:require [gdl.graphics :as graphics]))

(defn do! [{:keys [ctx/config]}]
  (graphics/ui-viewport (:ui-viewport config)))
