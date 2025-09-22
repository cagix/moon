(ns gdl.backends.desktop
  (:require [com.badlogic.gdx.backends.lwjgl3 :as lwjgl]
            [com.badlogic.gdx.utils :as utils]))

(defn dispatch-on-os [os->executions]
  (doseq [[f & params] (os->executions (utils/operating-system))]
    (apply f params)))

(defn application
  [{:keys [listener config]}]
  (lwjgl/start-application! listener config))
