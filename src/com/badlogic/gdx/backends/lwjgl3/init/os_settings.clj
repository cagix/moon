(ns com.badlogic.gdx.backends.lwjgl3.init.os-settings
  (:require [com.badlogic.gdx.utils.shared-library-loader :as shared-library-loader]
            [com.badlogic.gdx.utils.os :as os]))

(defn do! [init]
  (doseq [[f params] (get (:init/os->executions init)
                          (os/value->keyword (shared-library-loader/os)))]
    (f params))
  init)
