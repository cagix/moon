(ns cdq.create.vis-ui
  (:require [clojure.vis-ui :as vis-ui]))

(defn do! [ctx params]
  (assoc ctx :ctx/vis-ui (vis-ui/load! params)))
