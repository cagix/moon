(ns cdq.game.dispose
  (:require [cdq.ctx.audio :as audio] ; ctx namespaces make in protocols
            [cdq.ctx.graphics :as graphics]
            [cdq.ctx.stage :as stage]
            [cdq.ctx.world :as world]))

(defn do! [{:keys [ctx/audio
                   ctx/graphics
                   ctx/world]}]
  (audio/dispose! audio)
  (graphics/dispose! graphics)
  (world/dispose! world)
  (stage/dispose!))
