(ns cdq.gdx-app.dispose
  (:require [cdq.ctx.audio :as audio]
            [cdq.ctx.graphics :as graphics]
            [cdq.ctx.world :as world]
            [clojure.vis-ui :as vis-ui]))

(defn do!
  [{:keys [ctx/audio
           ctx/graphics
           ctx/world]
    :as ctx}]
  (audio/dispose! audio)
  (graphics/dispose! graphics)
  (vis-ui/dispose!)
  (world/dispose! world)
  ctx)
