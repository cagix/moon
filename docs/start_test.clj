(ns forge.app.start
  (:require [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.utils :refer [mac?]]
            [clojure.java.awt :as awt]
            [forge.app.systems]
            [forge.app.lifecycle :as lifecycle]
            [forge.db :as db]))

(defn -main []
  (let [{:keys [dock-icon
                schema
                properties
                title
                fps
                width
                height]} {:dock-icon "moon.png"
                          :schema "schema.edn"
                          :properties "properties.edn"
                          :title "Moon"
                          :fps 60
                          :width 1440
                          :height 900}]
    (awt/set-dock-icon dock-icon)
    (db/init :schema schema
             :properties properties)
    (when mac?
      (lwjgl3/configure-glfw-for-mac))
    (lwjgl3/application (proxy [com.badlogic.gdx.ApplicationAdapter] []
                          (create []
                            (lifecycle/create))

                          (dispose []
                            (lifecycle/dispose))

                          (render []
                            (lifecycle/render))

                          (resize [w h]
                            (lifecycle/resize)))
                        (lwjgl3/config {:title title
                                        :fps fps
                                        :width width
                                        :height height}))))
