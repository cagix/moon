(ns gdl.start
  (:require [clojure.gdx.app-listener :as app-listener]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.gdx.utils.os :as os]))

(defn- operating-system []
  (get os/mapping (shared-library-loader/os)))

(defn start! [config]
  (when (= (operating-system) :os/mac-osx)
    (when-let [[f params] (:mac-os config)]
      (f params)))
  (lwjgl/application! (:clojure.gdx.lwjgl/config config)
                      (app-listener/create-adapter (:listener config))))
