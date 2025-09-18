(ns cdq.application.listener
  (:require [cdq.application :as application]
            [clojure.scene2d.stage :as stage]
            [clojure.utils :as utils]))

(defn create
  [{:keys [create
           dispose
           render
           resize]}]
  {:create! (fn [{:keys [clojure.gdx/audio
                         clojure.gdx/files
                         clojure.gdx/graphics
                         clojure.gdx/input]}]
              (reset! application/state (utils/pipeline
                                         {:ctx/audio audio
                                          :ctx/files files
                                          :ctx/graphics graphics
                                          :ctx/input input}
                                         create)))
   :dispose! (fn []
               (swap! application/state dispose))
   :render! (fn []
              (swap! application/state utils/pipeline render)
              (stage/act!  (:ctx/stage @application/state))
              (stage/draw! (:ctx/stage @application/state)))
   :resize! (fn [width height]
              (swap! application/state resize width height))
   :pause! (fn [])
   :resume! (fn [])})
