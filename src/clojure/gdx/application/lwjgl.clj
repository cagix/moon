(ns clojure.gdx.application.lwjgl
  (:require [clojure.core-ext :refer [call]]
            [clojure.gdx.application.listener :as listener]
            [clojure.gdx.backends.lwjgl.application :as application]
            [clojure.gdx.backends.lwjgl.application.config :as config]
            [clojure.lwjgl.system.configuration]))

(defn start!
  [{:keys [listener
           config]}]
  (clojure.lwjgl.system.configuration/set-glfw-library-name! "glfw_async")
  (application/create (listener/create (call listener))
                      (config/create config)))
