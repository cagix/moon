(ns cdq.game.dispose
  (:require [cdq.audio :as audio]
            [cdq.graphics :as graphics]
            [cdq.world :as world]))

(defn do!
  [{:keys [ctx/audio
           ctx/graphics
           ctx/world
           ctx/vis-ui]
    :as ctx}]
  (audio/dispose! audio)
  (graphics/dispose! graphics)
  (com.badlogic.gdx.utils.Disposable/.dispose vis-ui)
  (world/dispose! world)
  ctx)
