(ns forge.app.screens
  (:require [anvil.app :as app]
            [anvil.graphics :as g]
            [anvil.system :as system]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.utils :refer [bind-root]]))

(defn create [[_ {:keys [screens first-k]}]]
  (bind-root app/screens
             (into {}
                   (for [k screens]
                     [k [:screens/stage {:stage (stage/create g/gui-viewport
                                                              g/batch
                                                              (system/actors [k]))
                                         :sub-screen [k]}]])))
  (app/change-screen first-k))

(defn dispose [_]
  (run! system/dispose (vals app/screens)))

(defn render [_]
  (system/render (app/current-screen)))
