(ns cdq.application
  (:require [cdq.audio :as audio]
            [cdq.graphics :as graphics]
            [cdq.world :as world]
            [clojure.config :as config]
            [clojure.utils :as utils]
            [gdl.scene2d.stage :as stage]
            [gdl.impl.application])
  (:gen-class))

(def state (atom nil))

(defn -main []
  (let [create-pipeline (config/edn-resource "create.edn")
        render-pipeline (config/edn-resource "render.edn")]
    (gdl.impl.application/start!
     {:listener {:create (fn [context]
                           (reset! state (utils/pipeline context create-pipeline)))
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
                 :resume (fn [])}
      :config {:title "Cyber Dungeon Quest"
               :windowed-mode {:width 1440
                               :height 900}
               :foreground-fps 60}})))
