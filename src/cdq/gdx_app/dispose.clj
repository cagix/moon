(ns cdq.gdx-app.dispose
  (:require [cdq.gdx.graphics :as graphics]
            [cdq.ctx.world :as world]
            [clojure.gdx.utils.disposable :as disposable]
            [clojure.vis-ui :as vis-ui]))

(defn- dispose-audio! [sounds]
  (run! disposable/dispose! (vals sounds)))

(defn do!
  [{:keys [ctx/audio
           ctx/graphics
           ctx/world]
    :as ctx}]
  (dispose-audio! audio)
  (graphics/dispose! graphics)
  (vis-ui/dispose!)
  (world/dispose! world)
  ctx)
