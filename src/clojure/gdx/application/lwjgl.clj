(ns clojure.gdx.application.lwjgl
  (:require [clojure.core-ext :refer [call]]
            [clojure.gdx.backends.lwjgl.application :as application]))

(defn start!
  [{:keys [listener
           config]}]
  (application/create (call listener)
                      (call config)))
