(ns cdq.ctx.dispose
  (:require [cdq.audio :as audio]
            [cdq.world :as world]
            [clojure.scene2d.vis-ui :as vis-ui]
            [clojure.disposable :refer [dispose!]]))

(defn do! [{:keys [ctx/audio
                   ctx/graphics
                   ctx/world]}]
  (vis-ui/dispose!)
  (audio/dispose! audio)
  (dispose! graphics)
  (world/dispose! world))
