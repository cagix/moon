(ns gdl.backends.gdx.lwjgl
  (:require [clojure.config :as config]
            [clojure.utils :as utils]
            [clojure.walk :as walk]))

(defn- create-executions [options]
  `[[clojure.config/dispatch-on
     com.badlogic.gdx.utils/operating-system
     {:mac
      [[org.lwjgl.system.configuration/set-glfw-library-name! "glfw_async"]
       [clojure.java.awt.taskbar/set-icon-image! "icon.png"]]}]

    [clojure.core/require
     gdl.backends.gdx.extends.application
     gdl.backends.gdx.extends.audio
     gdl.backends.gdx.extends.files
     gdl.backends.gdx.extends.graphics
     gdl.backends.gdx.extends.input]

    [com.badlogic.gdx.backends.lwjgl3/start-application! ~options]])

(defn start-application!
  [options]
  (->> options
       create-executions
       (walk/postwalk config/require-resolve-symbols)
       (run! utils/execute)))
