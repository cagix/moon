(ns cdq.application.dispose
  (:require [cdq.application :as application]
            [cdq.audio :as audio]
            [cdq.graphics :as graphics]
            [cdq.world :as world]))

(defn- do!* [{:keys [ctx/audio
                     ctx/graphics
                     ctx/vis-ui
                     ctx/world]}]
  (audio/dispose! audio)
  (graphics/dispose! graphics)
  (com.badlogic.gdx.utils.Disposable/.dispose vis-ui)
  (world/dispose! world))

(defn do! []
  (do!* @application/state))
