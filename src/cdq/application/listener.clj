(ns cdq.application.listener
  (:require [cdq.audio :as audio]
            [cdq.graphics :as graphics]
            [cdq.world :as world]
            [clojure.utils :as utils]
            [gdl.scene2d.stage :as stage]))

; TODO
; 1. gdx/state pass from create ✅
; 2. gdx/state treat as 'gdl/context' which can do stuff like create sprite-batch/viewport?
; 3. dispose over all elements ?
; 4. resize over all elements ?
; 5. do not use `gdl.scene2d.stage` but `cdq.stage` ?
; 6. stage into the pipeline -> dont give app/state
; set-ctx! and don't do `swap!` in handlers but return new ctx
; then get the new ctx (after InputQueue drain before render)
; and set it to nil again

(defn create [{:keys [create-pipeline
                      render-pipeline
                      state-var]}]
  (let [state @state-var]
    {:create (fn [{:keys [audio
                          files
                          graphics
                          input]}]
               (reset! state (utils/pipeline
                              {:ctx/audio audio
                               :ctx/files files
                               :ctx/graphics graphics
                               :ctx/input input}
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
     :resize (fn [width height]
               (let [{:keys [ctx/graphics]} @state]
                 (graphics/update-viewports! graphics width height)))
     :resume (fn [])}))
