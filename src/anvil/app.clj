(ns anvil.app
  (:require [gdl.screen :as screen]
            [anvil.screens.minimap :as minimap]
            [anvil.screens.world :as world]
            [gdl.graphics.sprite :as sprite]
            [gdl.ui :as ui]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.app :as app]
            [gdl.assets :as assets]
            [gdl.db :as db]
            [gdl.graphics :as g]
            [forge.world.create :refer [create-world]])
  (:import (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.utils ScreenUtils)))

(defn- clear-screen []
  (ScreenUtils/clear Color/BLACK))

(defn- start [{:keys [db app-config graphics ui world-id]}]
  (db/setup db)
  (app/start app-config
             (reify app/Listener
               (create [_]
                 (assets/setup)
                 (g/setup graphics)
                 (ui/setup ui)
                 (screen/setup {:screens/minimap (minimap/screen)
                                :screens/world (world/screen)}
                               :screens/world)
                 (create-world (db/build world-id)))

               (dispose [_]
                 (assets/cleanup)
                 (g/cleanup)
                 (ui/cleanup)
                 (screen/cleanup))

               (render [_]
                 (clear-screen)
                 (screen/render-current))

               (resize [_ w h]
                 (g/resize w h)))))

(defn -main []
  (-> "app.edn"
      io/resource
      slurp
      edn/read-string
      start))
