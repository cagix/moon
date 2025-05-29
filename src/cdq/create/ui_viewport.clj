(ns cdq.create.ui-viewport
  (:require [gdl.viewport :as viewport]))

(defn do! [{:keys [ctx/config]
            :as ctx}]
  (assoc ctx :ctx/ui-viewport (viewport/ui-viewport (:ui-viewport config))))
