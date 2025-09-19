(ns com.badlogic.gdx.backends.lwjgl3.init.os-settings
  (:require [com.badlogic.gdx.utils.shared-library-loader :as shared-library-loader]))

(defn do! [init]
  (doseq [[f params] (get (:init/os->executions init) (shared-library-loader/operating-system))]
    (f params))
  init)
