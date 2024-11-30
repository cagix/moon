(ns forge.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [forge.app.systems]
            [forge.application :as application]
            [forge.lifecycle :as lifecycle]
            [forge.screens.editor :as editor]
            [forge.screens.main :as main]
            [forge.screens.map-editor :as map-editor]
            [forge.screens.minimap :as minimap]
            [forge.screens.world :as world]))

(def ^:private config "app.edn")

(defn- screens []
  {:screens/main-menu  (main/create)
   :screens/map-editor (map-editor/create)
   :screens/editor     (editor/create)
   :screens/minimap    (minimap/create)
   :screens/world      (world/screen)})

(defn -main []
  (let [{:keys [application lifecycle]} (-> config io/resource slurp edn/read-string)]
    (application/start application
                       (reify application/Listener
                         (create [_]
                           (lifecycle/create lifecycle
                                             screens
                                             :screens/main-menu))

                         (dispose [_]
                           (lifecycle/dispose))

                         (render [_]
                           (lifecycle/render))

                         (resize [_ w h]
                           (lifecycle/resize w h))))))
