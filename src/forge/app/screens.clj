(ns forge.app.screens
  (:require [anvil.app :as app]
            [anvil.graphics :as g]
            [clojure.component :as component]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.utils :refer [bind-root]]))

(defn create [[_ {:keys [screens first-k]}]]
  (bind-root app/screens
             (into {}
                   (for [k screens]
                     [k [:screens/stage {:stage (stage/create g/gui-viewport
                                                              app/batch
                                                              (component/actors [k]))
                                         :sub-screen [k]}]])))
  (app/change-screen first-k))

(defn dispose [_]
  (run! component/dispose (vals app/screens)))

(defn render [_]
  (component/render (app/current-screen)))
