(ns forge.start
  (:require [clojure.awt :as awt]
            [clojure.edn :as edn]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.java.io :as io]
            [clojure.lwjgl :as lwjgl]
            [forge.core :refer :all])
  (:import (com.badlogic.gdx ApplicationAdapter)))

(defn -main []
  (let [{:keys [requires
                dock-icon
                glfw-config
                lwjgl3-config
                components]} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require requires)
    (awt/set-dock-icon (io/resource dock-icon))
    (lwjgl/set-glfw-config glfw-config)
    (lwjgl3/start-app (proxy [ApplicationAdapter] []
                        (create  []     (run! app-create          components))
                        (dispose []     (run! app-destroy         components))
                        (render  []     (run! app-render          components))
                        (resize  [w h] (run! #(app-resize % w h) components)))
                      lwjgl3-config)))
