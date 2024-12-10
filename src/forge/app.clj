(ns forge.app
  (:require [anvil.app :as app]
            [anvil.assets :as assets]
            [anvil.db :as db]
            [anvil.graphics :as g]
            [anvil.screen :as screen]
            [anvil.sprite :as sprite]
            [anvil.ui :as ui]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [forge.main-menu :as main-menu]
            [forge.world :as world]
            [forge.editor :as editor]
            [forge.minimap :as minimap]))

(defn- background-image [path]
  (fn []
    (ui/image->widget (sprite/create path)
                      {:fill-parent? true
                       :scaling :fill
                       :align :center})))

(defn- start [{:keys [db dock-icon lwjgl3-config assets graphics ui background]}]
  (db/setup db)
  (app/set-dock-icon dock-icon)
  (app/start lwjgl3-config
             (reify app/Listener
               (create [_]
                 (assets/setup assets)
                 (g/setup graphics)
                 (ui/setup ui)
                 (screen/setup {:screens/main-menu (main-menu/create (background-image background))
                                ;:screens/map-editor
                                :screens/editor (editor/create (background-image background))
                                :screens/minimap (minimap/screen)
                                :screens/world (world/screen)}
                               :screens/main-menu))

               (dispose [_]
                 (assets/cleanup)
                 (g/cleanup)
                 (ui/cleanup)
                 (screen/cleanup))

               (render [_]
                 (app/clear-screen)
                 (screen/render-current))

               (resize [_ w h]
                 (g/resize w h)))))

(defn -main []
  (-> "app.edn"
      io/resource
      slurp
      edn/read-string
      start))
