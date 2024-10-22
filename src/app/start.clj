(ns app.start
  (:require app.assets
            [app.config :as config]
            [app.dock-icon :as dock-icon]
            [app.screens.editor :as property-editor]
            [app.screens.main :as main-menu]
            [app.screens.map-editor :as map-editor]
            [app.screens.world :as world-screen]
            [component.db :as db]
            [gdx.assets :as assets]
            [gdx.backends.lwjgl3 :as lwjgl3]
            [gdx.graphics :as g]
            [gdx.screen :as screen]
            [gdx.vis-ui :as vis-ui])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (org.lwjgl.system Configuration)))

(defn- application-listener []
  (proxy [ApplicationAdapter] []
    (create []
      (assets/load (app.assets/search config/resources))
      (g/load! config/graphics)
      (vis-ui/load! config/skin-scale)
      (screen/set-screens! [(main-menu/create)
                            (map-editor/create)
                            (property-editor/screen)
                            (world-screen/create)])
      ((world-screen/start-game-fn :worlds/vampire)))

    (dispose []
      (assets/dispose)
      (g/dispose!)
      (vis-ui/dispose!)
      (screen/dispose-all!))

    (render []
      (g/clear-screen)
      (screen/render! (screen/current)))

    (resize [w h]
      (g/resize! [w h]))))

(defn -main []
  (db/load! config/properties)
  (when SharedLibraryLoader/isMac
    (dock-icon/set config/dock-icon)
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    (.set Configuration/GLFW_CHECK_THREAD0 false))
  (lwjgl3/application (application-listener)
                      (lwjgl3/config config/lwjgl3)))
