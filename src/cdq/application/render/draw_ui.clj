(ns cdq.application.render.draw-ui
  (:require [cdq.ctx :as ctx]
            [gdl.ui :as ui]))

(defn do! []
  (reset! (.ctx ctx/stage) {:ctx/player-eid ctx/player-eid
                            :ctx/ui-viewport ctx/ui-viewport
                            :ctx/draw (ctx/get-draw)})
  (ui/draw! ctx/stage))
