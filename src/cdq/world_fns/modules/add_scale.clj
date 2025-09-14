(ns cdq.world-fns.modules.add-scale
  (:require [cdq.world-fns.module :as module]))

(defn do! [w]
  (assoc w :scale module/modules-scale))
