(ns com.badlogic.gdx.backends.lwjgl3.init.logger
  (:import (com.badlogic.gdx ApplicationLogger)))

(defn- create-application-logger []
  (reify ApplicationLogger
    (log [_ tag message]
      (println (str "[" tag "] " message)))

    (log [_ tag message exception]
      (println (str "[" tag "] " message))
      (.printStackTrace exception System/out))

    (error [_ tag message]
      (.println System/err (str "[" tag "] " message)))

    (error [_ tag message exception]
      (.println System/err (str "[" tag "] " message))
      (.printStackTrace exception System/err))

    (debug [_ tag message]
      (println (str "[" tag "] " message)))

    (debug [_ tag message exception]
      (println (str "[" tag "] " message))
      (.printStackTrace exception System/out))))

(defn do!
  [{:keys [^com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application init/application]
    :as init}]
  (.setApplicationLogger application (create-application-logger))
  init)
