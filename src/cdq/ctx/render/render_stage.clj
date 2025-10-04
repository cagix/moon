(ns cdq.ctx.render.render-stage
  (:require [clojure.scene2d.stage :as stage]))

(defn do!
  [{:keys [ctx/stage]
    :as ctx}]
  (stage/set-ctx! stage ctx)
  (stage/act!     stage)
  (stage/draw!    stage)
  (stage/get-ctx  stage))
