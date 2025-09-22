(ns cdq.application.listener
  (:require [cdq.audio :as audio]
            [cdq.graphics :as graphics]
            [cdq.world :as world]
            [clojure.utils :as utils]
            [com.badlogic.gdx :as gdx]
            [gdl.scene2d.stage :as stage]))

(defn create [{:keys [create-pipeline
                      render-pipeline
                      state-var]}]
  (let [state @state-var]
    {:create (fn []
               (reset! state (utils/pipeline
                              (let [{:keys [audio files graphics input]} (gdx/state)]
                                {:ctx/audio audio
                                 :ctx/files files
                                 :ctx/graphics graphics
                                 :ctx/input input})
                              create-pipeline)))
     :dispose (fn []
                (let [{:keys [ctx/audio
                              ctx/graphics
                              ctx/vis-ui
                              ctx/world]} @state]
                  (audio/dispose! audio)
                  (graphics/dispose! graphics)
                  (com.badlogic.gdx.utils.Disposable/.dispose vis-ui)
                  (world/dispose! world)))
     :pause (fn [])
     :render (fn []
               (swap! state utils/pipeline render-pipeline)
               (stage/act!  (:ctx/stage @state))
               (stage/draw! (:ctx/stage @state)))
     :resize (fn [width height])
     :resume (fn [])}))
