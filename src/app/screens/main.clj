(ns ^:no-doc app.screens.main
  (:require [gdl.app :as app]
            [moon.db :as db]
            [gdl.input :refer [key-just-pressed?]]
            [gdl.screen :as screen]
            [gdl.ui :as ui]
            [gdl.utils :refer [dev-mode?]]
            [moon.app :refer [change-screen set-cursor]]
            [app.screens.world :as world]
            [moon.widgets.background-image :as background-image]))

(defn- buttons []
  (ui/table
   {:rows
    (remove nil?
            (concat
             (for [{:keys [property/id]} (db/all :properties/worlds)]
               [(ui/text-button (str "Start " id) #(world/start id))])
             [(when dev-mode?
                [(ui/text-button "Map editor" #(change-screen :screens/map-editor))])
              (when dev-mode?
                [(ui/text-button "Property editor" #(change-screen :screens/editor))])
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
