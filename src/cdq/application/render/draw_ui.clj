(ns cdq.application.render.draw-ui
  (:require [cdq.ctx :as ctx]
            [gdl.ui :as ui]))

(defn do! []
  (ui/draw! ctx/stage))
