(ns cdq.application.create.ui
  (:require [cdq.ctx :as ctx]
            [gdl.ui :as ui]))

(defn do! []
  (ui/load! (:ui ctx/config)))
