(ns cdq.application.dispose
  (:require [cdq.audio :as audio]
            [cdq.graphics :as graphics]
            [cdq.world :as world]))

(defn do! [state]
  (let [{:keys [ctx/audio
                ctx/graphics
                ctx/vis-ui
                ctx/world]} @state]
    (audio/dispose! audio)
    (graphics/dispose! graphics)
    (com.badlogic.gdx.utils.Disposable/.dispose vis-ui)
    (world/dispose! world)))
