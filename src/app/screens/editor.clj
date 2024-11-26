(ns app.screens.editor
  (:require [app.editor :as editor]
            [app.screens.background-image :as background-image]
            [clojure.gdx :as gdx]
            [forge.app :as app]
            [forge.ui :as ui]))

(defn create []
  {:actors [(background-image/create)
            (editor/tabs-table "[LIGHT_GRAY]Left-Shift: Back to Main Menu[]")
            (ui/actor {:act (fn []
                              (when (gdx/key-just-pressed? :shift-left)
                                (app/change-screen :screens/main-menu)))})]
   :screen (reify app/Screen
             (enter [_])
             (exit [_])
             (render [_])
             (dispose [_]))})
