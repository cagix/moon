(ns anvil.app
  (:require [anvil.screens.world :as world]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.app :as app]
            [gdl.assets :as assets]
            [gdl.db :as db]
            [gdl.graphics :as g]
            [gdl.screen :as screen]
            [gdl.ui :as ui]
            [forge.world.create :refer [create-world]]))

; Next: remove minimap ....
; Then: can remove screens themself ?!
; also: world-viewport ?!
; cursors/etc ?
; * @ editor doesn't need cursors/default-font/world-viewport
; * also make app mapgen-test -> also doesn't need certain stuffs

(defn- start [{:keys [db app-config graphics ui world-id]}]
  (db/setup db)
  (app/start app-config
             (reify app/Listener
               (create [_]
                 (assets/setup)
                 (g/setup graphics)
                 (ui/setup ui)
                 (screen/setup {:screens/world (world/screen)}
                               :screens/world)
                 (create-world (db/build world-id)))

               (dispose [_]
                 (assets/cleanup)
                 (g/cleanup)
                 (ui/cleanup)
                 (screen/cleanup))

               (render [_]
                 (g/clear-screen)
                 (screen/render-current))

               (resize [_ w h]
                 (g/resize w h)))))

(defn -main []
  (-> "app.edn"
      io/resource
      slurp
      edn/read-string
      start))
