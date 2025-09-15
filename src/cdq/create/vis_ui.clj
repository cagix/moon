(ns cdq.create.vis-ui
  (:require [clojure.vis-ui :as vis-ui]
            [clojure.vis-ui.widget]))

(defn do! [ctx params]
  (vis-ui/load! params)
  ctx)
