(ns cdq.application.lwjgl
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn start!
  [{:keys [listener config]}]
  (lwjgl/start-application! (let [[f params] listener]
                              ((requiring-resolve f) params))
                            config))
