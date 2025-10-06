(ns cdq.ctx.create.vis-ui
  (:require [clojure.scene2d.vis-ui :as vis-ui]))

(defn do! [ctx params]
  (vis-ui/load! params)
  ctx)
