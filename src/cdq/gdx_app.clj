(ns cdq.gdx-app
  (:require [clojure.gdx.backends.lwjgl3 :as lwjgl]))

(defn start!
  [{:keys [lwjgl-app-config listener]}]
  (lwjgl/start-application! (let [[f params] listener]
                              ((requiring-resolve f) params))
                            lwjgl-app-config))
