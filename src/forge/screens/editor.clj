(ns forge.screens.editor
  (:require [forge.app :as app]
            [forge.input :refer [key-just-pressed?]]
            [forge.screens.editor.ui :as editor]
            [forge.ui :as ui]
            [forge.ui.background-image :as background-image]))

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
