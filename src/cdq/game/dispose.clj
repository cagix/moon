(ns cdq.game.dispose
  (:require [cdq.audio :as audio]
            [cdq.graphics :as graphics]
            [cdq.ui :as ui]
            [cdq.world :as world]))

(defn do!
  [{:keys [ctx/audio
           ctx/graphics
           ctx/stage
           ctx/world]}]
  (audio/dispose! audio)
  (graphics/dispose! graphics)
  (ui/dispose! stage)
  (world/dispose! world))
