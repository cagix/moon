(ns cdq.application.render.update-ui
  (:require [cdq.ctx :as ctx]
            [gdl.ui :as ui]))

(defn do! []
  (reset! (.ctx ctx/stage) (ctx/make-map))
  (ui/act! ctx/stage))
