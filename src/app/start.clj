(ns app.start
  (:require [app.lifecycle :as lifecycle]
            [app.systems]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.utils :refer [mac?]]
            [clojure.java.awt :as awt]
            [clojure.java.io :as io]
            [forge.db :as db]))

(defn -main []
  (awt/set-dock-icon "moon.png")
  (db/init :schema "schema.edn"
           :properties "properties.edn")
  (when mac?
    (lwjgl3/configure-glfw-for-mac))
  (lwjgl3/application (proxy [com.badlogic.gdx.ApplicationAdapter] []
                        (create  []    (lifecycle/create))
                        (dispose []    (lifecycle/dispose))
                        (render  []    (lifecycle/render))
                        (resize  [w h] (lifecycle/resize w h)))
                      (lwjgl3/config {:title "Moon"
                                      :fps 60
                                      :width 1440
                                      :height 900})))
