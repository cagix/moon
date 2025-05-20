(ns cdq.application.render.draw-ui
  (:require [cdq.ctx :as ctx]
            [gdl.ui :as ui]))

(defn do! []
  (reset! (.ctx ctx/stage) (ctx/make-map))
  (ui/draw! ctx/stage))
