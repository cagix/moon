(ns cdq.gdx-app.dispose
  (:require [cdq.audio :as audio]
            [cdq.graphics :as graphics]
            [cdq.world :as world]
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
