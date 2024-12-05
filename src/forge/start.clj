(ns forge.start
  (:require [clojure.edn :as edn]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.java.awt :as awt]
            [clojure.java.io :as io]
            [clojure.lwjgl :as lwjgl]
            [forge.app :as app]
            [forge.core :refer [app-create
                                app-dispose
                                app-render
                                app-resize]]))

(defn -main []
  (let [{:keys [requires dock-icon lwjgl3-config components]}
        (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require requires)
    (awt/set-dock-icon dock-icon)
    (when shared-library-loader/mac?
      (lwjgl/configure {:glfw-library-name "glfw_async"
                        :glfw-check-thread0 false}))
    (lwjgl3/app (proxy [com.badlogic.gdx.ApplicationAdapter] []
                  (create  []    (run! app-create components))
                  (dispose []    (run! app-dispose components))
                  (render  []    (run! app-render components))
                  (resize  [w h] (run! #(app-resize % w h) components)))
                (lwjgl3/config lwjgl3-config))))
