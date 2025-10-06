(ns clojure.gdx.backends.lwjgl
  (:require [clojure.gdx.application :as app]
            [com.badlogic.gdx.backends.lwjgl3.application :as application]
            [com.badlogic.gdx.backends.lwjgl3.application.config :as config]))

(defn create [listener config]
  (application/create (app/listener listener)
                      (config/create config)))
