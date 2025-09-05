(ns cdq.application.mac-os-config
  (:require [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.lwjgl.system.configuration :as lwjgl-configuration]))

(defn set-glfw-async! [_]
  (when (= (shared-library-loader/operating-system) :mac)
    (lwjgl-configuration/set-glfw-library-name! "glfw_async")))
