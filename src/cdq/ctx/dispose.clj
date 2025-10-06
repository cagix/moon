(ns cdq.ctx.dispose
  (:require [cdq.audio :as audio]
            [cdq.graphics :as graphics]
            [cdq.world :as world]
            [clojure.scene2d.vis-ui :as vis-ui]))

(defn do! [{:keys [ctx/audio
                   ctx/graphics
                   ctx/world]}]
  (vis-ui/dispose!)
  (audio/dispose! audio)
  (graphics/dispose! graphics)
  (world/dispose! world))
