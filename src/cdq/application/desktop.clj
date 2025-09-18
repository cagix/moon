(ns cdq.application.desktop
  (:require clojure.config
            com.badlogic.gdx.backends.lwjgl3)
  (:gen-class))

(defn -main [path]
  (-> path
      clojure.config/edn-resource
      com.badlogic.gdx.backends.lwjgl3/start!))
