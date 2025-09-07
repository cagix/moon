(ns cdq.application.lwjgl
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn start!
  [{:keys [lwjgl-app-config listener]}]
  (lwjgl/start-application! (let [[f params] listener]
                              ((requiring-resolve f) params))
                            lwjgl-app-config))
