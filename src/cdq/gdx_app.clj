(ns cdq.gdx-app
  (:require [clojure.gdx.backends.lwjgl3 :as lwjgl]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.lwjgl.system.configuration :as lwjgl-configuration]))

(defn start!
  [{:keys [lwjgl-app-config listener]}]
  (when (= (shared-library-loader/operating-system) :mac)
    (lwjgl-configuration/set-glfw-library-name! "glfw_async"))
  (lwjgl/start-application! (let [[f params] listener]
                              ((requiring-resolve f) params))
                            lwjgl-app-config))
