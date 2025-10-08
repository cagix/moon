(ns clojure.gdx.backends.lwjgl.application
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application)))

(defn create
  [{:keys [listener config]}]
  (Lwjgl3Application. (let [[f & params] listener]
                        (apply (requiring-resolve f) params))
                      (let [[f & params] config]
                        (apply (requiring-resolve f) params))))
