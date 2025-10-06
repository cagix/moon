(ns cdq.ctx.dispose
  (:require [cdq.audio :as audio]
            [cdq.world :as world]
            [clojure.disposable :refer [dispose!]]))

(defn do! [{:keys [ctx/audio
                   ctx/graphics
                   ctx/vis-ui
                   ctx/world]}]
  (audio/dispose! audio)
  (dispose! graphics)
  (dispose! vis-ui)
  (world/dispose! world))
