(ns cdq.ctx
  (:require [cdq.graphics :as g]
            [gdl.ui.stage :as stage]))

(defn mouseover-actor [{:keys [ctx/stage] :as ctx}]
  (stage/hit stage (g/ui-mouse-position ctx)))
