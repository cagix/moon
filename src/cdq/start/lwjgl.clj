(ns cdq.start.lwjgl
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn do!
  [{:keys [ctx/config]
    :as ctx}]
  (let [{:keys [listener
                config]} (:cdq.start.lwjgl config)]
    (lwjgl/start-application! ((requiring-resolve listener) ctx)
                              config)))
