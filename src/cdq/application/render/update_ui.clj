(ns cdq.application.render.update-ui
  (:require [cdq.ctx :as ctx]
            [gdl.ui :as ui]))

(defn do! []
  (ui/act! ctx/stage))
