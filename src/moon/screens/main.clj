(ns ^:no-doc moon.screens.main
  (:require [gdl.app :as app]
            [gdl.input :refer [key-just-pressed?]]
            [gdl.ui :as ui]
            [gdl.utils :refer [dev-mode?]]
            [moon.component :refer [defc] :as component]
            [moon.stage :as stage]
            [moon.db :as db]
            [moon.graphics.cursors :as cursors]
            [moon.screen :as screen]
            [moon.world :as world]))

(defn- ->buttons []
  (ui/table {:rows (remove nil? (concat
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
    (cursors/set :cursors/default))
  (screen/exit [_])
  (screen/render [_])
  (screen/dispose [_]))

(defc :screens/main-menu
  (component/create [[_ background-image]]
    (stage/create :actors
                  [(background-image)
                   (->buttons)
                   (ui/actor {:act (fn []
                                     (when (key-just-pressed? :keys/escape)
                                       (app/exit)))})]
                  :screen (->MainMenuScreen))))
