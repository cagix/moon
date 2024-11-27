(ns app.screens.editor
  (:require [app.editor :as editor]
            [app.screens.background-image :as background-image]
            [forge.app :as app]
            [forge.input :refer [key-just-pressed?]]
            [forge.ui :as ui]))

(defn create []
  {:actors [(background-image/create)
            (editor/tabs-table "[LIGHT_GRAY]Left-Shift: Back to Main Menu[]")
            (ui/actor {:act (fn []
                              (when (key-just-pressed? :shift-left)
                                (app/change-screen :screens/main-menu)))})]
   :screen (reify app/Screen
             (enter [_])
             (exit [_])
             (render [_])
             (dispose [_]))})
