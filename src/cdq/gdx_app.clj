(ns cdq.gdx-app
  (:require [cdq.lwjgl :as lwjgl]
            [cdq.shared-library-loader :as shared-library-loader]))

(defn start!
  [{:keys [lwjgl-app-config listener]}]
  (when (= (shared-library-loader/operating-system) :mac)
    (lwjgl/set-glfw-async!))
  (lwjgl/start-application! (let [[f params] listener]
                              ((requiring-resolve f) params))
                            lwjgl-app-config))
