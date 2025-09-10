(ns cdq.gdx-app.dispose
  (:require [cdq.gdx.graphics :as graphics]
            [clojure.gdx.utils.disposable :as disposable]
            [clojure.vis-ui :as vis-ui]))

(defn- dispose-audio! [sounds]
  (run! disposable/dispose! (vals sounds)))

(defn do!
  [{:keys [ctx/audio
           ctx/world]
    :as ctx}]
  (dispose-audio! audio)
  (graphics/dispose! ctx)
  (disposable/dispose! (:world/tiled-map world))
  (vis-ui/dispose!))
