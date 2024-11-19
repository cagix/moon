(ns ^:no-doc moon.screens.main
  (:require [gdl.app :as app]
            [gdl.db :as db]
            [gdl.input :refer [key-just-pressed?]]
            [gdl.screen :as screen]
            [gdl.ui :as ui]
            [gdl.utils :refer [dev-mode?]]
            [moon.core :refer [set-cursor]]
            [moon.screens.world :as world]
            [moon.widgets.background-image :as background-image]))

(defn- buttons []
  (ui/table
   {:rows
    (remove nil?
            (concat
             (for [{:keys [property/id]} (db/all :properties/worlds)]
               [(ui/text-button (str "Start " id) #(world/start id))])
             [(when dev-mode?
                [(ui/text-button "Map editor" #(screen/change :screens/map-editor))])
              (when dev-mode?
                [(ui/text-button "Property editor" #(screen/change :screens/editor))])
              [(ui/text-button "Exit" app/exit)]]))
    :cell-defaults {:pad-bottom 25}
    :fill-parent? true}))

(deftype MainMenuScreen []
  screen/Screen
  (screen/enter [_]
    (set-cursor :cursors/default))
  (screen/exit [_])
  (screen/render [_])
  (screen/dispose [_]))

(defn create []
  {:actors [(background-image/create)
            (buttons)
            (ui/actor {:act (fn []
                              (when (key-just-pressed? :keys/escape)
                                (app/exit)))})]
   :screen (->MainMenuScreen)})
