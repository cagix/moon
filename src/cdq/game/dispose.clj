(ns cdq.game.dispose
  (:require [cdq.ctx.audio :as audio]
            [cdq.ctx.graphics :as graphics]
            [cdq.ctx.world :as world]
            [cdq.gdx.ui :as ui]))

(defn do! [{:keys [ctx/audio
                   ctx/graphics
                   ctx/world]}]
  (audio/dispose! audio)
  (graphics/dispose! graphics)
  (world/dispose! world)
  (ui/dispose!))
