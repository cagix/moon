(ns forge.start
  (:require [clojure.edn :as edn]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.java.awt :as awt]
            [clojure.java.io :as io]
            [clojure.lwjgl :as lwjgl]
            [forge.app :as app]))

(defn -main []
  (let [{:keys [components] :as config} (-> "app.edn"
                                            io/resource
                                            slurp
                                            edn/read-string)]
    (run! require (:requires config))
    (awt/set-dock-icon (:dock-icon config))
    (when shared-library-loader/mac?
      (lwjgl/configure {:glfw-library-name "glfw_async"
                        :glfw-check-thread0 false}))
    (lwjgl3/app (proxy [com.badlogic.gdx.ApplicationAdapter] []
                  (create  []    (run! app/create          components))
                  (dispose []    (run! app/dispose         components))
                  (render  []    (run! app/render          components))
                  (resize  [w h] (run! #(app/resize % w h) components)))
                (lwjgl3/config (:lwjgl3 config)))))
