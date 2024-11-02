(ns moon.screens.editor
  (:require [gdl.input :refer [key-just-pressed?]]
            [gdl.ui :as ui]
            [moon.component :as component]
            [moon.stage :as stage]
            [moon.screen :as screen]
            [moon.widgets.background-image :as background-image]))

(defmethods :screens/editor
  (component/create [_]
    (stage/create :actors
                  [(background-image/create)
                   (component/create [:widgets/properties-tabs nil])
                   (ui/actor {:act (fn []
                                     (when (key-just-pressed? :shift-left)
                                       (screen/change :screens/main-menu)))})])))
