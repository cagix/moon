(ns cdq.game
  (:require [cdq.application :as application]
            [clojure.config :as config]
            [com.badlogic.gdx.backends.lwjgl3 :as lwjgl])
  (:gen-class))

(defn -main [path]
  (let [{:keys [os->executions
                listener
                config]} (-> path config/edn-resource)]
    (lwjgl/start-application! {:create! (fn [gdx]
                                          (let [[f pipeline] (:create listener)]
                                            (f application/state gdx pipeline)))
                               :dispose! (fn []
                                           ((:dispose listener) application/state))
                               :render! (fn []
                                          (let [[f pipeline] (:render listener)]
                                            (f application/state pipeline)))
                               :resize! (fn [width height]
                                          ((:resize listener) application/state width height))
                               :pause! (fn [])
                               :resume! (fn [])}
                              config
                              os->executions)))
