(ns moon.screens.editor
  (:require [gdl.input :refer [key-just-pressed?]]
            [gdl.ui :as ui]
            [moon.component :refer [defc] :as component]
            [moon.stage :as stage]
            [moon.screen :as screen]))

(defc :screens/editor
  (component/create [[_ background-image]]
    (stage/create :actors
                  [(background-image)
                   (component/create [:widgets/properties-tabs nil])
                   (ui/actor {:act (fn []
                                     (when (key-just-pressed? :shift-left)
                                       (screen/change :screens/main-menu)))})])))
