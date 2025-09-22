(ns gdl.backends.desktop
  (:require [com.badlogic.gdx.backends.lwjgl3 :as lwjgl]
            [com.badlogic.gdx.utils :as utils]
            [gdl.backends.gdx.extends.audio]
            [gdl.backends.gdx.extends.files]
            [gdl.backends.gdx.extends.graphics]
            [gdl.backends.gdx.extends.input]))

(defn dispatch-on-os [os->executions]
  (doseq [[f & params] (os->executions (utils/operating-system))]
    (apply f params)))

(defn application
  [{:keys [listener config]}]
  (lwjgl/start-application! listener config))
